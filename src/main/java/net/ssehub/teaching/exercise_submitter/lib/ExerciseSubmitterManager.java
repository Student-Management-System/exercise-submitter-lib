package net.ssehub.teaching.exercise_submitter.lib;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.replay.Replayer;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.GroupNotFoundException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.IApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.UserNotInCourseException;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;


/**
 * Main class to work with the exercise submitter library. Provides access to all required client functionality.
 * <p>
 * Use {@link ExerciseSubmitterFactory} to create instances.
 * 
 * @author Adam
 * @author Lukas
 */
public class ExerciseSubmitterManager {
    
 
    private String svnBaseUrl; 
    
  
    private Course course;
    

    private IApiConnection mgmtConnection;
    
   
    private Credentials credentials;
    
    private Replayer replayer;
    
    private Assignment currentReplayerAssignment;
    
    
    
    /**
     * The Class Credentials is for save the current username and password for communicating with the Svn server.
     * 
     * TODO: move to data package.
     */
    public static class Credentials {
        
   
        private String username;
        
        
        private char[] password;
        
        /**
         * Instantiates new Credentials which will contain username and password of the current user.
         *
         * @param username the username
         * @param password the password
         */
        public Credentials(String username, char[] password) {
            this.username = username;
            this.password = password;
        }
        
        /**
         * Gets the username.
         *
         * @return the username
         */
        public String getUsername() {
            return this.username;
        }
        
        /**
         * Gets the password.
         *
         * @return the password as char
         */
        public char[] getPassword() {
            return this.password;
        }
    }
    
    /**
     * Creates a new connection to the student management system with the given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @param courseId The ID of the course, e.g. <code>java-wise2021</code>.
     * @param apiConnection The {@link IApiConnection} to use.
     * @param svnBaseUrl The SVN base URL.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws ApiException If a generic API exception occurs.
     */
    ExerciseSubmitterManager(String username, String password, String courseId, IApiConnection apiConnection,
                String svnBaseUrl)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        this.credentials = new Credentials(username, password.toCharArray());
        
        mgmtConnection = apiConnection;
        mgmtConnection.login(username, password);
        course = mgmtConnection.getCourse(courseId);
        this.svnBaseUrl = svnBaseUrl;
    }
    
    /**
     * Returns all assignments (all states).
     *
     * @return A list of all assignments.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException If a generic API exception occurs.
     */
    public List<Assignment> getAllAssignments() throws NetworkException, AuthenticationException, ApiException {
        return mgmtConnection.getAssignments(course);
    }
    
    /**
     * Returns all assignments that can be submitted.
     *
     * @return A list of all assignments that can be submitted.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException If a generic API exception occurs.
     * @see #isSubmittable(Assignment)
     */
    public List<Assignment> getAllSubmittableAssignments()
            throws NetworkException, AuthenticationException, ApiException {
        
        return mgmtConnection.getAssignments(course).stream()
                .filter(this::isSubmittable)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns all assignments that can be replayed.
     *
     * @return A list of all assignments that can be replayed.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException If a generic API exception occurs.
     * @see #isReplayable(Assignment)
     */
    public List<Assignment> getAllReplayableAssignments()
            throws NetworkException, AuthenticationException, ApiException {
        
        return mgmtConnection.getAssignments(course).stream()
                .filter(this::isReplayable)
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a {@link Submitter} for the given assignment.
     *
     * @param assignment The assignment to submit to.
     * @return A {@link Submitter} for the given {@link Assignment}.
     * @throws IllegalArgumentException If the given {@link Assignment} is not submittable.
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws UserNotInCourseException the user not in course exception
     * @throws GroupNotFoundException the group not found exception
     * @throws ApiException If the group name of a group assignment cannot be retrieved.
     * @see #isSubmittable(Assignment)
     */
    public Submitter getSubmitter(Assignment assignment)
            throws IllegalArgumentException, NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException {
        if (!isSubmittable(assignment)) {
            throw new IllegalArgumentException("Assignment " + assignment.getName() + " is not in submittable");
        }
        
        return new Submitter(getSvnUrl(assignment), this.credentials);
    }
    
    /**
     * Creates a {@link Replayer} for the given assignment.
     *
     * @param assignment The assignment to submit to.
     * @return A {@link Replayer} for the given {@link Assignment}.
     * @throws IllegalArgumentException If the given {@link Assignment} is not replayable.
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws UserNotInCourseException the user not in course exception
     * @throws GroupNotFoundException the group not found exception
     * @throws ApiException If the group name of a group assignment cannot be retrieved.
     * @throws IOException 
     * @see #isReplayable(Assignment)
     */
    public Replayer getReplayer(Assignment assignment)
            throws IllegalArgumentException, NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException, IOException {
        if (!isReplayable(assignment)) {
            throw new IllegalArgumentException("Assignment " + assignment.getName() + " is not replayable");
        }
        
        if (replayer  == null) {
       
            this.replayer = new Replayer(getSvnUrl(assignment), this.credentials);
            currentReplayerAssignment = assignment;
            
        } else {
            if (currentReplayerAssignment == null || (!currentReplayerAssignment.equals(assignment))) {
                
                this.replayer = new Replayer(getSvnUrl(assignment), this.credentials);
                currentReplayerAssignment = assignment;
            }
        }
        // TODO: this returns null if init failed and thus causes NullPointerExceptions all over the place
        return replayer;
     
    }
    
    /**
     * Checks if the given {@link Assignment} can be submitted. Note that tutors may be able to submit more assignments
     * than students.
     * 
     * @param assignment The assignment to check.
     * 
     * @return Whether the assignment can be submitted.
     */
    public boolean isSubmittable(Assignment assignment) {
        return assignment.getState() == State.SUBMISSION;
        // TODO: handle tutor rights
    }
    
    /**
     * Checks if the given {@link Assignment} can be replayed. Note that tutors may be able to replay more assignments
     * than students.
     * 
     * @param assignment The assignment to check.
     * 
     * @return Whether the assignment can be replayed.
     */
    public boolean isReplayable(Assignment assignment) {
        return assignment.getState() == State.SUBMISSION || assignment.getState() == State.REVIEWED;
        // TODO: handle tutor rights
    }
    

    /**
     * Creates the SVN URL for the given assignment. Package visiblity for test cases.
     * 
     * @param assignment The assignment.
     * 
     * @return The URL for the SVN location of the submission. Ends with a slash.
     * 
     * @throws ApiException If the group name of a group assignment cannot be retrieved. 
     */
    String getSvnUrl(Assignment assignment) throws ApiException {
        return svnBaseUrl + '/' + assignment.getName() + '/' + getGroupName(assignment) + '/';
    }
    
    /**
     * Return the group name for the given assignment. May be the username if this is not a group work.
     *
     * @param assignment The assignment.
     * @return The group name for the given assignment.
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws UserNotInCourseException the user not in course exception
     * @throws GroupNotFoundException the group not found exception
     * @throws ApiException If the group name of a group assignment cannot be retrieved.
     */
    private String getGroupName(Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException {
        String groupName;
        if (assignment.isGroupWork()) {
            
            groupName = mgmtConnection.getGroupName(course, assignment);
        } else {
            groupName = this.credentials.getUsername();
        }
        return groupName;
    }
    
}
