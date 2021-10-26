package net.ssehub.teaching.exercise_submitter.lib.submission;


/**
 * Represents an exception during a submission to the server.
 * 
 * @author Adam
 */
public class SubmissionException extends Exception {

    private static final long serialVersionUID = 4167684171216685580L;

    /**
     * Creates a new exception.
     */
    public SubmissionException() {
    }

    /**
     * Creates a new exception.
     * 
     * @param message A message describing the exception.
     */
    public SubmissionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception.
     * 
     * @param cause The cause of this exception.
     */
    public SubmissionException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates a new exception.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public SubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
