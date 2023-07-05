package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Participant {

	private int index = -1;
	private Quantifier quantifier;
	private Attributes attributes;

	public enum Quantifier {
		ANY, ALL
	}

	public Participant(int index) {
		this.index = index;
	}

	public Participant(Quantifier quantifier, Attributes attributes) {
		this.quantifier = quantifier;
		this.attributes = attributes;
	}

	public int getIndex() {
		return index;
	}

	public Quantifier getQuantifier() {
		return quantifier;
	}

	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		if (index > 0)
			return "" + index;
		return String.format("%s: %s",
			(quantifier == Quantifier.ANY ? "anySuchThat" : "allSuchThat"),
			attributes
		);
	}
}
