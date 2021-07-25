package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.List;
import java.util.NoSuchElementException;

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
     * @throws NoSuchElementException If the given course does not exist.
     */
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, NoSuchElementException;
    
    /**
     * Gets all {@link Assignment}s for the given {@link Course}.
     * 
     * @param course The {@link Course} to get all assignments of.
     * 
     * @return The list of all {@link Assignment}s.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws IllegalArgumentException If the {@link Course} does not exist.
     */
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, IllegalArgumentException;
        
}
