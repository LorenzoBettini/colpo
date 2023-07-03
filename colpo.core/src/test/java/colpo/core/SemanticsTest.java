package colpo.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.semantics.Semantics;

class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void shouldEvaluateRuleExpression() {
		policies.add(
			new Policy(
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(() -> true))))
		.add(
			new Policy(
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule(() -> true))));
		assertTrue(semantics.evaluate(
			new Request(
				new ParticipantIndex(1),
				new Attributes()
					.add("name", "Bob"),
				new ParticipantIndex(2)
			)
		));
	}

}
