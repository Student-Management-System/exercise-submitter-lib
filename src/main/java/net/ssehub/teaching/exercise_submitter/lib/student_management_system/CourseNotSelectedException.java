package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

/**
 * If no Course selected in the ExerciseManager.
 * 
 * @author lukas
 *
 */
public class CourseNotSelectedException extends ApiException {

    
    private static final long serialVersionUID = 1535372910448863344L;
    
    /**
     * Creates an {@link CourseNotSelectedException}.
     */
    public CourseNotSelectedException() {
    }
    
    /**
     * Creates an {@link CourseNotSelectedException}.
     * 
     * @param message A message describing the exception.
     */
    public CourseNotSelectedException(String message) {
        super(message);
    }
    
    /**
     * Creates an {@link CourseNotSelectedException}.
     * 
     * @param cause The cause of this exception.
     */
    public CourseNotSelectedException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates an {@link CourseNotSelectedException}.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public CourseNotSelectedException(String message, Throwable cause) {
        super(message, cause);
    }

}
