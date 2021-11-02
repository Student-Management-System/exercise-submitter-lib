package net.ssehub.teaching.exercise_reviewer.lib;

import java.util.List;
import java.util.Optional;

import net.ssehub.teaching.exercise_reviewer.lib.data.Assessment;
import net.ssehub.teaching.exercise_reviewer.lib.student_management_system.ApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;

/**
 * This class handles reviewer.
 *
 * @author lukas
 *
 */
public class Reviewer {
    private String username;
    private char[] password;
    private String courseId;

    private String mgmturl;
    private String authurl;
    private Optional<String> submissionurl = Optional.empty();

    private ApiConnection mgmtConnection;

    /**
     * Creates an instance of Reviewer.
     *
     * @param username the username
     * @param password the password
     * @param courseId the course id
     * @param mgmturl the mgmturl
     * @param authurl the authurl
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws ApiException the api exception
     */
    public Reviewer(String username, String password, String courseId,
            String mgmturl, String authurl)
            throws NetworkException, AuthenticationException, ApiException {
        this.username = username;
        this.password = password.toCharArray();
        this.courseId = courseId;
        this.mgmturl = mgmturl;
        this.authurl = authurl;
        this.login();
    }

    /**
     * Login to the mgmt network.
     *
     * @throws NetworkException
     * @throws AuthenticationException
     * @throws ApiException
     */
    private void login() throws NetworkException, AuthenticationException, ApiException {
        ApiConnection api = new ApiConnection(this.authurl, this.mgmturl);
        api.login(this.username, String.valueOf(this.password));
        this.mgmtConnection = api;
    }

    /**
     * adding the submissionserverurl.
     *
     * @param url
     * @return Reviewer
     */
    public Reviewer withExerciseSubmitterServerUrl(String url) {
        this.submissionurl = Optional.ofNullable(url);
        return this;
    }

    /**
     * Gets all assessments from the assignment.
     *
     * @param assignment , submission should be downloaded.
     * @return Reviewer
     * @throws ApiException
     */
    public List<Assessment> getAllSubmissionsFromAssignment(Assignment assignment) throws ApiException {
        return this.mgmtConnection.getAllAssessmentsFromAssignment(this.courseId, assignment);
    }
    /**
     * Gets all assignments from stuMgmt.
     * @return List<Assignment>
     * @throws ApiException
     */
    public List<Assignment> getAllAssignments() throws ApiException {
        return this.mgmtConnection.getAssignments(new Course("java", courseId));    
    }

}
