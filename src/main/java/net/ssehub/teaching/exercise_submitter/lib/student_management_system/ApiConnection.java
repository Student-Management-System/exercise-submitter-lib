package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentRegistrationApi;
import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

/**
 * Provides communication to the student-management system.
 */
public class ApiConnection implements IApiConnection {
    
    private static final Gson GSON = new Gson();

    private net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient;

    private ApiClient mgmtClient;
    
    private UserDto loggedInUser;

    /**
     * Instantiates a new API connection.
     *
     * @param authUrl The URL to the authentication sytem (sparky-service). Without a trailing slash.
     * @param mgmtUrl the URL to the student management system. Without a trailing slash.
     */
    public ApiConnection(String authUrl, String mgmtUrl) {
        this.authClient = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
        this.authClient.setBasePath(authUrl);

        this.mgmtClient = new ApiClient();
        this.mgmtClient.setBasePath(mgmtUrl);
    }

    @Override
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

    @Override
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        String courseId = name + "-" + semester;
        CourseApi api = new CourseApi(this.mgmtClient);
        
        Course course;
        
        try {
            CourseDto courseinfo = api.getCourseById(courseId);
            course = new Course(courseinfo.getTitle(), courseinfo.getId());
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        
        return course;
    }

    @Override
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

    @Override
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException {
        
        if (this.loggedInUser == null) {
            throw new AuthenticationException("Not logged in");
        }
        
        AssignmentRegistrationApi assignmentRegistrations = new AssignmentRegistrationApi(mgmtClient);
        
        String groupName;
        
        try {
            GroupDto group = assignmentRegistrations.getRegisteredGroupOfUser(course.getId(),
                    assignment.getManagementId(), this.loggedInUser.getId());
            
            groupName = group.getName();
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            if (e.getCode() == 404) {
                throw new GroupNotFoundException(parseResponseMessage(e.getResponseBody()));
            }
            
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        
        return groupName;
    }
    
    /**
     * Converts the given exception from the management API to a proper {@link ApiException}.
     * <p>
     * Handles:
     * <ul>
     *      <li>IOException: {@link NetworkException}</li>
     *      <li>Code 401: {@link AuthenticationException} "Not logged in"</li>
     *      <li>Fallback: {@link ApiException} "Unknown exception"</li>
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
     * Converts the given exception from the authentication API to a proper {@link ApiException}.
     * <p>
     * Handles:
     * <ul>
     *      <li>IOException: {@link NetworkException}</li>
     *      <li>Fallback: {@link ApiException} "Unknown exception"</li>
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
     * Converts the JSON response message object into a simple message string. Uses the <code>message</code> element
     * in the given JSON object. As a fallback (e.g. if JSON is not parseable), returns the whole response body.
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
