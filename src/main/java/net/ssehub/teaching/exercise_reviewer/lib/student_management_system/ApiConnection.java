package net.ssehub.teaching.exercise_reviewer.lib.student_management_system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssessmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.model.AssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_reviewer.lib.data.Assessment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.UserNotInCourseException;

/**
 * Handles the communication between the plugin and student management.
 *
 * @author lukas
 *
 */
public class ApiConnection {

    private static final Gson GSON = new Gson();

    private net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient;

    private ApiClient mgmtClient;

    private UserDto loggedInUser;

    /**
     * Instantiates a new API connection.
     *
     * @param authUrl The URL to the authentication sytem (sparky-service). Without
     *                a trailing slash.
     * @param mgmtUrl the URL to the student management system. Without a trailing
     *                slash.
     */
    public ApiConnection(String authUrl, String mgmtUrl) {
        this.authClient = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
        this.authClient.setBasePath(authUrl);

        this.mgmtClient = new ApiClient();
        this.mgmtClient.setBasePath(mgmtUrl);
    }

    /**
     * Logs into the system. Must be called before any other method of this
     * connection.
     *
     * @param username The username.
     * @param password The password.
     *
     * @throws NetworkException        If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException            If a generic exception occurs.
     */
    public void login(String username, String password) throws NetworkException, AuthenticationException, ApiException {
        AuthControllerApi api = new AuthControllerApi(this.authClient);

        CredentialsDto credentials = new CredentialsDto();
        credentials.setUsername(username);
        credentials.setPassword(password);

        try {
            AuthenticationInfoDto authinfo = api.authenticate(credentials);
            this.mgmtClient.setAccessToken(authinfo.getToken().getToken());

        } catch (net.ssehub.studentmgmt.sparkyservice_api.ApiException e) {
            if (e.getCode() == 401) {
                throw new AuthenticationException(
                        "Invalid credentials: " + this.parseResponseMessage(e.getResponseBody()));
            }
            throw this.handleAuthException(e);

        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }

        AuthenticationApi mgmtAuth = new AuthenticationApi(this.mgmtClient);
        try {
            this.loggedInUser = mgmtAuth.whoAmI();

        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            throw this.handleMgmtException(e);

        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
    }

    /**
     * Gets all reviewable submission from an assignment.
     *
     * @param courseId
     * @param assignment
     * @return List<Submission>
     * @throws ApiException
     */
    public List<Assessment> getAllAssessmentsFromAssignment(String courseId, Assignment assignment) 
            throws ApiException {
        AssessmentApi api = new AssessmentApi(this.mgmtClient);
        AssessmentDto dto;
        
        List<Assessment> assessments = null;
        try {

            List<AssessmentDto> assessmentDto = api.getAssessmentsForAssignment(courseId, assignment.getManagementId(),
                    null, null, null, null, null, null, null);

            assessments = new ArrayList<Assessment>();

            for (AssessmentDto element : assessmentDto) {
                Assessment assessment = new Assessment(element.getId(), assignment, false);
                assessments.add(assessment);
            }

        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            throw this.handleMgmtException(e);
        }

        return assessments;
    }
    /**
     * Retrieves all assignments created in the stuMgmt.
     * @param course
     * @return List<Assignments>
     * @throws NetworkException
     * @throws AuthenticationException
     * @throws UserNotInCourseException
     * @throws ApiException
     */
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        AssignmentApi api = new AssignmentApi(mgmtClient);
        List<Assignment> assignments;
        try {
            
            assignments = api.getAssignmentsOfCourse(course.getId()).stream()
                    .map((assignment) -> {
                        Assignment.State state;
                        switch (assignment.getState()) {
                        case EVALUATED:
                            state = State.REVIEWED;
                            break;
                        case INVISIBLE:
                            state = State.INVISIBLE;
                            break;
                        case IN_PROGRESS:
                            state = State.SUBMISSION;
                            break;
                        case IN_REVIEW:
                            state = State.IN_REVIEW;
                            break;
                        case CLOSED:
                            state = State.CLOSED;
                            break;
                        default:
                            state = State.INVISIBLE;
                            break;
                        }
                        
                        boolean groupwork = assignment.getCollaboration() != CollaborationEnum.SINGLE ? true : false;
                        
                        return new Assignment(assignment.getId(), assignment.getName(), state, groupwork);
                    })
                    .collect(Collectors.toList());
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        return assignments;
    }


    /**
     * Converts the given exception from the management API to a proper
     * {@link ApiException}.
     * <p>
     * Handles:
     * <ul>
     * <li>IOException: {@link NetworkException}</li>
     * <li>Code 401: {@link AuthenticationException} "Not logged in"</li>
     * <li>Fallback: {@link ApiException} "Unknown exception"</li>
     * </ul>
     *
     * @param exception The management exception.
     *
     * @return An {@link ApiException}.
     */
    private ApiException handleMgmtException(net.ssehub.studentmgmt.backend_api.ApiException exception) {
        ApiException result;

        if (exception.getCause() instanceof IOException) {
            result = new NetworkException(exception.getCause());

        } else if (exception.getCode() == 401) {
            result = new AuthenticationException(
                    "Not logged in: " + this.parseResponseMessage(exception.getResponseBody()));

        } else {
            result = new ApiException("Unknown exception: " + this.parseResponseMessage(exception.getResponseBody()),
                    exception);
        }

        return result;
    }

    /**
     * Converts the given exception from the authentication API to a proper
     * {@link ApiException}.
     * <p>
     * Handles:
     * <ul>
     * <li>IOException: {@link NetworkException}</li>
     * <li>Fallback: {@link ApiException} "Unknown exception"</li>
     * </ul>
     *
     * @param exception The authentication exception.
     *
     * @return An {@link ApiException}.
     */
    private ApiException handleAuthException(net.ssehub.studentmgmt.sparkyservice_api.ApiException exception) {
        ApiException result;

        if (exception.getCause() instanceof IOException) {
            result = new NetworkException(exception.getCause());

        } else {
            result = new ApiException("Unknown exception: " + this.parseResponseMessage(exception.getResponseBody()),
                    exception);
        }

        return result;
    }

    /**
     * Converts the JSON response message object into a simple message string. Uses
     * the <code>message</code> element in the given JSON object. As a fallback
     * (e.g. if JSON is not parseable), returns the whole response body.
     *
     * @param responseBody The response body of a failed API request.
     *
     * @return The message of the JSON result, or the whole response body.
     */
    private String parseResponseMessage(String responseBody) {
        String result = String.valueOf(responseBody);

        if (responseBody != null) {
            try {
                JsonObject obj = GSON.fromJson(responseBody, JsonObject.class);
                if (obj.has("message")) {
                    result = obj.get("message").getAsString();
                }
            } catch (JsonParseException e) {
                // ignore
            }
        }

        return result;
    }
}
