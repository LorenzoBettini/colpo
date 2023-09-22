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
}
