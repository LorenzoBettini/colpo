package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Rule {
	private ExpressionCode expression;
	private Exchange exchange;

	public Rule(ExpressionCode expression) {
		this.expression = expression;
	}

	public Rule(ExpressionCode expression, Exchange exchange) {
		this.expression = expression;
		this.exchange = exchange;
	}

	public ExpressionCode getExpression() {
		return expression;
	}

	public Exchange getExchange() {
		return exchange;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("resource=");
		stringBuilder.append(expression.toString());
		stringBuilder.append((exchange != null ? ", exchange=" + exchange.toString() : ""));
		return stringBuilder.toString();
	}
}
