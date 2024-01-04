package colpo.core.examples;

import static colpo.core.Participants.anySuchThat;
import static colpo.core.Participants.index;
import static colpo.core.Participants.me;
import static colpo.core.Participants.requester;
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

class CouriersExampleTest {

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
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Pistoia"),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								requester())
						)
					))));

		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Prato"),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating OR(Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], from=REQUESTER], Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=REQUESTER])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			    rule 2.1: OR
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition true -> true
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			"""
		);
	}

	private void assertResultTrue(Request request, String expectedTrace) {
		assertAll(
			() -> assertTrue(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}

}
