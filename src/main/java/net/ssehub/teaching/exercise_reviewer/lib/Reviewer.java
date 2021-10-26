package net.ssehub.teaching.exercise_reviewer.lib;

import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;

/**
 * This class handles reviewer.
 * @author lukas
 *
 */
public class Reviewer {
    private Assignment currentAssignment;
    private String username;
    private String password;
    private String courseId;
    /**
     * Creates an instance of Reviewer.
     * @param assignment
     * @param username
     * @param password
     * @param courseId
     */
    public Reviewer(Assignment assignment, String username, String password, String courseId) {
        this.currentAssignment = assignment;
        this.username = username;
        this.password = password;
        this.courseId = courseId;
    }
    
    
    
    
    
    
}
