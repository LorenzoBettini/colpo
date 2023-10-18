/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record SingleExchange(ExchangeToParticipant to, Attributes resource, Attributes credentials, ExchangeFromParticipant from)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", credentials=" + credentials + ", from=" + from
				+ "]";
	}

}
