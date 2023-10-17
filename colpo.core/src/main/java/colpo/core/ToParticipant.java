package colpo.core;

/**
 * Represents a participant specification usable in a "to" specification
 * 
 * @author Lorenzo Bettini
 */
public interface ToParticipant extends ParticipantInterface {

	default boolean isMe() {
		return false;
	}

}
