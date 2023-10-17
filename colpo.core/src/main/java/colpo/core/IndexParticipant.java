package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class IndexParticipant implements FromParticipant, ToParticipant {

	private int index = -1;

	private static final Attributes EMPTY = new Attributes();

	public IndexParticipant(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public boolean isAll() {
		return false;
	}

	@Override
	public Attributes getAttributes() {
		return EMPTY;
	}

	@Override
	public String toString() {
		return "" + index;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexParticipant other = (IndexParticipant) obj;
		return index == other.index;
	}
}
