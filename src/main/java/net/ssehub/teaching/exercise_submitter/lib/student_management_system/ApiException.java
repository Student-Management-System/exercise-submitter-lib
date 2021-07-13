package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * Exception thrown by the {@link IApiConnection}.
 * 
 * @author Adam
 */
public class ApiException extends Exception {

    private static final long serialVersionUID = -447443287500597222L;

    /**
     * Creates an {@link ApiException}.
     */
    public ApiException() {
    }
    
    /**
     * Creates an {@link ApiException}.
     * 
     * @param message A message describing the exception.
     */
    public ApiException(String message) {
        super(message);
    }
    
    /**
     * Creates an {@link ApiException}.
     * 
     * @param cause The cause of this exception.
     */
    public ApiException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates an {@link ApiException}.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
