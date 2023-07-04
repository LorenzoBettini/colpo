package colpo.core.semantics;

import java.util.stream.IntStream;

import colpo.core.AttributeMatcher;
import colpo.core.ParticipantIndex;
import colpo.core.ParticipantSuchThat;
import colpo.core.ParticipantVisitor;
import colpo.core.ParticipantSuchThat.Quantifier;
import colpo.core.Policies;
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
		var all = policies.all();
		var from = request.from();
		var result = from.accept(new ParticipantVisitor<Boolean>() {
			@Override
			public Boolean visit(ParticipantIndex participantIndex) {
				var index = participantIndex.index();
				return evaluate(index, all.get(index - 1), request);
			}

			@Override
			public Boolean visit(ParticipantSuchThat participantSuchThat) {
				// exclude the policy of the requester
				var policyIndexes = IntStream.range(0, all.size())
						.filter(i -> (i + 1) != request.requester().index());
				if (participantSuchThat.getQuantifier() == Quantifier.ANY) {
					return policyIndexes
						.anyMatch(i -> evaluate(i + 1, all.get(i), request));
				}
				return policyIndexes
					.allMatch(i -> evaluate(i + 1, all.get(i), request));
			}
		});
		trace.removeIndent();
		trace.add(String.format("result: %s", result));
		return result;
	}

	private boolean evaluate(int i, Policy policy, Request request) {
		var from = request.from();
		return from.accept(new ParticipantVisitor<Boolean>() {
			@Override
			public Boolean visit(ParticipantIndex participantIndex) {
				return evaluate(i, policy.rules(), request);
			}

			@Override
			public Boolean visit(ParticipantSuchThat participantSuchThat) {
				if (!matcher.match(participantSuchThat.getAttributes(), policy.party()))
					return false;
				return evaluate(i, policy.rules(), request);
			}
		});
	}

	private boolean evaluate(int i, Rules rules, Request request) {
		return rules.all().stream()
			.anyMatch(rule -> evaluate(i, rule, request));
	}

	private boolean evaluate(int i, Rule rule, Request request) {
		try {
			boolean ruleResult = rule.getExpression().evaluate();
			trace.add(String.format("expression %s -> %s", ruleResult, rule));
			return ruleResult;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public Trace getTrace() {
		return trace;
	}
}
