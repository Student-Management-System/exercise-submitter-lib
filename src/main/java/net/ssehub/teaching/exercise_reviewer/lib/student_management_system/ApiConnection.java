package net.ssehub.teaching.exercise_reviewer.lib.student_management_system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.api.SubmissionApi;
import net.ssehub.studentmgmt.backend_api.model.SubmissionDto;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_reviewer.lib.data.Submission;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;

/**
 * Handles the communication between the plugin and student management.
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
     * Logs into the system. Must be called before any other method of this connection.
     * 
     * @param username The username.
     * @param password The password.
     * 
     * @throws NetworkException If the network communication fails.
     * @throws AuthenticationException If the authentication fails.
     * @throws ApiException If a generic exception occurs.
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
                throw new AuthenticationException("Invalid credentials: " + parseResponseMessage(e.getResponseBody()));
            }
            throw handleAuthException(e);

        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }

        AuthenticationApi mgmtAuth = new AuthenticationApi(mgmtClient);
        try {
            this.loggedInUser = mgmtAuth.whoAmI();

        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            throw handleMgmtException(e);

        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
    }
    /**
     * Gets all reviewable submission from an assignment.
     * @param courseId
     * @param assignment
     * @return List<Submission>
     * @throws ApiException
     */
    public List<Submission> getAllSubmissionFromAssignment(String courseId, Assignment assignment) throws ApiException {
        SubmissionApi api = new SubmissionApi(this.mgmtClient);
        List<Submission> submissions = null;
        try {
            
            List<SubmissionDto> submissionsDto = api.getAllSubmissions(courseId, null, null, null,
                    assignment.getManagementId(), null, null, null);
            
            submissions = new ArrayList<Submission>();
            
            
            for (SubmissionDto element : submissionsDto) {
                Submission submission = new Submission(element.getAssignmentId(), element.getUserId(),
                        element.getDisplayName(), element.getDate().toString())
                        .withGroupId(element.getGroupId())
                        .withGroupName(element.getGroupName());
                
                submissions.add(submission);
            }
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            throw handleMgmtException(e);
        }
     
        return submissions;
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
            result = new AuthenticationException("Not logged in: " + parseResponseMessage(exception.getResponseBody()));

        } else {
            result = new ApiException("Unknown exception: " + parseResponseMessage(exception.getResponseBody()),
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
            result = new ApiException("Unknown exception: " + parseResponseMessage(exception.getResponseBody()),
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
