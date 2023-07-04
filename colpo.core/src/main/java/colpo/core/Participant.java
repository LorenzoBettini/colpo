package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public interface Participant {

	<T> T accept(ParticipantVisitor<T> visitor);
}
