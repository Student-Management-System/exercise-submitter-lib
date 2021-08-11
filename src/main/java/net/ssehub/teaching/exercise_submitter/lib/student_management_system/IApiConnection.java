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
     */
    public void login(String username, String password) throws NetworkException, AuthenticationException;
    
    /**
     * Gets the given {@link Course}.
     *
     * @param name The name of the course, e.g. <code>java</code>.
     * @param semester The semester of the course, e.g. <code>wise2021</code>.
     * 
     * @return The list of all {@link Course}s.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws UserNotInCourseException If the user is not enrolled in the course or the course does not exist.
     */
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, UserNotInCourseException;
    
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
     */
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException;
    
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
     */
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException;
        
}
