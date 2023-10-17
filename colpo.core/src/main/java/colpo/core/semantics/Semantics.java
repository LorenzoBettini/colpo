package colpo.core.semantics;

import static colpo.core.Participant.index;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import colpo.core.AndExchange;
import colpo.core.AttributeMatcher;
import colpo.core.Attributes;
import colpo.core.CompositeExchange;
import colpo.core.ContextHandler;
import colpo.core.Exchange;
import colpo.core.FromParticipant;
import colpo.core.IndexParticipant;
import colpo.core.OrExchange;
import colpo.core.ParticipantInterface;
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
	private ContextHandler contextHandler = EMPTY_CONTEXT_HANDLER;

	private static final ContextHandler EMPTY_CONTEXT_HANDLER = new ContextHandler();

	public Semantics(Policies policies) {
		this.policies = policies;
	}

	public Semantics contextHandler(ContextHandler contextHandler) {
		this.contextHandler = contextHandler;
		return this;
	}

	public boolean evaluate(Request request) {
		trace.reset();
		return evaluate(request, new LinkedHashSet<>());
	}

	private boolean evaluate(Request request, Set<Request> requests) {
		trace.addAndThenIndent(String.format("evaluating %s", request));
		var from = request.from();
		var index = from.getIndex();
		boolean result = false;
		if (index > 0)
			result = evaluate(index, policies.getByIndex(index), request, requests);
		else {
			trace.addAndThenIndent("finding matching policies");
			var policiesToEvaluate = policiesToEvaluate(request.requester(), from);
			trace.removeIndent();
			if (policiesToEvaluate.isEmpty()) {
				result = false;
			} else {
				Predicate<PolicyData> evaluatePredicate =
						d -> evaluate(d.index(), d.policy(),
								request.withFrom(d.index()), requests);
				if (from.isAll()) {
					result = policiesToEvaluate.stream()
						.allMatch(evaluatePredicate);
				} else {
					result = policiesToEvaluate.stream()
						.anyMatch(evaluatePredicate);
				}
			}
		}
		trace.removeIndentAndThenAdd(String.format("result: %s", result));
		return result;
	}

	private Collection<PolicyData> policiesToEvaluate(ParticipantInterface requester,
			ParticipantInterface from) {
		return policies.getPolicyData()
			.filter(d -> d.index() != requester.getIndex())
			.filter(d -> {
				var attributes1 = from.getAttributes();
				var attributes2 = d.policy().party();
				return tryMatch("policy " + d.index(), "from", attributes1, attributes2);
			})
			.toList();
	}

	private boolean tryMatch(String prefix, String description, Attributes attributes1, Attributes attributes2) {
		boolean matchResult = matcher.match(attributes1, attributes2);
		trace.add(String.format("%s: %s match(%s, %s) -> %s",
			prefix, description, attributes1, attributes2, matchResult));
		return matchResult;
	}

	private boolean evaluate(int policyIndex, Policy policy, Request request, Set<Request> requests) {
		return evaluate(policyIndex, policy.rules(), request, requests);
	}

	private boolean evaluate(int policyIndex, Rules rules, Request request, Set<Request> requests) {
		return rules.getRuleData()
			.anyMatch(r -> evaluate(policyIndex, r.index(), r.rule(), request, requests));
	}

	private boolean evaluate(int policyIndex, int ruleIndex, Rule rule, Request request, Set<Request> requests) {
		trace.addAndThenIndent(String.format("policy %d: evaluating %s",
				policyIndex, request));
		try {
			boolean result = tryMatch(traceForRule(policyIndex, ruleIndex), "resource", request.resource(), rule.getResource());
			if (!result)
				return false;
			result = rule.getCondition().evaluate(
				name -> Stream.of(
							request.resource(), request.credentials(),
							contextHandler.ofParty(policyIndex))
					.map(attributes -> attributes.name(name))
					.filter(Objects::nonNull)
					.findFirst()
					.orElseThrow(() -> new UndefinedName(name))
			);
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), result));
			if (!result)
				return false;
			result = evaluateExchange(policyIndex, ruleIndex, rule.getExchange(), request, requests);
			return result;
		} catch (Exception e) {
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), e.getMessage()));
			return false;
		} finally {
			trace.removeIndent();
		}
	}

	private boolean evaluateExchange(int policyIndex, int ruleIndex, Exchange exchange, Request request, Set<Request> requests) {
		boolean result = true;
		requests.add(request);

		var isComposite = exchange instanceof CompositeExchange;

		if (isComposite) {
			trace.addAndThenIndent(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));
		}

		if (exchange instanceof OrExchange orExchange) {
			result = evaluateExchange(policyIndex, ruleIndex, orExchange.left(), request, requests);
			if (!result) {
				trace.addInPreviousIndent(String.format("%s: OR", traceForRule(policyIndex, ruleIndex)));
				result = evaluateExchange(policyIndex, ruleIndex, orExchange.right(), request, requests);
			}
		} else if (exchange instanceof AndExchange orExchange) {
			result = evaluateExchange(policyIndex, ruleIndex, orExchange.left(), request, requests);
			if (result) {
				trace.addInPreviousIndent(String.format("%s: AND", traceForRule(policyIndex, ruleIndex)));
				result = evaluateExchange(policyIndex, ruleIndex, orExchange.right(), request, requests);
			}
		} else if (exchange instanceof SingleExchange singleExchange)
			result = evaluate(policyIndex, ruleIndex, singleExchange, request, requests);
		else // exchange is null
			result = true;

		if (isComposite) {
			trace.removeIndentAndThenAdd(String.format("%s: END Exchange -> %s", traceForRule(policyIndex, ruleIndex), result));
		}

		requests.remove(request);

		return result;
	}

	private boolean evaluate(int policyIndex, int ruleIndex, SingleExchange exchange, Request request, Set<Request> requests) {
		trace.add(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));

		var exchangeFrom = exchange.from();
		var exchangeTo = exchange.to();

		Collection<Integer> fromIndexes;
		Collection<Integer> toIndexes;

		if (exchangeFrom.isRequester()) {
			fromIndexes = Collections.singleton(request.requester().getIndex());
		} else {
			fromIndexes = computeIndexes(exchangeFrom.getAttributes());
		}

		if (exchangeTo.isMe()) {
			toIndexes = Collections.singleton(policyIndex);
		} else {
			toIndexes = computeIndexes(exchangeTo.getAttributes());
		}

		if (toIndexes.isEmpty()) {
			trace.add(String.format("%s: satisfied: no one to exchange", traceForRule(policyIndex, ruleIndex)));
			return true; // there's no one to satisfy
		}
		// this check would be implied by the later
		// atLeastOneRequest.hasBeenGenerated for from: allSuchThat
		// but this way we can give a more informative message
		if (fromIndexes.isEmpty()) {
			trace.add(String.format("%s: not satisfied: no one from exchange", traceForRule(policyIndex, ruleIndex)));
			return false; // no one can satisfy
		}

		BiPredicate<Integer, Integer> differentIndexes = (i1, i2) -> !i1.equals(i2);

		Predicate<Integer> innerOperation;

		// to keep track of the fact that at least one inner
		// "loop" is executed; that's required for the semantics of allMatch
		// see below
		var atLeastOneRequest = new Object() {
			boolean hasBeenGenerated = false;
		};

		BiPredicate<Integer, Integer> innerPredicate = (fromIndex, toIndex) -> {
			// record that at least one inner loop has been executed
			// useful for the external allMatch case
			// see below
			atLeastOneRequest.hasBeenGenerated = true;
			return evaluateExchangeRequest(policyIndex, ruleIndex, exchange,
				index(toIndex), index(fromIndex), requests);
		};

		if (exchangeTo.isAll()) {
			innerOperation = fromIndex ->
				toIndexes.stream()
					.filter(toIndex -> differentIndexes.test(toIndex, fromIndex))
					.allMatch(toIndex -> innerPredicate.test(fromIndex, toIndex));
		} else {
			innerOperation = fromIndex ->
				toIndexes.stream()
					.filter(toIndex -> differentIndexes.test(toIndex, fromIndex))
					.anyMatch(toIndex -> innerPredicate.test(fromIndex, toIndex));
		}

		var result = false;

		if (exchangeFrom.isAll()) {
			result = fromIndexes.stream()
				.allMatch(innerOperation);
			// this additional check is required because allMatch returns
			// true if the stream was empty
			if (!atLeastOneRequest.hasBeenGenerated) {
				trace.add(String.format("%s: not satisfied: no request could be generated", traceForRule(policyIndex, ruleIndex)));
				result = false;
			}
		} else {
			result = fromIndexes.stream()
				.anyMatch(innerOperation);
		}

		return result;
	}

	private List<Integer> computeIndexes(Attributes attributesToMatch) {
		return policies.getPolicyData()
				.filter(d -> {
					var attributes1 = attributesToMatch;
					var attributes2 = d.policy().party();
					return tryMatch("policy " + d.index(), "from", attributes1, attributes2);
				})
				.map(PolicyData::index)
				.toList();
	}

	private boolean evaluateExchangeRequest(int policyIndex,
			int ruleIndex,
			SingleExchange exchange,
			IndexParticipant exchangeRequestRequester,
			FromParticipant exchangeRequestFrom,
			Set<Request> requests) {
		var exchangeRequest = new Request(
			exchangeRequestRequester,
			exchange.resource(),
			exchange.credentials(),
			exchangeRequestFrom);
		boolean result = true;
		if (requests.contains(exchangeRequest)) {
			trace.add(String.format("%s: already found %s", traceForRule(policyIndex, ruleIndex), exchangeRequest));
		} else {
			result = evaluate(exchangeRequest, requests);
		}
		return result;
	}

	private String traceForRule(int policyIndex, int ruleIndex) {
		return String.format("rule %d.%d", policyIndex, ruleIndex);
	}

	public Trace getTrace() {
		return trace;
	}
}
