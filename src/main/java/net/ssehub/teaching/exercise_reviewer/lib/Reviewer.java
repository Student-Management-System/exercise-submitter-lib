package net.ssehub.teaching.exercise_reviewer.lib;

import java.util.List;
import java.util.Optional;

import net.ssehub.teaching.exercise_reviewer.lib.data.Submission;
import net.ssehub.teaching.exercise_reviewer.lib.student_management_system.ApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
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
    private Assignment currentAssignment;
    private String username;
    private char[] password;
    private String courseId;

    private Optional<String> mgmturl = Optional.empty();
    private Optional<String> authurl = Optional.empty();
    private Optional<String> submissionurl = Optional.empty();

    private ApiConnection mgmtConnection;

    /**
     * Creates an instance of Reviewer.
     *
     * @param assignment
     * @param username
     * @param password
     * @param courseId
     * @throws ApiException
     * @throws AuthenticationException
     * @throws NetworkException
     */
    public Reviewer(Assignment assignment, String username, String password, String courseId)
            throws NetworkException, AuthenticationException, ApiException {
        this.currentAssignment = assignment;
        this.username = username;
        this.password = password.toCharArray();
        this.courseId = courseId;
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
        ApiConnection api = new ApiConnection(this.authurl.get(), this.mgmturl.get());
        api.login(this.username, this.password.toString());
        this.mgmtConnection = api;
    }

    /**
     * adding the mgmturl.
     *
     * @param url
     * @return Reviewer
     */
    public Reviewer withMgmtUrl(String url) {
        this.mgmturl = Optional.ofNullable(url);
        return this;
    }

    /**
     * adding the authurl.
     *
     * @param url
     * @return Reviewer
     */
    public Reviewer withAuthUrl(String url) {
        this.authurl = Optional.ofNullable(url);
        return this;
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
     * Gets all submission from the assignment.
     *
     * @param assignment , submission should be downloaded.
     * @return Reviewer
     * @throws ApiException
     */
    public List<Submission> getAllSubmissionsFromAssignment(Assignment assignment) throws ApiException {
        return this.mgmtConnection.getAllSubmissionFromAssignment(this.courseId, assignment);
    }

}
