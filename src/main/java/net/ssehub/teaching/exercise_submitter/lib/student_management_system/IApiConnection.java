package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.List;

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
     */
    public boolean hasTutorRights(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException;
        
}
