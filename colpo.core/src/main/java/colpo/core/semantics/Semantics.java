package colpo.core.semantics;

import java.util.stream.IntStream;

import colpo.core.ParticipantIndex;
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

	public Semantics(Policies policies) {
		this.policies = policies;
	}

	public boolean evaluate(Request request) {
		var all = policies.all();
		return IntStream.range(0, all.size())
			// our indexes start from 1
			.anyMatch(i -> evaluate(i + 1, all.get(i), request));
	}

	private boolean evaluate(int i, Policy policy, Request request) {
		var from = request.from();
		if (from instanceof ParticipantIndex index && index.index() == i) {
			// this check is only for requests generated during the evaluation
			// of an exchange: users cannot specify an index for "from"
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
