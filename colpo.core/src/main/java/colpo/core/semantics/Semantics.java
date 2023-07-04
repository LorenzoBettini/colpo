package colpo.core.semantics;

import java.util.stream.IntStream;

import colpo.core.AttributeMatcher;
import colpo.core.ParticipantIndex;
import colpo.core.ParticipantSuchThat;
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

	public Semantics(Policies policies) {
		this.policies = policies;
	}

	public boolean evaluate(Request request) {
		// REMEMBER: our indexes start from 1
		var all = policies.all();
		var from = request.from();
		// exclude the policy of the requester
		var policyIndexes = IntStream.range(0, all.size())
				.filter(i -> (i + 1) != request.requester().index());
		if (from instanceof ParticipantSuchThat fromSuchThat && fromSuchThat.getQuantifier() == Quantifier.ALL) {
			return policyIndexes
				.allMatch(i -> evaluate(i + 1, all.get(i), request));
		}
		return policyIndexes
			.anyMatch(i -> evaluate(i + 1, all.get(i), request));
	}

	private boolean evaluate(int i, Policy policy, Request request) {
		var from = request.from();
		if (from instanceof ParticipantIndex index && index.index() == i) {
			// this check is only for requests generated during the evaluation
			// of an exchange: users cannot specify an index for "from"
			return evaluate(i, policy.rules(), request);
		} else if (from instanceof ParticipantSuchThat fromSuchThat) {
			if (!matcher.match(fromSuchThat.getAttributes(), policy.party()))
				return false;
			return evaluate(i, policy.rules(), request);
		}
		return false;
	}

	private boolean evaluate(int i, Rules rules, Request request) {
		return rules.all().stream()
			.anyMatch(rule -> evaluate(i, rule, request));
	}

	private boolean evaluate(int i, Rule rule, Request request) {
		try {
			return rule.getExpression().evaluate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}
