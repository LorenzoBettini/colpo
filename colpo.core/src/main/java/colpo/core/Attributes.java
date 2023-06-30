/**
 * 
 */
package colpo.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lorenzo Bettini
 */
public class Attributes {

	private Map<String, Object> attributeMap = new HashMap<>();

	public Attributes add(String attributeName, Object attributeValue) {
		Object previous = attributeMap.put(attributeName, attributeValue);
		if (previous != null)
			throw new IllegalArgumentException(
				String.format("'%s' is already present as '%s'", attributeName, previous));
		return this;
	}

	public Object name(String attributeName) {
		return attributeMap.get(attributeName);
	}
}
