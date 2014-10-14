/**
 * 
 */
package mailbox.exception;

/**
 * @author seoi
 */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 7421890469055706293L;

    public NotFoundException(String message) {
        super(message);
    }
}
