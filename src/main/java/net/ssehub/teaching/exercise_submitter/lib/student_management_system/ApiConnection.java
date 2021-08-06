package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.sparkyservice_api.ApiException;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

/**
 * This class provides the interface between the {@link net.ssehub.studentmgmt.sparkyservice_api.ApiClient}
 * and {@link ApiClient} and the library.
 */
public class ApiConnection implements IApiConnection {

    
    private net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient;

    
    private ApiClient mgmtClient;

    /**
     * Instantiates a new api connection.
     *
     * @param authUrl the auth url
     * @param mgmtUrl the mgmt url
     */
    public ApiConnection(String authUrl, String mgmtUrl) {
        this.authClient = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
        this.authClient.setBasePath(authUrl);

        this.mgmtClient = new ApiClient();
        this.mgmtClient.setBasePath(mgmtUrl);
    }

    /**
     * Login a User.
     *
     * @param username the username
     * @param password the password
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     */
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

    }

    /**
     * Gets the course back if the user is enrolled in it.
     *
     * @param name the name
     * @param semester the semester
     * @return the course
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws NoSuchElementException the no such element exception
     */
    @Override
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, NoSuchElementException {
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
                // code: 401 auth error
                // code 403 Student not enrolled in course
                throw new NoSuchElementException();
            }
        }
        return course;
    }

    /**
     * Gets the assignments from a specific course.
     *
     * @param course the course
     * @return the assignments
     * @throws NetworkException the network exception
     * @throws AuthenticationException the authentication exception
     * @throws IllegalArgumentException the illegal argument exception
     */
    @Override
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, IllegalArgumentException {
        
        AssignmentApi api = new AssignmentApi(mgmtClient);
        List<Assignment> assignments = new ArrayList<Assignment>();
        try {
            
            List<AssignmentDto> assignmentinfo = api.getAssignmentsOfCourse(course.getId());
            assignmentinfo.forEach(oldassignment -> {
                
                Assignment.State state = Assignment.State.valueOf("SUBMISSION"); //TODO: IN_PROGRESS OR SUBMISSION
                boolean groupwork = oldassignment.getCollaboration() != CollaborationEnum.SINGLE ? true : false;
                assignments.add(new Assignment(oldassignment.getName(), state, groupwork));
                
            });
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            
            if (e.getCode() == 401) {
                throw new AuthenticationException();
            }
            if (e.getCode() == 403) {
                // code: 401 auth error
                // code 403 Student not enrolled in course
                throw new NoSuchElementException();
            }
        }
        return assignments;
    }

}
