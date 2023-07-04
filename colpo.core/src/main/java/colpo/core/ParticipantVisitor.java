package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public interface ParticipantVisitor<T> {

	T visit(ParticipantIndex participantIndex);

	T visit(ParticipantSuchThat participantSuchThat);

}
