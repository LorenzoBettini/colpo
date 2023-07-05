package colpo.core.semantics;

import java.util.Collection;

import colpo.core.AttributeMatcher;
import colpo.core.ParticipantIndex;
import colpo.core.ParticipantSuchThat;
import colpo.core.ParticipantSuchThat.Quantifier;
import colpo.core.ParticipantVisitor;
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
		var result = from.accept(new ParticipantVisitor<Boolean>() {
			@Override
			public Boolean visit(ParticipantIndex participantIndex) {
				var index = participantIndex.index();
				return evaluate(index, policies.getByIndex(index), request);
			}

			@Override
			public Boolean visit(ParticipantSuchThat participantSuchThat) {
				trace.add("finding matching policies");
				trace.addIndent();
				var policiesToEvaluate = policiesToEvaluate(request.requester(), participantSuchThat);
				trace.removeIndent();
				if (policiesToEvaluate.isEmpty())
					return false;
				if (participantSuchThat.getQuantifier() == Quantifier.ANY) {
					return policiesToEvaluate.stream()
						.anyMatch(d -> evaluate(d.index(), d.policy(), request));
				}
				return policiesToEvaluate.stream()
					.allMatch(d -> evaluate(d.index(), d.policy(), request));
			}
		});
		trace.removeIndent();
		trace.add(String.format("result: %s", result));
		return result;
	}

	private Collection<PolicyData> policiesToEvaluate(ParticipantIndex requester,
			ParticipantSuchThat from) {
		return policies.getPolicyData()
			.filter(d -> d.index() != requester.index())
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
			boolean ruleResult = rule.getExpression().evaluate();
			trace.add(String.format("%d: expression %s -> %s", index, rule, ruleResult));
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
