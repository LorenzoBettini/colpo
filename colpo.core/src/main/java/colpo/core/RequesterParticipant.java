package colpo.core;

public interface RequesterParticipant extends FromParticipant {

	@Override
	default boolean isRequester() {
		return true;
	}
}
