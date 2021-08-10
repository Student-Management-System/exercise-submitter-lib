package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * Exception thrown if the student-management system user is not enrolled in a course that it tries to access.
 * 
 * @author Adam
 */
public class UserNotInCourseException extends AuthenticationException {

    private static final long serialVersionUID = 1600689121143666681L;

    /**
     * Creates this exception.
     */
    public UserNotInCourseException() {
    }

    /**
     * Creates this exception.
     * 
     * @param message A message describing the exception.
     */
    public UserNotInCourseException(String message) {
        super(message);
    }

    /**
     * Creates this exception.
     * 
     * @param cause An exception that caused this exception.
     */
    public UserNotInCourseException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates this exception.
     * 
     * @param message A message describing the exception.
     * @param cause An exception that caused this exception.
     */
    public UserNotInCourseException(String message, Throwable cause) {
        super(message, cause);
    }

}
