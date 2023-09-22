package colpo.core.semantics;

import static colpo.core.Participant.allSuchThat;
import static colpo.core.Participant.anySuchThat;
import static colpo.core.Participant.index;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.Attributes;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

public class SemanticsTest {

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
					.add(new Rule())))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule())));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(name : Bob)], rules=[resource=[], condition=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=[], condition=true]]
		""");
		// from as index is only generated using exchange
		// so this is just an internal test
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=2]
			  2: condition true -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=3]
			  3: condition true -> true
			result: true
			"""
		);
	}

	@Test
	void shouldCheckAnyAndAllSuchThat() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider"),
				new Rules()
					.add(new Rule())));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(role : Provider)], rules=[resource=[], condition=true]]
		3 = Policy[party=[(name : Bob), (role : Provider)], rules=[resource=[], condition=true]]
			""");
		// Alice requests
		// ( resource: empty, from: anySuchThat (name : "Bob"))
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=anySuchThat: [(name : Bob)]]
			  finding matching policies
			    2: false match([(name : Bob)], [(role : Provider)])
			    3: true match([(name : Bob)], [(name : Bob), (role : Provider)])
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (role : "Provider"))
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider)])
			  2: condition true -> true
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (name : "Bob"))
		// even if there's only one Bob
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=allSuchThat: [(name : Bob)]]
			  finding matching policies
			    2: false match([(name : Bob)], [(role : Provider)])
			    3: true match([(name : Bob)], [(name : Bob), (role : Provider)])
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (name : "Carl"))
		// and there's no Carl
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Carl"))
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=allSuchThat: [(name : Carl)]]
			  finding matching policies
			    2: false match([(name : Carl)], [(role : Provider)])
			    3: false match([(name : Carl)], [(name : Bob), (role : Provider)])
			result: false
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
