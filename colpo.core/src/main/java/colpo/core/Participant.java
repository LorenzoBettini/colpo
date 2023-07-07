package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Participant {

	private int index = -1;
	private Quantifier quantifier = Quantifier.UNSET;
	private Special special = Special.NOT_SPECIAL;
	private Attributes attributes;

	public enum Quantifier {
		UNSET, ANY, ALL
	}

	public enum Special {
		NOT_SPECIAL, ME, REQUESTER
	}

	private Participant(int index) {
		this.index = index;
	}

	private Participant(Special special) {
		this.special = special;
	}

	private Participant(Quantifier quantifier, Attributes attributes) {
		this.quantifier = quantifier;
		this.attributes = attributes;
	}

	public static Participant index(int index) {
		return new Participant(index);
	}

	public static Participant me() {
		return new Participant(Special.ME);
	}

	public static Participant requester() {
		return new Participant(Special.REQUESTER);
	}

	public static Participant anySuchThat(Attributes attributes) {
		return new Participant(Quantifier.ANY, attributes);
	}

	public static Participant allSuchThat(Attributes attributes) {
		return new Participant(Quantifier.ALL, attributes);
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

	public boolean isMe() {
		return special == Special.ME;
	}

	public boolean isRequester() {
		return special == Special.REQUESTER;
	}

	@Override
	public String toString() {
		if (index > 0)
			return "" + index;
		if (special != Special.NOT_SPECIAL)
			return special.toString();
		return String.format("%s: %s",
			(quantifier == Quantifier.ANY ? "anySuchThat" : "allSuchThat"),
			attributes
		);
	}
}
