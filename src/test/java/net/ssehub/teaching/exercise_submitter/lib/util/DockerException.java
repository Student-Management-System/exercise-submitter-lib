package net.ssehub.teaching.exercise_submitter.lib.util;


/**
 * Indicates a problem with running docker containers for testing purposes. An unchecked exception for convenience
 * in test code.
 * 
 * @author Adam
 */
public class DockerException extends RuntimeException {

    private static final long serialVersionUID = 697627935516955755L;

    public DockerException() {
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerException(String message) {
        super(message);
    }

    public DockerException(Throwable cause) {
        super(cause);
    }
    
}
