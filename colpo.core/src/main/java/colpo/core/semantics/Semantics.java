package colpo.core.semantics;

import static colpo.core.Participant.index;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import colpo.core.AndExchange;
import colpo.core.AttributeMatcher;
import colpo.core.Attributes;
import colpo.core.CompositeExchange;
import colpo.core.EvaluationContext;
import colpo.core.Exchange;
import colpo.core.OrExchange;
import colpo.core.Participant;
import colpo.core.Participant.Quantifier;
import colpo.core.Policies;
import colpo.core.Policies.PolicyData;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;
import colpo.core.SingleExchange;

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
		trace.addAndThenIndent(String.format("evaluating %s", request));
		var from = request.from();
		var index = from.getIndex();
		boolean result = false;
		if (index > 0)
			result = evaluate(index, policies.getByIndex(index), request, R);
		else {
			trace.addAndThenIndent("finding matching policies");
			var policiesToEvaluate = policiesToEvaluate(request.requester(), from);
			trace.removeIndent();
			if (policiesToEvaluate.isEmpty()) {
				result = false;
			} else {
				Predicate<PolicyData> evaluatePredicate =
						d -> evaluate(d.index(), d.policy(),
								request.withFrom(d.index()), R);
				if (from.getQuantifier() == Quantifier.ANY) {
					result = policiesToEvaluate.stream()
						.anyMatch(evaluatePredicate);
				} else {
					result = policiesToEvaluate.stream()
						.allMatch(evaluatePredicate);
				}
			}
		}
		trace.removeIndentAndThenAdd(String.format("result: %s", result));
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
		return evaluate(policyIndex, policy.rules(), request, R);
	}

	private boolean evaluate(int policyIndex, Rules rules, Request request, Set<Request> R) {
		return rules.getRuleData()
			.anyMatch(r -> evaluate(policyIndex, r.index(), r.rule(), request, R));
	}

	private boolean evaluate(int policyIndex, int ruleIndex, Rule rule, Request request, Set<Request> R) {
		trace.addAndThenIndent(String.format("policy %d: evaluating %s",
				policyIndex, request));
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
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), result));
			if (!result)
				return false;
			result = evaluateExchange(policyIndex, ruleIndex, rule.getExchange(), request, R);
			return result;
		} catch (Exception e) {
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), e.getMessage()));
			return false;
		} finally {
			trace.removeIndent();
		}
	}

	private boolean evaluateExchange(int policyIndex, int ruleIndex, Exchange exchange, Request request, Set<Request> R) {
		boolean result = true;
		R.add(request);

		var isComposite = exchange instanceof CompositeExchange;

		if (isComposite) {
			trace.addAndThenIndent(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));
		}

		if (exchange instanceof OrExchange orExchange) {
			result = evaluateExchange(policyIndex, ruleIndex, orExchange.left(), request, R);
			if (!result) {
				trace.addInPreviousIndent(String.format("%s: OR", traceForRule(policyIndex, ruleIndex)));
				result = evaluateExchange(policyIndex, ruleIndex, orExchange.right(), request, R);
			}
		} else if (exchange instanceof AndExchange orExchange) {
			result = evaluateExchange(policyIndex, ruleIndex, orExchange.left(), request, R);
			if (result) {
				trace.addInPreviousIndent(String.format("%s: AND", traceForRule(policyIndex, ruleIndex)));
				result = evaluateExchange(policyIndex, ruleIndex, orExchange.right(), request, R);
			}
		} else if (exchange instanceof SingleExchange singleExchange)
			result = evaluate(policyIndex, ruleIndex, singleExchange, request, R);
		else // exchange is null
			result = true;

		if (isComposite) {
			trace.removeIndentAndThenAdd(String.format("%s: END Exchange -> %s", traceForRule(policyIndex, ruleIndex), result));
		}

		R.remove(request);

		return result;
	}

	private boolean evaluate(int policyIndex, int ruleIndex, SingleExchange exchange, Request request, Set<Request> R) {
		trace.add(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));

		Participant exchangeRequestRequester = index(policyIndex);
		Participant exchangeRequestFrom = request.requester();

		var exchangeFrom = exchange.from();
		var exchangeTo = exchange.to();

		if (!exchangeFrom.isRequester() && !exchangeTo.isMe()) {
			// TODO: assumed to be both allSuchThat
			var fromSet = computeIndexSet(-1, exchangeFrom.getAttributes());
			var toSet = computeIndexSet(-1, exchangeTo.getAttributes());

			return fromSet.stream()
				.allMatch(d1 -> 
					toSet.stream().allMatch(d2 ->
					evaluateExchangeRequest(policyIndex, ruleIndex, exchange,
						index(d2.index()),
						index(d1.index()), R)));
		}

		if (!exchangeFrom.isRequester()) {
			var fromSet = computeIndexSet(policyIndex, exchangeFrom.getAttributes());
			Predicate<PolicyData> evaluatePredicate =
				d -> evaluateExchangeRequest(policyIndex, ruleIndex, exchange,
					exchangeRequestRequester,
					index(d.index()), R);
			if (exchangeFrom.getQuantifier() == Quantifier.ALL) {
				return fromSet.stream()
					.allMatch(evaluatePredicate);
			}
			return fromSet.stream()
					.anyMatch(evaluatePredicate);
		}

		if (!exchangeTo.isMe()) {
			var toSet = computeIndexSet(exchangeRequestFrom.getIndex(), exchangeTo.getAttributes());

			if (toSet.isEmpty()) {
				trace.add(String.format("%s: satisfied: no one to exchange", traceForRule(policyIndex, ruleIndex)));
				return true; // there's no one to satisfy
			}

			Predicate<PolicyData> evaluatePredicate =
				d -> evaluateExchangeRequest(policyIndex, ruleIndex, exchange,
					index(d.index()),
					exchangeRequestFrom, R);
			if (exchangeTo.getQuantifier() == Quantifier.ALL) {
				return toSet.stream()
					.allMatch(evaluatePredicate);
			}
			return toSet.stream()
					.anyMatch(evaluatePredicate);
			
		}

		return evaluateExchangeRequest(policyIndex, ruleIndex, exchange, exchangeRequestRequester, exchangeRequestFrom, R);
	}

	private List<PolicyData> computeIndexSet(int indexToSkip, Attributes attributesToMatch) {
		return policies.getPolicyData()
			.filter(d -> d.index() != indexToSkip)
			.filter(d -> {
				var attributes1 = attributesToMatch;
				var attributes2 = d.policy().party();
				return tryMatch("policy", d.index(), "from", attributes1, attributes2);
			})
			.toList();
	}

	private boolean evaluateExchangeRequest(int policyIndex, int ruleIndex, SingleExchange exchange,
			Participant exchangeRequestRequester, Participant exchangeRequestFrom, Set<Request> R) {
		var exchangeRequest = new Request(
			exchangeRequestRequester,
			exchange.resource(),
			exchange.credentials(),
			exchangeRequestFrom);
		boolean result = true;
		if (R.contains(exchangeRequest)) {
			trace.add(String.format("%s: already found %s", traceForRule(policyIndex, ruleIndex), exchangeRequest));
		} else {
			result = evaluate(exchangeRequest, R);
		}
		return result;
	}

	private String traceForRule(int policyIndex, int ruleIndex) {
		return String.format("rule %d", ruleIndex);
	}

	public Trace getTrace() {
		return trace;
	}
}
