package colpo.core;

public class ExpressionWithDescription implements ExpressionCode {

	private ExpressionCode expressionCode;
	private String description;

	public ExpressionWithDescription(ExpressionCode expressionCode, String description) {
		this.expressionCode = expressionCode;
		this.description = description;
	}

	@Override
	public boolean evaluate() throws Exception {
		return expressionCode.evaluate();
	}

	@Override
	public String toString() {
		return description;
	}
}
