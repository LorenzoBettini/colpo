package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Rule {

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(context -> true, "true");

	private Attributes resource;
	private ExpressionCode condition = TRUE;
	private Exchange exchange;

	public Rule(Attributes resource) {
		this.resource = resource;
	}

	public Rule(Attributes resource, ExpressionCode condition) {
		this.resource = resource;
		this.condition = condition;
	}

	public Rule(Attributes resource, Exchange exchange) {
		this.resource = resource;
		this.exchange = exchange;
	}

	public Rule(Attributes resource, ExpressionCode condition, Exchange exchange) {
		this.resource = resource;
		this.condition = condition;
		this.exchange = exchange;
	}

	public Attributes getResource() {
		return resource;
	}

	public ExpressionCode getCondition() {
		return condition;
	}

	public Exchange getExchange() {
		return exchange;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("resource=");
		stringBuilder.append(resource.toString());
		stringBuilder.append(", condition=");
		stringBuilder.append(condition.toString());
		stringBuilder.append((exchange != null ? ", exchange=" + exchange.toString() : ""));
		return stringBuilder.toString();
	}
}
