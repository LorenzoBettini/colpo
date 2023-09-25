package colpo.core.examples;

import static colpo.core.Participant.anySuchThat;
import static colpo.core.Participant.index;
import static colpo.core.Participant.me;
import static colpo.core.Participant.requester;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.Attributes;
import colpo.core.OrExchange;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;
import colpo.core.SingleExchange;
import colpo.core.semantics.Semantics;

public class CouriersExampleTest {

	private Semantics semantics;
	private Policies policies;

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void firstScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca")
					))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Prato"),
						new OrExchange(
							new SingleExchange(
								requester(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Pistoia"),
								new Attributes(),
								me()),
							new SingleExchange(
								requester(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								new Attributes(),
								me())
						)
					))));
		assertPolicies("""
		1 = Policy[party=[(service : delivery), (company : RabbitService)], rules=[resource=[(type : addrInfo), (city : Lucca)], condition=true]]
		2 = Policy[party=[(service : delivery), (company : FastAndFurious)], rules=[resource=[(type : addrInfo), (city : Prato)], condition=true, exchange=OrExchange[left=Exchange[from=REQUESTER, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], to=ME], right=Exchange[from=REQUESTER, resource=[(type : addrInfo), (city : Lucca)], credentials=[], to=ME]]]]
		""");
		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Prato"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			  policy 2: evaluating rules
			    rule 1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 1: condition true -> true
			    rule 2: evaluating Exchange[from=REQUESTER, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], to=ME]
			    evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], from=1]
			      policy 1: evaluating rules
			        rule 1: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			    result: false
			    rule 2: evaluating Exchange[from=REQUESTER, resource=[(type : addrInfo), (city : Lucca)], credentials=[], to=ME]
			    evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[], from=1]
			      policy 1: evaluating rules
			        rule 1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			        rule 1: condition true -> true
			    result: true
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

}
