package colpo.core.semantics;

import colpo.core.AttributeMatcher;
import colpo.core.ParticipantIndex;
import colpo.core.ParticipantSuchThat;
import colpo.core.ParticipantSuchThat.Quantifier;
import colpo.core.ParticipantVisitor;
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
		var from = request.from();
		var result = from.accept(new ParticipantVisitor<Boolean>() {
			@Override
			public Boolean visit(ParticipantIndex participantIndex) {
				var index = participantIndex.index();
				return evaluate(index, policies.getByIndex(index), request);
			}

			@Override
			public Boolean visit(ParticipantSuchThat participantSuchThat) {
				// exclude the policy of the requester
				var policyData = policies.getPolicyData()
						.filter(d -> d.index() != request.requester().index());
				if (participantSuchThat.getQuantifier() == Quantifier.ANY) {
					return policyData
						.anyMatch(d -> evaluate(d.index(), d.policy(), request));
				}
				return policyData
					.allMatch(d -> evaluate(d.index(), d.policy(), request));
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
				var attributes1 = participantSuchThat.getAttributes();
				var attributes2 = policy.party();
				boolean matchResult = matcher.match(attributes1, attributes2);
				trace.add(String.format("%s match(%s, %s)", matchResult, attributes1, attributes2));
				if (!matchResult)
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
			trace.add(String.format("expression %s -> %s", rule, ruleResult));
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
