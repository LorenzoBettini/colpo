package colpo.core;

import colpo.core.QuantifiedParticipant.Quantifier;

/**
 * @author Lorenzo Bettini
 */
public class Participants {

	private static final Attributes EMPTY = new Attributes();

	public static IndexParticipant index(int index) {
		return new IndexParticipant(index);
	}

	public static MeParticipant me() {
		return new MeParticipant() {
			@Override
			public Attributes getAttributes() {
				return EMPTY;
			}

			@Override
			public String toString() {
				return "ME";
			}
		};
	}

	public static RequesterParticipant requester() {
		return new RequesterParticipant() {
			@Override
			public Attributes getAttributes() {
				return EMPTY;
			}

			@Override
			public String toString() {
				return "REQUESTER";
			}
		};
	}

	public static QuantifiedParticipant anySuchThat(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ANY, attributes);
	}

	public static QuantifiedParticipant allSuchThat(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ALL, attributes);
	}

}
