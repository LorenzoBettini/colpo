package colpo.core;

public class ParticipantSuchThat implements Participant {

	private Quantifier quantifier;
	private Attributes attributes;

	public enum Quantifier {
		ANY, ALL
	}

	public ParticipantSuchThat(Quantifier quantifier, Attributes attributes) {
		this.quantifier = quantifier;
		this.attributes = attributes;
	}

	public Quantifier getQuantifier() {
		return quantifier;
	}

	public Attributes getAttributes() {
		return attributes;
	}
}
