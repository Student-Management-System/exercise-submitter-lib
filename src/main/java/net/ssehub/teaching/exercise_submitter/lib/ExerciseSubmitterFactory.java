package net.ssehub.teaching.exercise_submitter.lib;

import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.DummyApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.IApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.UserNotInCourseException;

/**
 * Factory for creating instances of {@link ExerciseSubmitterManager}.
 * 
 * @author Adam
 */
public class ExerciseSubmitterFactory {
    
    private String username;
    
    private String password;
    
    private String courseId;
    
    private String authUrl;
    
    private String mgmtUrl;
    
    private String svnurl;
    
    private boolean dummyApiConnection;
    
    /**
     * Sets the username to use. This will be used to log into the student management system and homework submission
     * server.
     * 
     * @param username The username to use.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withUsername(String username) {
        this.username = username;
        return this;
    }
    
    /**
     * Sets the password to use. This will be used to log into the student management system and homework submission
     * server.
     * 
     * @param password The password to use.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withPassword(String password) {
        this.password = password;
        return this;
    }
    
    /**
     * Sets the ID of the course to use. In the form of <code>java-wise2021</code>.
     * 
     * @param courseId The course ID to use.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withCourse(String courseId) {
        this.courseId = courseId;
        return this;
    }
    
    /**
     * Sets the URL of the authentication system to use (sparky-service).
     * 
     * @param url The auth URL to use. No traling slash.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withAuthUrl(String url) {
        this.authUrl = url;
        return this;
    }
    
    /**
     * Sets the URL of the student-management system to use.
     * 
     * @param url The management URL to use. No traling slash.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withMgmtUrl(String url) {
        this.mgmtUrl = url;
        return this;
    }
    
    /**
     * Sets the URL of the SVN system to use.
     * 
     * @param url The management URL to use. No trailing slash.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withSvnUrl(String url) {
        this.svnurl = url;
        return this;
    }
    /**
     * Uses the {@link DummyApiConnection} instead of a real one. Useful only for test cases.
     * 
     * @return This.
     */
    public ExerciseSubmitterFactory withDummyApiConnection() {
        this.dummyApiConnection = true;
        return this;
    }
    
    /**
     * Creates a new {@link ExerciseSubmitterManager} and logs into student management the system.
     * <p>
     * Note that all appropriate setter of this class have to be called first.
     * 
     * @return The {@link ExerciseSubmitterManager}.
     * 
     * @throws UserNotInCourseException  If the given user is not enrolled in the given course.
     * @throws NetworkException If the network communication with the student management system fails.
     * @throws AuthenticationException If authentication fails, e.g. due to invalid credentials.
     * @throws ApiException If any other unexpected exception happens during the API operations.
     */
    public ExerciseSubmitterManager build()
            throws UserNotInCourseException, NetworkException, AuthenticationException, ApiException {
        
        IApiConnection apiConnection;
        if (!dummyApiConnection) {
            apiConnection = new ApiConnection(authUrl, mgmtUrl);
        } else {
            apiConnection = new DummyApiConnection();
        }
        
        return new ExerciseSubmitterManager(username, password, courseId, apiConnection, svnurl);
    }
    

}
