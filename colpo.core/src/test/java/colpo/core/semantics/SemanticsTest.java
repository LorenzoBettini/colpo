package colpo.core.semantics;

import static colpo.core.Participant.allSuchThat;
import static colpo.core.Participant.anySuchThat;
import static colpo.core.Participant.index;
import static colpo.core.Participant.me;
import static colpo.core.Participant.requester;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.AndExchange;
import colpo.core.Attributes;
import colpo.core.SingleExchange;
import colpo.core.ExpressionWithDescription;
import colpo.core.OrExchange;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

public class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	private static final ExpressionWithDescription FALSE =
			new ExpressionWithDescription(context -> false, "always false");

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void shouldCheckRequestFromWithIndexes() {
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
			  policy 2: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
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
			  policy 3: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
			result: true
			"""
		);
	}

	@Test
	void shouldCheckRuleMatch() {
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
					.add(new Rule(
						new Attributes()
							.add("paper", "black")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						FALSE
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "David"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white")
							.add("file/format", "PDF"),
						// the expression seems redundant,
						// but it checks that the requester asks
						// for both attributes
						new ExpressionWithDescription(
							c -> c.name("file/format").equals("PDF"),
							"file/format = PDF"
						)
					))))
		.add(
			new Policy( // index 5
				new Attributes()
					.add("name", "Edward"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						new ExpressionWithDescription(
							c -> c.name("job/role").equals("writer"),
							"job/role = writer"
						)
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(name : Bob)], rules=[resource=[(paper : black)], condition=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=[(paper : white)], condition=always false]]
		4 = Policy[party=[(name : David)], rules=[resource=[(paper : white), (file/format : PDF)], condition=file/format = PDF]]
		5 = Policy[party=[(name : Edward)], rules=[resource=[(paper : white)], condition=job/role = writer]]
		""");
		// Alice requests
		// ( resource: (paper : white), from: 2)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes(),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], credentials=[], from=2]
			  policy 2: evaluating rules
			    rule 1: resource match([(paper : white)], [(paper : black)]) -> false
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white), from: 3)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes(),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], credentials=[], from=3]
			  policy 3: evaluating rules
			    rule 1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 1: condition always false -> false
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white), from: 4)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes(),
				index(4)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], credentials=[], from=4]
			  policy 4: evaluating rules
			    rule 1: resource match([(paper : white)], [(paper : white), (file/format : PDF)]) -> true
			    rule 1: condition file/format = PDF -> Undefined name: file/format
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white)(file : PDF), from: 4)
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white")
					.add("file/format", "PDF"),
				new Attributes(),
				index(4)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white), (file/format : PDF)], credentials=[], from=4]
			  policy 4: evaluating rules
			    rule 1: resource match([(paper : white), (file/format : PDF)], [(paper : white), (file/format : PDF)]) -> true
			    rule 1: condition file/format = PDF -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (paper : white), from: 5)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes(),
				index(5)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], credentials=[], from=5]
			  policy 5: evaluating rules
			    rule 1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 1: condition job/role = writer -> Undefined name: job/role
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white), credentials: (job/role : writer), from: 5)
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes()
					.add("job/role", "writer"),
				index(5)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], credentials=[(job/role : writer)], from=5]
			  policy 5: evaluating rules
			    rule 1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 1: condition job/role = writer -> true
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
			    policy 2: from match([(name : Bob)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Bob)], [(name : Bob), (role : Provider)]) -> true
			  policy 3: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
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
			    policy 2: from match([(role : Provider)], [(role : Provider)]) -> true
			    policy 3: from match([(role : Provider)], [(name : Bob), (role : Provider)]) -> true
			  policy 2: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
			  policy 3: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
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
			    policy 2: from match([(name : Bob)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Bob)], [(name : Bob), (role : Provider)]) -> true
			  policy 3: evaluating rules
			    rule 1: resource match([], []) -> true
			    rule 1: condition true -> true
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
			    policy 2: from match([(name : Carl)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Carl)], [(name : Bob), (role : Provider)]) -> false
			result: false
			"""
		);
	}

	@Test
	void simpleExchange() {
		// Alice gives printer provided the requester gives paper
		// Bob gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							new Attributes(),
							me())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 1: condition true -> true
			    result: true
			result: true
			"""
		);
	}

	@Test
	void mutualExchange() {
		// Alice gives printer provided the requester gives paper
		// Bob gives paper provided the requester gives ink
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							new Attributes(),
							me())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								requester(),
								new Attributes()
									.add("resource/type", "ink"),
								new Attributes(),
								me())))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								new Attributes(),
								me())))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : ink)], credentials=[], to=ME]]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], credentials=[], to=ME]]]
			""");
		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 1: condition true -> true
			        rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : ink)], credentials=[], to=ME]
			        evaluating Request[requester=2, resource=[(resource/type : ink)], credentials=[], from=1]
			          policy 1: evaluating rules
			            rule 1: resource match([(resource/type : ink)], [(resource/type : printer)]) -> false
			        result: false
			    result: false
			result: false
			"""
		);
		assertResultTrue(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 1: condition true -> true
			        rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], credentials=[], to=ME]
			        rule 1: satisfied Request[requester=3, resource=[(resource/type : printer)], credentials=[], from=1]
			    result: true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			  policy 2: evaluating rules
			    rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : ink)], credentials=[], to=ME]
			    evaluating Request[requester=2, resource=[(resource/type : ink)], credentials=[], from=1]
			      policy 1: evaluating rules
			        rule 1: resource match([(resource/type : ink)], [(resource/type : printer)]) -> false
			    result: false
			  policy 3: evaluating rules
			    rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], credentials=[], to=ME]
			    evaluating Request[requester=3, resource=[(resource/type : printer)], credentials=[], from=1]
			      policy 1: evaluating rules
			        rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			        rule 1: condition true -> true
			        rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			        rule 1: satisfied Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=3]
			    result: true
			result: true
			"""
		);
	}

	@Test
	void orExchange() {
		// Alice gives printer provided the requester gives white paper or yellow paper
		// Bob gives white paper
		// Carl gives yellow paper
		// Ed gives green paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new OrExchange(
							new SingleExchange(
								requester(),
								new Attributes()
									.add("paper/color", "white"),
								new Attributes(),
								me()),
							new SingleExchange(
								requester(),
								new Attributes()
									.add("paper/color", "yellow"),
								new Attributes(),
								me())
						)))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "yellow")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "green")
					))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=OrExchange[left=Exchange[from=REQUESTER, resource=[(paper/color : white)], credentials=[], to=ME], right=Exchange[from=REQUESTER, resource=[(paper/color : yellow)], credentials=[], to=ME]]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(paper/color : white)], condition=true]]
			3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(paper/color : yellow)], condition=true]]
			4 = Policy[party=[(name : Ed), (role : PaperProvider)], rules=[resource=[(paper/color : green)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(paper/color : white)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(paper/color : white)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(paper/color : white)], [(paper/color : white)]) -> true
			        rule 1: condition true -> true
			    result: true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(paper/color : white)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(paper/color : white)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(paper/color : white)], [(paper/color : yellow)]) -> false
			    result: false
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(paper/color : yellow)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(paper/color : yellow)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(paper/color : yellow)], [(paper/color : yellow)]) -> true
			        rule 1: condition true -> true
			    result: true
			result: true
			"""
		);
		assertResultFalse(
			new Request(
				index(4), // Ed
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=4, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(paper/color : white)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(paper/color : white)], credentials=[], from=4]
			      policy 4: evaluating rules
			        rule 1: resource match([(paper/color : white)], [(paper/color : green)]) -> false
			    result: false
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(paper/color : yellow)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(paper/color : yellow)], credentials=[], from=4]
			      policy 4: evaluating rules
			        rule 1: resource match([(paper/color : yellow)], [(paper/color : green)]) -> false
			    result: false
			result: false
			"""
		);
	}

	@Test
	void andExchange() {
		// Alice gives printer provided the requester gives paper and color white
		// Bob gives paper and color white
		// Carl gives paper
		// Ed gives color white
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new AndExchange(
							new SingleExchange(
								requester(),
								new Attributes()
									.add("resource/type", "paper"),
								new Attributes(),
								me()),
							new SingleExchange(
								requester(),
								new Attributes()
									.add("color", "white"),
								new Attributes(),
								me())
						)))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
							.add("color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("color", "white")
					))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=AndExchange[left=Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME], right=Exchange[from=REQUESTER, resource=[(color : white)], credentials=[], to=ME]]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper), (color : white)], condition=true]]
			3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
			4 = Policy[party=[(name : Ed), (role : PaperProvider)], rules=[resource=[(color : white)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(resource/type : paper), (color : white)]) -> true
			        rule 1: condition true -> true
			    result: true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(color : white)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(color : white)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(color : white)], [(resource/type : paper), (color : white)]) -> true
			        rule 1: condition true -> true
			    result: true
			result: true
			"""
		);
		assertResultFalse(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 1: condition true -> true
			    result: true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(color : white)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(color : white)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(color : white)], [(resource/type : paper)]) -> false
			    result: false
			result: false
			"""
		);
		assertResultFalse(
			new Request(
				index(4), // Ed
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=4, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], credentials=[], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], credentials=[], from=4]
			      policy 4: evaluating rules
			        rule 1: resource match([(resource/type : paper)], [(color : white)]) -> false
			    result: false
			result: false
			"""
		);
	}

	@Test
	void exchangeFromAnySuchThat() {
		// Alice gives printer provided any paper provider gives green paper
		// Bob gives white paper
		// Carl gives yellow paper
		// Ed gives green paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							anySuchThat(new Attributes()
									.add("role", "PaperProvider")),
							new Attributes()
								.add("paper/color", "green"),
							new Attributes(),
							me())
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "yellow")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "green")
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[from=anySuchThat: [(role : PaperProvider)], resource=[(paper/color : green)], credentials=[], to=ME]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(paper/color : white)], condition=true]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(paper/color : yellow)], condition=true]]
		4 = Policy[party=[(name : Ed), (role : PaperProvider)], rules=[resource=[(paper/color : green)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], credentials=[], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating rules
			    rule 1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1: condition true -> true
			    rule 1: evaluating Exchange[from=anySuchThat: [(role : PaperProvider)], resource=[(paper/color : green)], credentials=[], to=ME]
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			    policy 4: from match([(role : PaperProvider)], [(name : Ed), (role : PaperProvider)]) -> true
			    evaluating Request[requester=1, resource=[(paper/color : green)], credentials=[], from=2]
			      policy 2: evaluating rules
			        rule 1: resource match([(paper/color : green)], [(paper/color : white)]) -> false
			    result: false
			    evaluating Request[requester=1, resource=[(paper/color : green)], credentials=[], from=3]
			      policy 3: evaluating rules
			        rule 1: resource match([(paper/color : green)], [(paper/color : yellow)]) -> false
			    result: false
			    evaluating Request[requester=1, resource=[(paper/color : green)], credentials=[], from=4]
			      policy 4: evaluating rules
			        rule 1: resource match([(paper/color : green)], [(paper/color : green)]) -> true
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

	private void assertResultFalse(Request request, String expectedTrace) {
		assertAll(
			() -> assertFalse(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}
}
