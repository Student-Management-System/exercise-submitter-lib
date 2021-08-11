package net.ssehub.teaching.exercise_submitter.lib;

import java.util.List;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.replay.Replayer;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.DummyApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.IApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.UserNotInCourseException;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;

/**
 * Main class to work with the exercise submitter library. Provides access to all required client functionality.
 * 
 * @author Adam
 */
public class ExerciseSubmitterManager {
    
    private String svnBaseUrl = "http://127.0.0.1/java/abgabe"; // TODO: read from config
    
    private Course course;
    
    private IApiConnection mgmtConnection;
    
    private String username;
    
    /**
     * Creates a new connection to the student management system with the given username and password.
     * 
     * @param username The username.
     * @param password The password.
     * @param courseName The name of the course, e.g. <code>java</code>.
     * @param courseSemester The semester of the course, e.g. <code>wise20210</code>.
     * 
     * @throws AuthenticationException If the authentication fails.
     * @throws NetworkException If the network communication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     */
    public ExerciseSubmitterManager(String username, String password, String courseName, String courseSemester)
            throws NetworkException, AuthenticationException, UserNotInCourseException {
        this.username = username;
        
        mgmtConnection = new DummyApiConnection(); // TODO: factory
        mgmtConnection.login(username, password);
        course = mgmtConnection.getCourse(courseName, courseSemester);
    }
    
    /**
     * Returns all assignments (all states).
     * 
     * @return A list of all assignments.
     * 
     * @throws AuthenticationException If the authentication fails.
     * @throws NetworkException If the network communication fails.
     */
    public List<Assignment> getAllAssignments() throws NetworkException, AuthenticationException {
        return mgmtConnection.getAssignments(course);
    }
    
    /**
     * Returns all assignments that can be submitted.
     * 
     * @return A list of all assignments that can be submitted.
     * 
     * @throws AuthenticationException If the authentication fails.
     * @throws NetworkException If the network communication fails.
     * 
     * @see #isSubmittable(Assignment)
     */
    public List<Assignment> getAllSubmittableAssignments() throws NetworkException, AuthenticationException {
        return mgmtConnection.getAssignments(course).stream()
                .filter(this::isSubmittable)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns all assignments that can be replayed.
     * 
     * @return A list of all assignments that can be replayed.
     * 
     * @throws AuthenticationException If the authentication fails.
     * @throws NetworkException If the network communication fails.
     * 
     * @see #isReplayable(Assignment)
     */
    public List<Assignment> getAllReplayableAssignments() throws NetworkException, AuthenticationException {
        return mgmtConnection.getAssignments(course).stream()
                .filter(this::isReplayable)
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a {@link Submitter} for the given assignment.
     * 
     * @param assignment The assignment to submit to.
     * 
     * @return A {@link Submitter} for the given {@link Assignment}.
     * 
     * @throws IllegalArgumentException If the given {@link Assignment} is not submittable.
     * 
     * @throws ApiException If the group name of a group assignment cannot be retrieved. 
     * 
     * @see #isSubmittable(Assignment)
     */
    public Submitter getSubmitter(Assignment assignment) throws IllegalArgumentException, ApiException {
        if (!isSubmittable(assignment)) {
            throw new IllegalArgumentException("Assignment " + assignment.getName() + " is not in submittable");
        }
        
        return new Submitter(getSvnUrl(assignment));
    }
    
    /**
     * Creates a {@link Replayer} for the given assignment.
     * 
     * @param assignment The assignment to submit to.
     * 
     * @return A {@link Replayer} for the given {@link Assignment}.
     * 
     * @throws IllegalArgumentException If the given {@link Assignment} is not replayable.
     * 
     * @throws ApiException If the group name of a group assignment cannot be retrieved. 
     * 
     * @see #isReplayable(Assignment)
     */
    public Replayer getReplayer(Assignment assignment) throws IllegalArgumentException, ApiException {
        if (!isReplayable(assignment)) {
            throw new IllegalArgumentException("Assignment " + assignment.getName() + " is not replayable");
        }
        
        return new Replayer(getSvnUrl(assignment));
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
     * 
     * @return The group name for the given assignment.
     * 
     * @throws ApiException If the group name of a group assignment cannot be retrieved. 
     */
    private String getGroupName(Assignment assignment) throws ApiException {
        String groupName;
        if (assignment.isGroupWork()) {
            groupName = mgmtConnection.getGroupName(course, assignment);
        } else {
            groupName = username;
        }
        return groupName;
    }
    
}
