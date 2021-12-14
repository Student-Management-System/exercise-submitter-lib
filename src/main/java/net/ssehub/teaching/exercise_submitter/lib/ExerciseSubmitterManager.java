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
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.CourseNotSelectedException;
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
    
    private Optional<Course> course = Optional.empty();
    
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
        if (courseId != null) {
            this.course = Optional.ofNullable(this.mgmtConnection.getCourse(courseId));
            
        }
        this.exerciseSubmitterServerUrl = exerciseSubmitterServerUrl;
        
        this.cachedReplayer = Optional.empty();
        this.cachedReplayerAssignment = Optional.empty();
    }
    
    /**
     * Returns the API connection to the student management system.
     * 
     * @return The {@link IApiConnection} for the student management system.
     */
    public IApiConnection getStudentManagementConnection() {
        return mgmtConnection;
    }
    
    /**
     * Returns the {@link Course} that this manager is connected to.
     * 
     * @return The course of this manager.
     */
    public Course getCourse() {
        Course returnCourse;
        if (course.isPresent()) {
            returnCourse = course.get();
        } else {
            returnCourse = new Course("Not Selected", "");
        }
        return returnCourse;
    }
    
    /**
     * Returns all assignments (all states).
     *
     * @return A list of all assignments.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course.
     * @throws ApiException If a generic API exception occurs.
     */
    public List<Assignment> getAllAssignments()
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        return mgmtConnection.getAssignments(course.orElseThrow(CourseNotSelectedException::new));
    }
    
    /**
     * Returns all assignments that can be submitted.
     *
     * @return A list of all assignments that can be submitted.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course.
     * @throws ApiException If a generic API exception occurs.
     * @see #isSubmittable(Assignment)
     */
    public List<Assignment> getAllSubmittableAssignments()
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        return mgmtConnection.getAssignments(course.orElseThrow(CourseNotSelectedException::new)).stream()
                .filter(this::isSubmittable)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns all assignments that can be replayed.
     *
     * @return A list of all assignments that can be replayed.
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course.
     * @throws ApiException If a generic API exception occurs.
     * @see #isReplayable(Assignment)
     */
    public List<Assignment> getAllReplayableAssignments()
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        return mgmtConnection.getAssignments(course.orElseThrow(CourseNotSelectedException::new)).stream()
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
        
        return new Submitter(exerciseSubmitterServerUrl, 
                course.orElseThrow(CourseNotSelectedException::new).getId(), assignment.getName(),
                getGroupName(assignment), mgmtConnection.getToken());
    }
    
    /**
     * Creates a {@link Replayer} for the given assignment. The group of the currently logged-in user is used.
     *
     * @param assignment The assignment to replay.
     * 
     * @return A {@link Replayer} for the given {@link Assignment}.
     * 
     * @throws IllegalArgumentException If the given {@link Assignment} is not replayable.
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws UserNotInCourseException the user not in course exception
     * @throws GroupNotFoundException the group not found exception
     * @throws ApiException If the group name of a group assignment cannot be retrieved.
     * 
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
            result = new Replayer(exerciseSubmitterServerUrl,
                    course.orElseThrow(CourseNotSelectedException::new).getId(), assignment.getName(),
                    getGroupName(assignment), mgmtConnection.getToken());
            
            cachedReplayer = Optional.of(result);
            cachedReplayerAssignment = Optional.of(assignment);
        }
        
        return result;
    }
    
    /**
     * Creates a {@link Replayer} for the given assignment and group name. This method should be used by tutors instead
     * of {@link #getReplayer(Assignment)}, as tutors can have access to arbitrary groups.
     * 
     * @param assignment The assignment to replay.
     * @param groupName The name of the group in the assignment to replay.
     * 
     * @return A {@link Replayer} for the given {@link Assignment} and group.
     * @throws CourseNotSelectedException 
     */
    public Replayer getReplayer(Assignment assignment, String groupName) throws CourseNotSelectedException {
        return new Replayer(exerciseSubmitterServerUrl, 
                course.orElseThrow(CourseNotSelectedException::new).getId(), assignment.getName(), groupName,
                mgmtConnection.getToken());
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
            
            groupName = mgmtConnection.getGroupName(course.orElseThrow(CourseNotSelectedException::new), assignment);
        } else {
            groupName = mgmtConnection.getUsername();
        }
        return groupName;
    }
    /**
     * Sets the course current course.
     * @param course
     */
    public void setCourse(Course course) {
        this.course = Optional.ofNullable(course);
    }
    
}
