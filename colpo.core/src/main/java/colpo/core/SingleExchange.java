/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class SingleExchange
		implements Exchange {

	private Participant from;
	private Attributes resource;
	private Attributes credentials;
	private Participant to;

	public SingleExchange(Participant to, Attributes resource, Attributes credentials, Participant from) {
		this.from = from;
		this.resource = resource;
		this.credentials = credentials;
		this.to = to;
	}

	
	public Participant from() {
		return from;
	}


	public Attributes resource() {
		return resource;
	}


	public Attributes credentials() {
		return credentials;
	}


	public Participant to() {
		return to;
	}


	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", credentials=" + credentials + ", from=" + from
				+ "]";
	}

}
