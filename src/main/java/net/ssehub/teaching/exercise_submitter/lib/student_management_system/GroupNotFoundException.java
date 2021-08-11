package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * Exception thrown if the group for an assignment cannot be found.
 * 
 * @author Adam
 */
public class GroupNotFoundException extends ApiException {

    private static final long serialVersionUID = 1600689121143666681L;

    /**
     * Creates this exception.
     */
    public GroupNotFoundException() {
    }

    /**
     * Creates this exception.
     * 
     * @param message A message describing the exception.
     */
    public GroupNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates this exception.
     * 
     * @param cause An exception that caused this exception.
     */
    public GroupNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates this exception.
     * 
     * @param message A message describing the exception.
     * @param cause An exception that caused this exception.
     */
    public GroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
