package colpo.core;

public interface MeParticipant extends ToParticipant {

	@Override
	default boolean isMe() {
		return true;
	}

}
