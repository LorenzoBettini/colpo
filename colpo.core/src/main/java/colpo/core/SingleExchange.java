/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record SingleExchange(Participant to, Attributes resource, Attributes credentials, Participant from)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", credentials=" + credentials + ", from=" + from
				+ "]";
	}

}
