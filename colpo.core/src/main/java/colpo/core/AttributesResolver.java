/**
 * 
 */
package colpo.core;

import colpo.core.semantics.UndefinedName;

/**
 * @author Lorenzo Bettini
 */
public interface AttributesResolver {

	Object name(String name) throws UndefinedName;
}
