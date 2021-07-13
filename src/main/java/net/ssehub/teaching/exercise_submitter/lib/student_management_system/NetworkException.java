package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * Exception thrown by the {@link IApiConnection} if the network communication fails.
 * 
 * @author Adam
 */
public class NetworkException extends ApiException {

    private static final long serialVersionUID = -9052172174230165263L;

    /**
     * Creates an {@link NetworkException}.
     */
    public NetworkException() {
    }
    
    /**
     * Creates an {@link NetworkException}.
     * 
     * @param message A message describing the exception.
     */
    public NetworkException(String message) {
        super(message);
    }
    
    /**
     * Creates an {@link NetworkException}.
     * 
     * @param cause The cause of this exception.
     */
    public NetworkException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates an {@link NetworkException}.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
