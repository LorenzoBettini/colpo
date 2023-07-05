package colpo.core.semantics;

import java.util.Collection;

import colpo.core.AttributeMatcher;
import colpo.core.EvaluationContext;
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
		trace.add(String.format("evaluating %s", request));
		trace.addIndent();
		// REMEMBER: our indexes start from 1
		var from = request.from();
		var index = from.getIndex();
		boolean result = false;
		if (index > 0)
			result = evaluate(index, policies.getByIndex(index), request);
		else {
			trace.add("finding matching policies");
			trace.addIndent();
			var policiesToEvaluate = policiesToEvaluate(request.requester(), from);
			trace.removeIndent();
			if (policiesToEvaluate.isEmpty())
				result = false;
			else if (from.getQuantifier() == Quantifier.ANY) {
				result = policiesToEvaluate.stream()
					.anyMatch(d -> evaluate(d.index(), d.policy(), request));
			} else
				result = policiesToEvaluate.stream()
					.allMatch(d -> evaluate(d.index(), d.policy(), request));
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
				boolean matchResult = matcher.match(attributes1, attributes2);
				trace.add(String.format("%d: %s match(%s, %s)",
					d.index(), matchResult, attributes1, attributes2));
				return matchResult;
			})
			.toList();
	}

	private boolean evaluate(int i, Policy policy, Request request) {
		return evaluate(i, policy.rules(), request);
	}

	private boolean evaluate(int index, Rules rules, Request request) {
		return rules.all().stream()
			.anyMatch(rule -> evaluate(index, rule, request));
	}

	private boolean evaluate(int index, Rule rule, Request request) {
		try {
			boolean ruleResult = rule.getExpression().evaluate(new EvaluationContext() {
				@Override
				public Object attribute(String name) throws UndefinedName {
					var value = request.resource().name(name);
					if (value == null)
						throw new UndefinedName(name);
					return value;
				}
			});
			trace.add(String.format("%d: expression %s -> %s", index, rule, ruleResult));
			return ruleResult;
		} catch (Exception e) {
			trace.add(String.format("%d: expression %s -> false: %s", index, rule, e.getMessage()));
			return false;
		}
	}

	public Trace getTrace() {
		return trace;
	}
}
