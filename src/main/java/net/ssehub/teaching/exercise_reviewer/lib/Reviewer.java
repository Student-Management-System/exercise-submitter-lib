package net.ssehub.teaching.exercise_reviewer.lib;

import java.util.List;

import net.ssehub.teaching.exercise_reviewer.lib.data.Submission;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager.Credentials;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;

/**
 * ddfas.
 * @author lukas
 *
 */
public class Reviewer {
    private Assignment currentAssignment;
    private Credentials credentials;
    
    private String courseId;
    /**
     * Creates an instance of Reviewer.
     * @param assignment
     * @param credentials
     * @param courseId
     */
    public Reviewer(Assignment assignment, Credentials credentials, String courseId) {
        this.currentAssignment = assignment;
        this.credentials = credentials;
        this.courseId = courseId;
    }
    
    
    
    
    
    
}
