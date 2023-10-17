package colpo.core;

/**
 * Represents a participant specification usable in a "from" specification
 * 
 * @author Lorenzo Bettini
 */
public interface FromParticipant extends ParticipantInterface {

	default boolean isRequester() {
		return false;
	}
}
