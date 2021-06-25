package net.ssehub.teaching.exercise_submitter.lib;


public class SubmissionException extends Exception {

    private static final long serialVersionUID = 4167684171216685580L;

    public SubmissionException() {
    }
    
    public SubmissionException(String message) {
        super(message);
    }
    
    public SubmissionException(Throwable cause) {
        super(cause);
    }
    
    public SubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
