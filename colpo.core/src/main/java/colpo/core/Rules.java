package colpo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lorenzo Bettini
 */
public class Rules {

	private List<Rule> rules = new ArrayList<>();

	public Rules add(Rule rule) {
		rules.add(rule);
		return this;
	}

	public Collection<Rule> all() {
		return rules;
	}
}
