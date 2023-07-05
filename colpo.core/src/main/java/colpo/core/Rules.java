package colpo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lorenzo Bettini
 */
public class Rules {

	private List<Rule> collection = new ArrayList<>();

	public Rules add(Rule rule) {
		collection.add(rule);
		return this;
	}

	public Collection<Rule> all() {
		return collection;
	}
}
