/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record SingleExchange(ToParticipant to, Attributes resource, Attributes credentials, FromParticipant from)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", credentials=" + credentials + ", from=" + from
				+ "]";
	}

}
