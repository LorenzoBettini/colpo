package colpo.core.semantics;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import colpo.core.AttributeMatcher;
import colpo.core.Attributes;
import colpo.core.EvaluationContext;
import colpo.core.Exchange;
import colpo.core.Participant;
import colpo.core.Participant.Quantifier;
import colpo.core.Policies;
import colpo.core.Policies.PolicyData;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

/**
 * @author Lorenzo Bettini
 */
public class Semantics {

	private Policies policies;
	private AttributeMatcher matcher = new AttributeMatcher();
	private Trace trace = new Trace();

	public Semantics(Policies policies) {
		this.policies = policies;
	}

	public boolean evaluate(Request request) {
		trace.reset();
		return evaluate(request, new LinkedHashSet<>());
	}

	private boolean evaluate(Request request, Set<Request> R) {
		trace.add(String.format("evaluating %s", request));
		trace.addIndent();
		var from = request.from();
		var index = from.getIndex();
		boolean result = false;
		if (index > 0)
			result = evaluate(index, policies.getByIndex(index), request, R);
		else {
			trace.add("finding matching policies");
			trace.addIndent();
			var policiesToEvaluate = policiesToEvaluate(request.requester(), from);
			trace.removeIndent();
			if (policiesToEvaluate.isEmpty())
				result = false;
			else if (from.getQuantifier() == Quantifier.ANY) {
				result = policiesToEvaluate.stream()
						.anyMatch(d -> evaluate(d.index(), d.policy(), request, R));
			} else
				result = policiesToEvaluate.stream()
				.allMatch(d -> evaluate(d.index(), d.policy(), request, R));
		}
		trace.removeIndent();
		trace.add(String.format("result: %s", result));
		return result;
	}

	private Collection<PolicyData> policiesToEvaluate(Participant requester,
			Participant from) {
		return policies.getPolicyData()
			.filter(d -> d.index() != requester.getIndex())
			.filter(d -> {
				var attributes1 = from.getAttributes();
				var attributes2 = d.policy().party();
				return tryMatch("policy", d.index(), "from", attributes1, attributes2);
			})
			.toList();
	}

	private boolean tryMatch(String prefix, int index, String description, Attributes attributes1, Attributes attributes2) {
		boolean matchResult = matcher.match(attributes1, attributes2);
		trace.add(String.format("%s %d: %s match(%s, %s) -> %s",
			prefix, index, description, attributes1, attributes2, matchResult));
		return matchResult;
	}

	private boolean evaluate(int policyIndex, Policy policy, Request request, Set<Request> R) {
		trace.add(String.format("policy %d: evaluating rules",
				policyIndex));
		trace.addIndent();
		var result = evaluate(policyIndex, policy.rules(), request, R);
		trace.removeIndent();
		return result;
	}

	private boolean evaluate(int policyIndex, Rules rules, Request request, Set<Request> R) {
		return rules.getRuleData()
			.anyMatch(r -> evaluate(policyIndex, r.index(), r.rule(), request, R));
	}

	private boolean evaluate(int policyIndex, int ruleIndex, Rule rule, Request request, Set<Request> R) {
		try {
			boolean result = tryMatch("rule", ruleIndex, "resource", request.resource(), rule.getResource());
			if (!result)
				return false;
			result = rule.getCondition().evaluate(new EvaluationContext() {
				@Override
				public Object name(String name) throws UndefinedName {
					var value = request.resource().name(name);
					if (value == null)
						value = request.credentials().name(name);
					if (value == null)
						throw new UndefinedName(name);
					return value;
				}
			});
			trace.add(String.format("rule %d: condition %s -> %s", ruleIndex, rule.getCondition(), result));
			if (!result)
				return false;
			var exchange = rule.getExchange();
			if (exchange != null) {
				var processedRequest = new Request(
					request.requester(),
					request.resource(),
					request.credentials(),
					Participant.index(policyIndex));
				R.add(processedRequest);
				trace.add(String.format("policy %d: evaluating %s", policyIndex, exchange));
				result = evaluate(policyIndex, exchange, request, R);
				R.remove(processedRequest);
			}
			return result;
		} catch (Exception e) {
			trace.add(String.format("rule %d: condition %s -> %s", ruleIndex, rule.getCondition(), e.getMessage()));
			return false;
		}
	}

	private boolean evaluate(int policyIndex, Exchange exchange, Request request, Set<Request> R) {
		var exchangeRequest = new Request(
			Participant.index(policyIndex),
			exchange.resource(),
			exchange.credentials(),
			request.requester());
		if (R.contains(exchangeRequest)) {
			trace.add(String.format("%d: satisfied %s", policyIndex, exchangeRequest));
			return true;
		}
		return evaluate(exchangeRequest, R);
	}

	public Trace getTrace() {
		return trace;
	}
}
