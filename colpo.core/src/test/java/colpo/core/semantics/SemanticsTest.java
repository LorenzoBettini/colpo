package colpo.core.semantics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.Attributes;
import colpo.core.ExpressionWithDescription;
import colpo.core.ParticipantIndex;
import colpo.core.ParticipantSuchThat;
import colpo.core.ParticipantSuchThat.Quantifier;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(() -> true, "true");
	private static final ExpressionWithDescription FALSE =
			new ExpressionWithDescription(() -> false, "false");

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
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule(TRUE))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(TRUE))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=false]]
		2 = Policy[party=[(name : Bob)], rules=[resource=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=true]]
		""");
		// from as index is only generated using exchange
		// so this is just an internal test
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes(),
				new ParticipantIndex(2) // from(r) == Bob (expression = true)
			),
			"""
			evaluating Request[requester=1, resource=[], from=2]
			  2: expression resource=true -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes(),
				new ParticipantIndex(3) // from(r) == Carl (expression = true)
			),
			"""
			evaluating Request[requester=1, resource=[], from=3]
			  3: expression resource=true -> true
			result: true
			"""
		);
		// a requester should not refer to him/herself
		// this is just an internal test to check correct use of from
		assertResultFalse(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes(),
				new ParticipantIndex(1) // from(r) == Alice (expression = false)
			),
			"""
			evaluating Request[requester=1, resource=[], from=1]
			  1: expression resource=false -> false
			result: false
			"""
		);
	}

	@Test
	void shouldCheckAttributesMatch() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(TRUE))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(TRUE))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice)], rules=[resource=false]]
			2 = Policy[party=[(role : Provider), (resource/name : aResource)], rules=[resource=true]]
			3 = Policy[party=[(name : Bob), (role : Provider), (resource/name : aResource)], rules=[resource=true]]
			""");
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: anySuchThat (name : "Bob"))
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ANY,
						new Attributes()
							.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=anySuchThat: [(name : Bob)]]
			  2: false match([(name : Bob)], [(role : Provider), (resource/name : aResource)])
			  3: true match([(name : Bob)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			  3: expression resource=true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (role : "Provider"))
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ALL,
						new Attributes()
							.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(role : Provider)]]
			  2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			  3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			  2: expression resource=true -> true
			  3: expression resource=true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (name : "Bob"))
		// even if there's only one Bob
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ALL,
						new Attributes()
							.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(name : Bob)]]
			  2: false match([(name : Bob)], [(role : Provider), (resource/name : aResource)])
			  3: true match([(name : Bob)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			  3: expression resource=true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (name : "Carl"))
		// and there's no Carl
		assertResultFalse(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ALL,
						new Attributes()
							.add("name", "Carl"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(name : Carl)]]
			  2: false match([(name : Carl)], [(role : Provider), (resource/name : aResource)])
			  3: false match([(name : Carl)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			result: false
			"""
		);
	}

	@Test
	void shouldCheckExpression() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(TRUE))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice)], rules=[resource=false]]
			2 = Policy[party=[(role : Provider), (resource/name : aResource)], rules=[resource=false]]
			3 = Policy[party=[(name : Bob), (role : Provider), (resource/name : aResource)], rules=[resource=true]]
			""");
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (role : "Provider"))
		// fails because the first provider has a rule evaluating to false
		// and we asked allSuchThat
		assertResultFalse(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ALL,
						new Attributes()
							.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(role : Provider)]]
			  2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			  3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			  2: expression resource=false -> false
			result: false
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (role : "Provider"))
		// succeeds because even if the first provider has a rule evaluating to false
		// and we asked allSuchThat
		assertResultTrue(
			new Request(
				new ParticipantIndex(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new ParticipantSuchThat(Quantifier.ANY,
						new Attributes()
							.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=anySuchThat: [(role : Provider)]]
			  2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			  3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			  2: expression resource=false -> false
			  3: expression resource=true -> true
			result: true
			"""
		);
	}

	private void assertPolicies(String expected) {
		assertEquals(expected, policies.description());
	}

	private void assertResultTrue(Request request, String expectedTrace) {
		assertAll(
			() -> assertTrue(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}

	private void assertResultFalse(Request request, String expectedTrace) {
		assertAll(
			() -> assertFalse(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}
}
