package net.ssehub.teaching.exercise_submitter.lib;

import java.util.List;
import java.util.Optional;
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
    
    private Course course;
    
    private IApiConnection mgmtConnection;
    
    private String exerciseSubmitterServerUrl;
    
    private Optional<Replayer> cachedReplayer;
    
    private Optional<Assignment> cachedReplayerAssignment;
    
    /**
     * Creates a new connection to the student management system with the given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @param courseId The ID of the course, e.g. <code>java-wise2021</code>.
     * @param apiConnection The {@link IApiConnection} to use.
     * @param exerciseSubmitterServerUrl The URL to the API of the exercise-submitter-server.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws ApiException If a generic API exception occurs.
     */
    ExerciseSubmitterManager(String username, String password, String courseId, IApiConnection apiConnection,
                String exerciseSubmitterServerUrl)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        this.mgmtConnection = apiConnection;
        this.mgmtConnection.login(username, password);
        this.course = mgmtConnection.getCourse(courseId);
        this.exerciseSubmitterServerUrl = exerciseSubmitterServerUrl;
        
        this.cachedReplayer = Optional.empty();
        this.cachedReplayerAssignment = Optional.empty();
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
        
        return new Submitter(exerciseSubmitterServerUrl, course.getId(), assignment.getName(),
                getGroupName(assignment), mgmtConnection.getToken());
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
     * @see #isReplayable(Assignment)
     */
    public Replayer getReplayer(Assignment assignment)
            throws IllegalArgumentException, NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException {
        if (!isReplayable(assignment)) {
            throw new IllegalArgumentException("Assignment " + assignment.getName() + " is not replayable");
        }
        
        Replayer result;
        
        if (cachedReplayer.isPresent() && cachedReplayerAssignment.map(a -> a.equals(assignment)).orElse(false)) {
            result = cachedReplayer.get();
            
        } else {
            result = new Replayer(exerciseSubmitterServerUrl, course.getId(), assignment.getName(),
                    getGroupName(assignment), mgmtConnection.getToken());
            
            cachedReplayer = Optional.of(result);
            cachedReplayerAssignment = Optional.of(assignment);
        }
        
        return result;
     
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
     * Return the group name for the given assignment. May be the username if this is not a group work.
     * <p>
     * Package visibility for test cases.
     *
     * @param assignment The assignment.
     * @return The group name for the given assignment.
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws UserNotInCourseException the user not in course exception
     * @throws GroupNotFoundException the group not found exception
     * @throws ApiException If the group name of a group assignment cannot be retrieved.
     */
    String getGroupName(Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException {
        String groupName;
        if (assignment.isGroupWork()) {
            
            groupName = mgmtConnection.getGroupName(course, assignment);
        } else {
            groupName = mgmtConnection.getUsername();
        }
        return groupName;
    }
    
}
