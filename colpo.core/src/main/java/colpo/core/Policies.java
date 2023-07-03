package colpo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lorenzo Bettini
 */
public class Policies {

	private List<Policy> policies = new ArrayList<>();

	public Policies add(Policy policy) {
		policies.add(policy);
		return this;
	}

	public List<Policy> all() {
		return policies;
	}
}
