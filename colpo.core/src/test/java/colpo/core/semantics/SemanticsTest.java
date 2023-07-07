package colpo.core.semantics;

import static colpo.core.Participant.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.Attributes;
import colpo.core.Exchange;
import colpo.core.ExpressionWithDescription;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(context -> true, "true");
	private static final ExpressionWithDescription FALSE =
			new ExpressionWithDescription(context -> false, "false");

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
				index(1), // Alice
				new Attributes(),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[], from=2]
			  2: expression resource=true -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				index(3)
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
				index(1), // Alice
				new Attributes(),
				index(1)
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
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				anySuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=anySuchThat: [(name : Bob)]]
			  finding matching policies
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
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
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
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				allSuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(name : Bob)]]
			  finding matching policies
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
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				allSuchThat(new Attributes()
					.add("name", "Carl"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], from=allSuchThat: [(name : Carl)]]
			  finding matching policies
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
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/usage")),
							"resource/usage = read")
						))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/usage")) ||
									"write".equals(c.attribute("resource/usage")),
							"resource/usage = read or resource/usage = write")
						))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Carl")
					.add("role", "SpecialProvider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/access")),
							"resource/access = read")
						))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice)], rules=[resource=false]]
			2 = Policy[party=[(role : Provider), (resource/name : aResource)], rules=[resource=resource/usage = read]]
			3 = Policy[party=[(name : Bob), (role : Provider), (resource/name : aResource)], rules=[resource=resource/usage = read or resource/usage = write]]
			4 = Policy[party=[(name : Carl), (role : SpecialProvider), (resource/name : aResource)], rules=[resource=resource/access = read]]
			""");
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource=resource/usage = read -> false
			result: false
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "read"),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : read)], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource=resource/usage = read -> true
			  3: expression resource=resource/usage = read or resource/usage = write -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				anySuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=anySuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource=resource/usage = read -> false
			  3: expression resource=resource/usage = read or resource/usage = write -> true
			result: true
			"""
		);
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				anySuchThat(new Attributes()
					.add("role", "SpecialProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=anySuchThat: [(role : SpecialProvider)]]
			  finding matching policies
			    2: false match([(role : SpecialProvider)], [(role : Provider), (resource/name : aResource)])
			    3: false match([(role : SpecialProvider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: true match([(role : SpecialProvider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  4: expression resource=resource/access = read -> false: Undefined name: resource/access
			result: false
			"""
		);
	}

	@Test
	void simpleExchange() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider")
					.add("resource/type", "printer"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider")
					.add("resource/type", "paper"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "printer"),
							me())))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider), (resource/type : printer)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider), (resource/type : paper)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    1: true match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider), (resource/type : printer)])
			  1: expression resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME] -> true
			  1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
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
