package net.ssehub.teaching.exercise_submitter.lib.replay;

/**
 * Represents an exception during a replay of a submission.
 * 
 * @author Lukas
 *
 */
public class ReplayException extends Exception {

    
    private static final long serialVersionUID = -6984269557211018607L;

    /**
     * Creates a new exception.
     */
    public ReplayException() {
    }

    /**
     * Creates a new exception.
     * 
     * @param message A message describing the exception.
     */
    public ReplayException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception.
     * 
     * @param cause The cause of this exception.
     */
    public ReplayException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates a new exception.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public ReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
