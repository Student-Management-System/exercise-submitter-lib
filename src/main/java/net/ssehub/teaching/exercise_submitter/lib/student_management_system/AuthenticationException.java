package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * Exception thrown by the {@link IApiConnection} if the authentication fails.
 * 
 * @author Adam
 */
public class AuthenticationException extends ApiException {

    private static final long serialVersionUID = -86576627577019978L;

    /**
     * Creates an {@link AuthenticationException}.
     */
    public AuthenticationException() {
    }
    
    /**
     * Creates an {@link AuthenticationException}.
     * 
     * @param message A message describing the exception.
     */
    public AuthenticationException(String message) {
        super(message);
    }
    
    /**
     * Creates an {@link AuthenticationException}.
     * 
     * @param cause The cause of this exception.
     */
    public AuthenticationException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates an {@link AuthenticationException}.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
