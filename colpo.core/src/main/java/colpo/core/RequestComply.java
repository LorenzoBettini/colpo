package colpo.core;

/**
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface RequestComply {
	boolean test(Request newRequest, Request existingRequest); // NOSONAR
}
