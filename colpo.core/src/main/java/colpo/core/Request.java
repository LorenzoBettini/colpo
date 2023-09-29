/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record Request(Participant requester, Attributes resource, Attributes credentials, Participant from) {

	public Request {
		if (requester != null && requester.getIndex() > 0 && requester.getIndex() == from.getIndex())
			throw new IllegalArgumentException("requester and from are the same: " + requester.getIndex());
	}

	/**
	 * Create a copy of this request but replacing the "from" Participant
	 * with the Participant index.
	 * 
	 * @param participantIndex
	 * @return
	 */
	public Request withFrom(int participantIndex) {
		return new Request(requester, resource, credentials,
			Participant.index(participantIndex));
	}
}
