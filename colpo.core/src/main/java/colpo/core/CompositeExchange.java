package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public interface CompositeExchange extends Exchange {

	Exchange left();

	Exchange right();
}
