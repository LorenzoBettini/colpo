package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Rule {
	private ExpressionCode expression;

	public Rule(ExpressionCode expression) {
		this.expression = expression;
	}
	
	public ExpressionCode getExpression() {
		return expression;
	}
}
