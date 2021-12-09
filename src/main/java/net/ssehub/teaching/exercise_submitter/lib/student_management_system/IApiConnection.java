package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.ssehub.teaching.exercise_submitter.lib.data.Assessment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

/**
 * A connection to the API of the student management system.
 * 
 * @author Adam
 */
public interface IApiConnection {

    /**
     * Logs into the system. Must be called before any other method of this connection.
     * 
     * @param username The username.
     * @param password The password.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException If a generic exception occurs.
     */
    public void login(String username, String password) throws NetworkException, AuthenticationException, ApiException;
    
    /**
     * Returns the username of the logged-in user.
     * 
     * @return The username.
     */
    public String getUsername();
    
    /**
     * Returns the authentication token of the logged-in user.
     * 
     * @return The token.
     */
    public String getToken();
    
    /**
     * Gets the given {@link Course}.
     *
     * @param courseId The ID of the course, e.g. <code>java-wise2021</code>.
     * 
     * @return The list of all {@link Course}s.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws ApiException If a generic exception occurs.
     */
    public Course getCourse(String courseId)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException;
    
    /**
     * Gets all {@link Assignment}s for the given {@link Course}.
     * 
     * @param course The {@link Course} to get all assignments of.
     * 
     * @return The list of all {@link Assignment}s.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws ApiException If a generic exception occurs.
     */
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException;
    
    /**
     * Retrieves the group name that the currently logged-in user has for the given assignment.
     * 
     * @param course The course where the assignment is from.
     * @param assignment The assignment to get the group name for. Must be a group work.
     * 
     * @return The name of the group.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws GroupNotFoundException If the given assignment is not a group assignment.
     * @throws ApiException If a generic exception occurs.
     */
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException;
    
    /**
     * Checks if the currently logged-in user is a tutor or lecturer in the given course.
     * 
     * @param course The course to check the role in.
     * 
     * @return Whether the currently logged-in user is a tutor in the given course.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     * @throws ApiException If a generic exception occurs.
     */
    public boolean hasTutorRights(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException;
    
    /**
     * Returns all groups that are registered for the given assignment. If the assignment is not a group work,
     * it returns all participant names (which can be treated as group names). Note that the user must have tutor
     * rights in the course (see {@link #hasTutorRights(Course)}).
     * 
     * @param course The course that contains the assignment.
     * @param assignment The assignment to get all group names for.
     * 
     * @return All group names of that assignment.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist, or the
     *      user is not a tutor in the course.
     * @throws ApiException If a generic exception occurs.
     */
    public Set<String> getAllGroups(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException;
    
    /**
     * Returns the assessment that have been created for the given assignment and group. Note that the user must have
     * tutor rights in the course, see {@link #hasTutorRights(Course)}.
     * 
     * @param course The course where the assignment is from.
     * @param assignment The assignment to get all assessments for.
     * @param groupName The name of the group to get the assessment for. If the assignment is not a group work, use the
     *      username of the participant.
     * 
     * @return The assessment for the group in the given assignment, or {@link Optional#empty()} if no assessment for
     *      the group exist.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course, the course does not exist, or the
     *      user is not a tutor in the course.
     * @throws GroupNotFoundException If the given group (or user) does not exist in the assignment.
     * @throws ApiException If a generic exception occurs.
     */
    public Optional<Assessment> getAssessment(Course course, Assignment assignment, String groupName)
            throws NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException;
    
    /**
     * Updates or creates the given assessment. Note that the user must have tutor rights in the course, see
     * {@link #hasTutorRights(Course)}.
     * 
     * @param course The course where the assignment is in.
     * @param assignment The assignment to upload the assessment for.
     * @param groupName The group name to create the assessment for. If the assignment is not a group work, use the
     *      username of the participant.
     * @param assessment The new assessment to create or update.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course, the course does not exist, or the
     *      user is not a tutor in the course.
     * @throws GroupNotFoundException If the given group (or user) does not exist in the assignment.
     * @throws ApiException If a generic exception occurs.
     */
    public void uploadAssessment(Course course, Assignment assignment, String groupName, Assessment assessment)
            throws NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException;

    
    /**
     * Returns all Courses registered in the stuMgmt System.
     * @return the List of the Courses, empty if no Courses are created,
     * @throws ApiException
     */
    public List<Course> getAllCourses() throws ApiException;
    
}
