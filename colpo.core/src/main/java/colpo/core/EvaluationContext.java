/**
 * 
 */
package colpo.core;

import colpo.core.semantics.UndefinedName;

/**
 * @author Lorenzo Bettini
 */
public interface EvaluationContext {

	Object name(String name) throws UndefinedName;
}
