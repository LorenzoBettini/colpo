package colpo.core;

import java.util.Collection;

/**
 * @author Lorenzo Bettini
 */
public record Policy(Attributes party, Collection<Rule> rules) {

}
