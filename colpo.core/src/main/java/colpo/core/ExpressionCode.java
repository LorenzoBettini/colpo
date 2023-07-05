package colpo.core;

/**
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface ExpressionCode {
	boolean evaluate(EvaluationContext context) throws Exception; // NOSONAR
}
