package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record IndexParticipant(int index) implements FromParticipant, ToParticipant {

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "" + index;
	}
}
