package colpo.core;

public interface ParticipantInterface {

	default int getIndex() {
		return -1;
	}

	default boolean isAll() {
		return false;
	}

	Attributes getAttributes();
}
