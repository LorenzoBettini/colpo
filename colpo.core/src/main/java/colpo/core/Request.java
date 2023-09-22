/**
 * 
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record Request(Participant requester, Attributes resource, Attributes credentials, Participant from) {

}
