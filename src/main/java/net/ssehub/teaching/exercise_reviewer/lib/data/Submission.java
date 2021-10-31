package net.ssehub.teaching.exercise_reviewer.lib.data;

import java.util.List;
import java.util.Optional;

import net.ssehub.teaching.exercise_submitter.lib.submission.Problem;


/**
 * This class is a submission.
 * @author lukas
 *
 */
public class Submission {
    
    
    private String assignmentId;
    
 
    private String userId;
    
 
    private String userDisplayName;
    
   
    private String date;
    
  
    private Optional<String> groupId = Optional.empty();
    
   
    private Optional<String> groupName = Optional.empty();
    
    
    private Optional<List<Problem>> problems = Optional.empty();
    
    
    /**
     * Creates an instance of Submission.
     *
     * @param assignmentId the assignment id
     * @param userId the user id
     * @param displayname the displayname
     * @param date the date
     */
    public Submission(String assignmentId, String userId, String displayname, String date) {
        this.assignmentId = assignmentId;
        this.userId = userId;
        this.userDisplayName = displayname;
        this.date = date;
    }
    
    /**
     * Sets the groupId.
     *
     * @param groupId the group id
     * @return Submission
     */
    public Submission withGroupId(String groupId) {
        this.groupId = Optional.ofNullable(groupId);
        return this;
    }
    
    /**
     * Sets the groupName.
     *
     * @param groupName the group name
     * @return Submission
     */
    public Submission withGroupName(String groupName) {
        this.groupName = Optional.ofNullable(groupName);
        return this;
    }
    /**
     * Sets the problems if existing.
     *
     * @param problems the problems
     * @return Submission
     */
    public Submission withProblems(List<Problem> problems) {
        this.problems = Optional.ofNullable(problems);
        return this;
    }
    
    /**
     * Gets the assignment id.
     *
     * @return the assignment id
     */
    public String getAssignmentId() {
        return assignmentId;
    }
    
    /**
     * Gets the user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the user display name.
     *
     * @return the user display name
     */
    public String getUserDisplayName() {
        return userDisplayName;
    }
    
    /**
     * Gets the date.
     *
     * @return the date
     */
    public String getDate() {
        return date;
    }
    
    /**
     * Gets the group id.
     *
     * @return the group id
     */
    public Optional<String> getGroupId() {
        return groupId;
    }
    
    /**
     * Gets the group name.
     *
     * @return the group name
     */
    public Optional<String> getGroupName() {
        return groupName;
    }
    
    
    

}
