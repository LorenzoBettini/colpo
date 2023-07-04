/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record ParticipantIndex(int index) implements Participant {

	@Override
	public <T> T accept(ParticipantVisitor<T> visitor) {
		return visitor.visit(this);
	}

}
