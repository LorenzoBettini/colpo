package colpo.core;

/**
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface ExpressionCode {
	boolean evaluate() throws Exception; // NOSONAR
}
