/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record SingleExchange(Participant from, Attributes resource, Attributes credentials, Participant to)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[from=" + from + ", resource=" + resource + ", credentials=" + credentials + ", to=" + to
				+ "]";
	}

}
