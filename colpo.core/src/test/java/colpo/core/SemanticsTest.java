package colpo.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.ParticipantSuchThat.Quantifier;
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
	void shouldCheckRequestFrom() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(() -> false))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule(() -> true))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(() -> true))));
		// from as index is only generated using exchange
		// so this is just an internal test
		assertTrue(semantics.evaluate(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes(),
				new ParticipantIndex(2) // from(r) == Bob (expression = true)
			)
		));
		// a requester should not refer to him/herself
		// this is just an internal test to check correct use of from
		assertFalse(semantics.evaluate(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes(),
				new ParticipantIndex(1) // from(r) == Alice (expression = false)
			)
		));
	}

	@Test
	void shouldCheckAttributesMatch() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(() -> false))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(() -> true))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(() -> true))));
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: anySuchThat (name : "Bob"))
		assertTrue(semantics.evaluate(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ANY,
						new Attributes()
							.add("name", "Bob"))
			)
		));
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (role : "Provider"))
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		assertTrue(semantics.evaluate(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ALL,
						new Attributes()
							.add("role", "Provider"))
			)
		));
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (name : "Bob"))
		// fails because there's only one Bob
		assertTrue(semantics.evaluate(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ANY,
						new Attributes()
							.add("name", "Bob"))
			)
		));
	}
}
