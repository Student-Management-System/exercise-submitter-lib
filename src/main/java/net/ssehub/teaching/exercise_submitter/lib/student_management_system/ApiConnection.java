package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.ArrayList;
import java.util.List;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentRegistrationApi;
import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.ApiException;
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
    public void login(String username, String password) throws NetworkException, AuthenticationException {
        AuthControllerApi api = new AuthControllerApi(this.authClient);
        CredentialsDto credentials = new CredentialsDto();
        credentials.setUsername(username);
        credentials.setPassword(password);
        try {
            AuthenticationInfoDto authinfo = api.authenticate(credentials);
            this.mgmtClient.setAccessToken(authinfo.getToken().getToken());
        } catch (ApiException e) {
            // TODO: use response body
            throw new AuthenticationException();
        }

        AuthenticationApi mgmtAuth = new AuthenticationApi(mgmtClient);
        try {
            this.loggedInUser = mgmtAuth.whoAmI();
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            // TODO: use response body
            throw new AuthenticationException();
        }
    }

    @Override
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, UserNotInCourseException {
        
        String courseId = name + "-" + semester;
        CourseApi api = new CourseApi(this.mgmtClient);
        
        Course course = null;
        
        try {
            CourseDto courseinfo = api.getCourseById(courseId);
            course = new Course(courseinfo.getTitle(), courseinfo.getId());
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 401) {
                throw new AuthenticationException();
            }
            if (e.getCode() == 403) {
                throw new UserNotInCourseException();
            }
            // TODO: handle generic exception cases
        }
        return course;
    }

    @Override
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, IllegalArgumentException {
        
        AssignmentApi api = new AssignmentApi(mgmtClient);
        List<Assignment> assignments = new ArrayList<Assignment>();
        try {
            
            List<AssignmentDto> assignmentinfo = api.getAssignmentsOfCourse(course.getId());
            assignmentinfo.forEach(oldassignment -> {
                
                Assignment.State state;
                switch (oldassignment.getState()) {
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
                case CLOSED: // TODO: handle this case
                default:
                    state = State.INVISIBLE;
                    break;
                }
                
                boolean groupwork = oldassignment.getCollaboration() != CollaborationEnum.SINGLE ? true : false;
                assignments.add(new Assignment(oldassignment.getId(), oldassignment.getName(), state, groupwork));
                
            });
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 401) {
                throw new AuthenticationException();
            }
            if (e.getCode() == 403) {
                throw new UserNotInCourseException();
            }
            // TODO: handle generic exception cases
        }
        return assignments;
    }

    @Override
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException {
        
        if (this.loggedInUser == null) {
            throw new AuthenticationException("Not logged in");
        }
        
        AssignmentRegistrationApi assignmentRegistrations = new AssignmentRegistrationApi(mgmtClient);
        
        String groupName = "";
        
        try {
            GroupDto group = assignmentRegistrations.getRegisteredGroupOfUser(course.getId(),
                    assignment.getManagementId(), this.loggedInUser.getId());
            
            groupName = group.getName();
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 401) {
                throw new AuthenticationException();
            }
            if (e.getCode() == 403) {
                throw new UserNotInCourseException();
            }
            if (e.getCode() == 404) {
                throw new GroupNotFoundException();
            }
            
            // TODO: handle generic exception cases
        }
        
        return groupName;
    }

}
