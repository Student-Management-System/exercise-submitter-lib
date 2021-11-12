package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssessmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentRegistrationApi;
import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.api.CourseParticipantsApi;
import net.ssehub.studentmgmt.backend_api.model.AssessmentCreateDto;
import net.ssehub.studentmgmt.backend_api.model.AssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssessmentUpdateDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto.SeverityEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_submitter.lib.data.Assessment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;

/**
 * Provides communication to the student-management system.
 */
public class ApiConnection implements IApiConnection {
    
    private static final Gson GSON = new Gson();

    private net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient;

    private ApiClient mgmtClient;
    
    private UserDto loggedInUser;
    
    private String token;

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
            this.token = authinfo.getToken().getToken();
            this.mgmtClient.setAccessToken(this.token);
            
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
    public String getUsername() {
        return loggedInUser.getUsername();
    }
    
    @Override
    public String getToken() {
        return token;
    }

    @Override
    public Course getCourse(String courseId)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
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
    

    @Override
    public boolean hasTutorRights(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        if (this.loggedInUser == null) {
            throw new AuthenticationException("Not logged in");
        }
        
        CourseParticipantsApi api = new CourseParticipantsApi(mgmtClient);
        
        boolean isTutor;
        try {
            ParticipantDto dto = api.getParticipant(course.getId(), this.loggedInUser.getId());
            isTutor = dto.getRole() == RoleEnum.LECTURER || dto.getRole() == RoleEnum.TUTOR;
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        
        return isTutor;
    }
    
    @Override
    public Set<String> getAllGroups(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        Set<String> result = new HashSet<>();
        
        try {
            
            if (assignment.isGroupWork()) {
                AssignmentRegistrationApi api = new AssignmentRegistrationApi(mgmtClient);
                
                api.getRegisteredGroups(course.getId(), assignment.getManagementId(), null, null, null).stream()
                        .map(GroupDto::getName)
                        .forEach(result::add);
                
            } else {
                CourseParticipantsApi api = new CourseParticipantsApi(mgmtClient);
                
                api.getUsersOfCourse(course.getId(), null, null, List.of("STUDENT"), null, null).stream()
                        .map(ParticipantDto::getUsername)
                        .forEach(result::add);
            }
            
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        
        return result;
    }
    
    @Override
    public Optional<Assessment> getAssessment(Course course, Assignment assignment, String groupName)
            throws NetworkException, AuthenticationException, UserNotInCourseException,
            GroupNotFoundException, ApiException {
        
        
        Optional<Assessment> result;
        
        try {
            AssessmentApi api = new AssessmentApi(mgmtClient);
            
            List<AssessmentDto> assessment;
            if (assignment.isGroupWork()) {
                assessment = api.getAssessmentsForAssignment(course.getId(), assignment.getManagementId(),
                        null, null, null, getGroupId(course, assignment, groupName), null, null, null);
                
            } else {
                assessment = api.getAssessmentsForAssignment(course.getId(), assignment.getManagementId(),
                        null, null, null, null, getUsesrId(course, groupName), null, null);
            }
            
            if (!assessment.isEmpty()) {
                result = Optional.of(assessmentDtoToAssessment(assessment.get(0)));
                
            } else {
                result = Optional.empty();
            }
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
        
        return result;
    }
    
    /**
     * Converts a DTO to our own {@link Assessment}.
     * 
     * @param assessment The assessment to convert.
     * 
     * @return The assessment.
     */
    private Assessment assessmentDtoToAssessment(AssessmentDto assessment) {
        Assessment result = new Assessment();
        result.setManagementId(assessment.getId());
        if (assessment.getAchievedPoints() != null) {
            result.setPoints(assessment.getAchievedPoints().doubleValue());
            
            // only set draft status to false if there are points set
            if (!assessment.isIsDraft()) {
                result.setDraft(false);
            }
        }
        if (assessment.getComment() != null) {
            result.setComment(assessment.getComment());
        }
        if (assessment.getPartialAssessments() != null) {
            if (assessment.getPartialAssessments().get(0) != null) {
                if (assessment.getPartialAssessments().get(0).getMarkers() != null) {
                    List<Problem> problems = new ArrayList<>();
                    for (MarkerDto marker : assessment.getPartialAssessments().get(0).getMarkers()) {
                        Severity sev = marker.getSeverity() == SeverityEnum.ERROR ? Severity.ERROR : Severity.WARNING;
                        Problem problem = new Problem("Server", marker.getComment(), sev);
                        problem.setColumn(marker.getStartColumn().toBigInteger().intValueExact());
                        problem.setFile(new File(marker.getPath()));
                        problem.setLine(marker.getStartLineNumber().toBigInteger().intValueExact());
                        problems.add(problem);
                        
                    }
                    result.setProblems(problems);
                }
            }
        }
        return result;
    }

    @Override
    public void uploadAssessment(Course course, Assignment assignment, String groupName, Assessment assessment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException {
        
        try {
            AssessmentApi api = new AssessmentApi(mgmtClient);
            
            if (assessment.getManagementId().isEmpty()) {
                // create new assessment
                AssessmentCreateDto dto = new AssessmentCreateDto();
                dto.setAssignmentId(assignment.getManagementId());
                dto.setIsDraft(assessment.isDraft());
                assessment.getComment().ifPresent(dto::setComment);
                assessment.getPoints().map(BigDecimal::valueOf).ifPresent(dto::setAchievedPoints);
                if (assignment.isGroupWork()) {
                    dto.setGroupId(getGroupId(course, assignment, groupName));
                } else {
                    dto.setUserId(getUsesrId(course, groupName));
                }
                
                api.createAssessment(dto, course.getId(), assignment.getManagementId());
                
            } else {
                // update assessment
                AssessmentUpdateDto dto = new AssessmentUpdateDto();
                dto.setIsDraft(assessment.isDraft());
                assessment.getComment().ifPresent(dto::setComment);
                assessment.getPoints().map(BigDecimal::valueOf).ifPresent(dto::setAchievedPoints);
                
                api.updateAssessment(dto, course.getId(), assignment.getManagementId(),
                        assessment.getManagementId().get());
            }
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            if (e.getCode() == 403) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            } else if (e.getCode() == 404) {
                throw new UserNotInCourseException(parseResponseMessage(e.getResponseBody()));
            }
            
            throw handleMgmtException(e);
            
        } catch (JsonParseException e) {
            throw new ApiException("Invalid JSON response", e);
        }
    }

    /**
     * Gets the student management system ID of the given group.
     * 
     * @param course The course where the assignment is in.
     * @param assignment The assignment where the group is in.
     * @param groupName The  name of the group.
     * 
     * @return The ID of the group.
     * 
     * @throws GroupNotFoundException If the group is not registered for this assignment.
     * @throws net.ssehub.studentmgmt.backend_api.ApiException If an API call fails.
     */
    private String getGroupId(Course course, Assignment assignment, String groupName)
            throws GroupNotFoundException, net.ssehub.studentmgmt.backend_api.ApiException {
        
        AssignmentRegistrationApi groupApi = new AssignmentRegistrationApi(mgmtClient);
        String groupId = groupApi.getRegisteredGroups(course.getId(), assignment.getManagementId(),
            null, null, groupName).stream()
                .filter(dto -> dto.getName().equals(groupName))
                .map(dto -> dto.getId())
                .findFirst()
                .orElseThrow(() -> new GroupNotFoundException("Group " + groupName + " not found in assignment "
                        + assignment.getName()));
        return groupId;
    }
    
    /**
     * Gets the student management system ID of the given user.
     * 
     * @param course The course where the assignment is in.
     * @param userName The  name of the user.
     * 
     * @return The ID of the group.
     * 
     * @throws GroupNotFoundException If the user is not registered in this course.
     * @throws net.ssehub.studentmgmt.backend_api.ApiException If an API call fails.
     */
    private String getUsesrId(Course course, String userName)
            throws GroupNotFoundException, net.ssehub.studentmgmt.backend_api.ApiException {
        
        CourseParticipantsApi userApi = new CourseParticipantsApi(mgmtClient);
        String userId = userApi.getUsersOfCourse(course.getId(), null, null, null, userName, null).stream()
                .filter(dto -> dto.getUsername().equals(userName))
                .map(dto -> dto.getUserId())
                .findFirst()
                .orElseThrow(() -> new GroupNotFoundException("User " + userName + " not found in course "
                        + course.getId()));
        return userId;
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
