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

import colpo.core.AndExchange;
import colpo.core.Attributes;
import colpo.core.ExpressionWithDescription;
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

	@Test
	void secondScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> attributes.name("company").equals("RabbitService"),
							"company = RabbitService")))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> !attributes.name("company").equals("RabbitService"),
							"company != RabbitService"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Prato"),
							requester())))
					))
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
		assertPolicies("""
		1 = Policy[party=[(service : delivery), (company : RabbitService)], rules=[resource=[(type : addrInfo), (city : Lucca)], condition=company = RabbitService, resource=[(type : addrInfo), (city : Lucca)], condition=company != RabbitService, exchange=Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], from=REQUESTER]]]
		2 = Policy[party=[(service : delivery), (company : FastAndFurious)], rules=[resource=[(type : addrInfo), (city : Prato)], condition=true, exchange=OR(Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], from=REQUESTER], Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=REQUESTER])]]
		""");
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
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			    rule 2.1: OR
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition company = RabbitService -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.2: condition company != RabbitService -> true
			          rule 1.2: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], from=REQUESTER]
			          rule 1.2: already found Request[requester=1, resource=[(type : addrInfo), (city : Prato)], from=2]
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			"""
		);
	}

	@Test
	void thirdScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> attributes.name("company").equals("RabbitService"),
							"company = RabbitService")))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> !attributes.name("company").equals("RabbitService"),
							"company != RabbitService"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Prato"),
							requester())))
					))
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
						new AndExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								anySuchThat(new Attributes()
									.add("service", "delivery")
									.add("company", "RabbitService"))),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Grosseto"),
								anySuchThat(new Attributes()
										.add("service", "delivery")
										.add("company", "RabbitService")))
						)
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Grosseto")
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
			    policy 3: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : RabbitService)]) -> false
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating AND(Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=anySuchThat: [(service : delivery), (company : RabbitService)]], Exchange[to=ME, resource=[(type : addrInfo), (city : Grosseto)], from=anySuchThat: [(service : delivery), (company : RabbitService)]])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], from=anySuchThat: [(service : delivery), (company : RabbitService)]]
			      policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			      policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition company = RabbitService -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.2: condition company != RabbitService -> true
			          rule 1.2: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], from=REQUESTER]
			          rule 1.2: already found Request[requester=1, resource=[(type : addrInfo), (city : Prato)], from=2]
			      result: true
			    rule 2.1: AND
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Grosseto)], from=anySuchThat: [(service : delivery), (company : RabbitService)]]
			      policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			      policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Lucca)]) -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], from=3]
			        policy 3: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], from=3]
			          rule 3.1: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Grosseto)]) -> true
			          rule 3.1: condition true -> true
			      result: true
			    rule 2.1: END Exchange -> true
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
