package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonSyntaxException;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.api.AssessmentApi;
import net.ssehub.studentmgmt.backend_api.model.AssessmentCreateDto;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.data.Assessment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiConnectionTest.DummyHttpServer;

public class ApiConnectionIT {

    private static StuMgmtDocker docker;

    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("adam", "123456");

        String student1Id = docker.createUser("student1", "Bunny123");
        String student2Id = docker.createUser("student2", "abcdefgh");
        docker.createUser("student3", "abcdefgh");
        docker.createUser("notInCourse", "abcdefgh");

        createJavaCourse();
        createNotEnrolledCourse();
        createAllStatesCourse();
        createAssessmentsCourse(student1Id, student2Id);
        createGroupsCourse();
        createAssessmentUploadCourse(student1Id);
    }

    private static void createJavaCourse() {
        String javaCourseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
        docker.enrollStudent(javaCourseId, "student1");
        docker.enrollStudent(javaCourseId, "student2");
        docker.enrollStudent(javaCourseId, "student3");
        
        docker.createGroup(javaCourseId, "AwesomeGroup", "student1", "student2");
        
        docker.createAssignment(javaCourseId, "exercise01", AssignmentState.REVIEWED, Collaboration.SINGLE);
        docker.createAssignment(javaCourseId, "exercise02", AssignmentState.SUBMISSION, Collaboration.GROUP);
    }
    
    private static void createNotEnrolledCourse() {
        docker.createCourse("notenrolled", "wise2021", "Not Enrolled", "adam");
    }

    private static void createAllStatesCourse() {
        String allStatesCourse = docker.createCourse("allstates", "wise2021", "All Assignment States", "adam");
        docker.createAssignment(allStatesCourse, "assignment01", AssignmentState.SUBMISSION, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment02", AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment03", AssignmentState.REVIEWED, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment04", AssignmentState.INVISIBLE, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment05", AssignmentState.CLOSED, Collaboration.SINGLE);
    }
    
    private static void createAssessmentsCourse(String student1Id, String student2Id) {
        String assessmentsCourse = docker.createCourse("assessments", "sose20", "Assessments", "adam");
        docker.enrollStudent(assessmentsCourse, "student1");
        docker.enrollStudent(assessmentsCourse, "student2");
        
        String group01Id = docker.createGroup(assessmentsCourse, "Group01", "student1");
        docker.createGroup(assessmentsCourse, "Group02", "student2");
        
        docker.createAssignment(assessmentsCourse, "0Assessments", AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        String a1 = docker.createAssignment(
                assessmentsCourse, "1Assessment", AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        String a2 = docker.createAssignment(
                assessmentsCourse, "2Assessments", AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        String a3 = docker.createAssignment(
                assessmentsCourse, "1GroupAssessment", AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.changeAssignmentState(assessmentsCourse, a3, AssignmentState.IN_REVIEW);
        
        createAssessment(assessmentsCourse, a1, new AssessmentCreateDto()
                .assignmentId(a1).userId(student1Id).isDraft(true).comment("some comment"));
        
        createAssessment(assessmentsCourse, a2, new AssessmentCreateDto()
                .assignmentId(a2).userId(student1Id).isDraft(true));
        createAssessment(assessmentsCourse, a2, new AssessmentCreateDto()
                .assignmentId(a2).userId(student2Id).isDraft(false).comment("other comment")
                .achievedPoints(BigDecimal.valueOf(5.5)));
        
        createAssessment(assessmentsCourse, a3, new AssessmentCreateDto()
                .assignmentId(a3).groupId(group01Id).isDraft(true).comment("group comment"));
    }

    private static void createGroupsCourse() {
        String course = docker.createCourse("groups", "sose20", "Different Group Configurations", "adam");
        docker.enrollStudent(course, "student1");
        docker.enrollStudent(course, "student2");
        
        docker.createAssignment(course, "noGroups", AssignmentState.SUBMISSION, Collaboration.GROUP);
        
        docker.createGroup(course, "Group01", "student1");
        docker.createGroup(course, "Group02", "student2");
        
        docker.createAssignment(course, "twoGroups", AssignmentState.SUBMISSION, Collaboration.GROUP);
        
        docker.createAssignment(course, "single", AssignmentState.SUBMISSION, Collaboration.SINGLE);
    }
    
    private static void createAssessmentUploadCourse(String student1Id) {
        String course = docker.createCourse("assessmentupdate", "sose20", "New Assessments", "adam");
        docker.enrollStudent(course, "student1");
        docker.enrollStudent(course, "student2");
        
        docker.createAssignment(course, "singleNoPreexisting",
                AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        
        String groupId = docker.createGroup(course, "Group01", "student1", "student2");
        
        String a1 = docker.createAssignment(course, "groupNoPreexisting",
                AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.changeAssignmentState(course, a1, AssignmentState.IN_REVIEW);
        
        
        String a2 = docker.createAssignment(course, "singlePreexisting",
                AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        
        createAssessment(course, a2, new AssessmentCreateDto()
                .assignmentId(a2).userId(student1Id).comment("old comment"));
        
        String a3 = docker.createAssignment(course, "groupPreexisting",
                AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.changeAssignmentState(course, a2, AssignmentState.IN_REVIEW);
        
        createAssessment(course, a3, new AssessmentCreateDto()
                .assignmentId(a3).groupId(groupId).comment("old comment"));
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }
    
    private static void createAssessment(String courseId, String assignmentId, AssessmentCreateDto assessment) {
        ApiClient client = new ApiClient();
        client.setBasePath(docker.getStuMgmtUrl());
        client.setAccessToken(docker.getAuthToken("adam"));
        AssessmentApi api = new AssessmentApi(client);
        
        assertDoesNotThrow(() -> api.createAssessment(assessment, courseId, assignmentId));
    }
    
    @Nested
    public class Login {
        
        @Test
        public void wrongUsernameThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.login("wronguser", "123456"));
            assertEquals("Invalid credentials: Unauthorized", e.getMessage());
        }
        
        @Test
        public void wrongPasswordThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.login("student1", "123456"));
            assertEquals("Invalid credentials: Unauthorized", e.getMessage());
        }
        
        @Test
        public void mgmtUrlInvalidHostThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), "http://doesnt.exist.local:3000");
            NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void mgmtUrlNoServiceListeningThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), "http://localhost:55555");
            NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void mgmtResponseWrongContentTypeThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), "http://localhost:" + dummyServer.getPort());
            
            ApiException e = assertThrows(ApiException.class, () -> api.login("student1", "Bunny123"));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
                () -> assertNotNull(e.getCause())
            );
        }
        
        @Test
        public void mgmtResponseInvalidJsonThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), "http://localhost:" + dummyServer.getPort());
            
            ApiException e = assertThrows(ApiException.class, () -> api.login("student1", "Bunny123"));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Invalid JSON response", e.getMessage()),
                () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
            );
        }
        
        @Test
        public void login() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        }
        
    }
    
    @Nested
    public class GetCourse {
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.getCourse("java-wise2021"));
            assertEquals("Not logged in: Authorization header is missing.", e.getMessage());
        }
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            assertThrows(UserNotInCourseException.class, () -> api.getCourse("wrongCourse-wise2021"));
        }
    
        @Test
        public void studentNotEnrolledThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
    
            assertThrows(UserNotInCourseException.class, () -> api.getCourse("notenrolled-wise2021"));
        }
    
        @Test
        public void getCourse() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
    
            
            Course course = assertDoesNotThrow(() -> api.getCourse("java-wise2021"));
            
            assertAll(
                () -> assertEquals("java-wise2021", course.getId()),
                () -> assertEquals("Programmierpraktikum: Java", course.getName())
            );
        }
        
    }
    
    @Nested
    public class GetAllCourses {
        
        @Test
        public void getAllCourses() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            // no login required
            
            Set<Course> courses = assertDoesNotThrow(() -> api.getAllCourses());
            
            assertEquals(Set.of(
                    new Course("Programmierpraktikum: Java", "java-wise2021"),
                    new Course("Not Enrolled", "notenrolled-wise2021"),
                    new Course("All Assignment States", "allstates-wise2021"),
                    new Course("Assessments", "assessments-sose20"),
                    new Course("Different Group Configurations", "groups-sose20"),
                    new Course("New Assessments", "assessmentupdate-sose20")), courses);
        }
        
    }
    
    @Nested
    public class GetAssignments {
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.getAssignments(new Course("Java", "java-wise2021")));
            assertEquals("Not logged in: Authorization header is missing.", e.getMessage());
        }
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
    
            assertThrows(UserNotInCourseException.class,
                () -> api.getAssignments(new Course("NotExisting", "notexisting")));
        }
    
        @Test
        public void correctCourseReturnsAssignments() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
    
            List<Assignment> assignments = assertDoesNotThrow(
                () -> api.getAssignments(new Course("Java", "java-wise2021")));
            
            Assignment exercise01;
            Assignment exercise02;
            
            if (assignments.get(0).getName().equals("exercise01")) {
                exercise01 = assignments.get(0);
                exercise02 = assignments.get(1);
            } else {
                exercise02 = assignments.get(0);
                exercise01 = assignments.get(1);
            }
            
            assertAll(
                () -> assertEquals(2, assignments.size()),
                
                () -> assertEquals("exercise01", exercise01.getName()),
                () -> assertEquals(Assignment.State.REVIEWED, exercise01.getState()),
                () -> assertEquals(false, exercise01.isGroupWork()),
                
                () -> assertEquals("exercise02", exercise02.getName()),
                () -> assertEquals(Assignment.State.SUBMISSION, exercise02.getState()),
                () -> assertEquals(true, exercise02.isGroupWork())
            );
        }
        
        @Test
        public void allStates( ) {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("allstates-wise2021"));
            
            assertAll(
                () -> assertEquals(State.SUBMISSION, getAssignmentByName(api, course, "assignment01").getState()),
                () -> assertEquals(State.IN_REVIEW, getAssignmentByName(api, course, "assignment02").getState()),
                () -> assertEquals(State.REVIEWED, getAssignmentByName(api, course, "assignment03").getState()),
                () -> assertEquals(State.INVISIBLE, getAssignmentByName(api, course, "assignment04").getState()),
                () -> assertEquals(State.CLOSED, getAssignmentByName(api, course, "assignment05").getState())
            );
            
        }
        
    }
    
    @Nested
    public class GetGroupName {
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getGroupName(
                    new Course("NotExisting", "notexisting"),
                    new Assignment("001", "exercise01", State.SUBMISSION, false)));
        }
        
        @Test
        public void notExistingAssignmentThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("java-wise2021"));
            
            assertThrows(GroupNotFoundException.class, () -> api.getGroupName(
                    course,
                    new Assignment("12345678-1234-1234-1234-123456789abc", "doesntexist", State.SUBMISSION, false)));
        }
        
        @Test
        public void notInCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getGroupName(
                    new Course("Java", "java-wise2021"),
                    new Assignment("001", "exercise01", State.SUBMISSION, false)));
        }
        
        @Test
        public void nonGroupAssignmentThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("java-wise2021"));
            Assignment exercise01 = getAssignmentByName(api, course, "exercise01");
            
            assertThrows(GroupNotFoundException.class, () -> api.getGroupName(course, exercise01));
        }
        
        @Test
        public void notInAnyGroupThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student3", "abcdefgh"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("java-wise2021"));
            Assignment exercise01 = getAssignmentByName(api, course, "exercise02");
            
            assertThrows(GroupNotFoundException.class, () -> api.getGroupName(course, exercise01));
        }
        
        @Test
        public void groupAssignmentReturnsGroupName() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("java-wise2021"));
            Assignment exercise02 = getAssignmentByName(api, course, "exercise02");
            
            String groupName = assertDoesNotThrow(() -> api.getGroupName(course, exercise02));
            assertEquals("AwesomeGroup", groupName);
        }
        
    }
    
    @Nested
    public class HasTutorRights {
        
        @Test
        public void falseForStudent() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            assertFalse(assertDoesNotThrow(() -> api.hasTutorRights(new Course("Java", "java-wise2021"))));
        }
        
        @Test
        public void trueForLecturer() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            assertTrue(assertDoesNotThrow(() -> api.hasTutorRights(new Course("Java", "java-wise2021"))));
        }
        
        @Test
        public void notInCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
            
            assertThrows(UserNotInCourseException.class, () -> api.hasTutorRights(new Course("Java", "java-wise2021")));
        }
        
        @Test
        public void nonExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            assertThrows(UserNotInCourseException.class,
                () -> api.hasTutorRights(new Course("NotExisting", "notexisting")));
        }
        
    }
    
    @Nested
    public class GetAllGroups {
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.getAllGroups(
                        new Course("Java", "groups-sose20"), new Assignment("123", "", State.IN_REVIEW, true)));
            assertEquals("Not logged in: Authorization header is missing.", e.getMessage());
        }
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAllGroups(
                    new Course("NotExisting", "notexisting"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false)));
        }
        
        @Test
        @Disabled("Causes internal server error in the mgmt client")
        public void notExistingAssignmentThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("groups-sose20"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAllGroups(
                    course,
                    new Assignment("12345678-1234-1234-1234-123456789abc", "doesntexist", State.SUBMISSION, false)));
        }
        
        @Test
        public void notInCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAllGroups(
                    new Course("Java", "groups-sose20"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false)));
        }
        
        @Test
        public void notATutorThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("groups-sose20"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAllGroups(
                    course, getAssignmentByName(api, course, "noGroups")));
        }
        
        @Test
        public void noGroupsReturnsEmpty() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("groups-sose20"));
            
            Set<String> groups = assertDoesNotThrow(
                () -> api.getAllGroups(course, getAssignmentByName(api, course, "noGroups")));
            
            assertEquals(Collections.emptySet(), groups);
        }
        
        @Test
        public void twoGroupsReturnsSet() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("groups-sose20"));
            
            Set<String> groups = assertDoesNotThrow(
                () -> api.getAllGroups(course, getAssignmentByName(api, course, "twoGroups")));
            
            assertEquals(Set.of("Group01", "Group02"), groups);
        }
        
        @Test
        public void singleAssignmentReturnsParticipants() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("groups-sose20"));
            
            Set<String> groups = assertDoesNotThrow(
                () -> api.getAllGroups(course, getAssignmentByName(api, course, "single")));
            
            assertEquals(Set.of("student1", "student2"), groups);
        }
        
    }
    
    @Nested
    public class GetAssessment {
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.getAssessment(new Course("Java", "assessments-sose20"),
                        new Assignment("123", "", State.IN_REVIEW, true), "student1"));
            assertEquals("Not logged in: Authorization header is missing.", e.getMessage());
        }
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAssessment(
                    new Course("NotExisting", "notexisting"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false), "student1"));
        }
        
        @Test
        public void notExistingAssignmentReturnsEmpty() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            assertEquals(Optional.empty(), assertDoesNotThrow(() -> api.getAssessment(course,
                    new Assignment("12345678-1234-1234-1234-123456789abc", "doesntexist", State.SUBMISSION, false),
                    "student1")));
        }
        
        @Test
        public void notInCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAssessment(
                    new Course("Java", "assessments-sose20"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false), "student1"));
        }
        
        @Test
        public void notATutorThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            assertThrows(UserNotInCourseException.class, () -> api.getAssessment(
                    course, getAssignmentByName(api, course, "1Assessment"), "student1"));
        }
        
        @Test
        public void notExistingUserThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            assertThrows(GroupNotFoundException.class, () -> api.getAssessment(
                    course, getAssignmentByName(api, course, "1Assessment"), "DoesntExist"));
        }
        
        @Test
        public void notExistingGroupThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            assertThrows(GroupNotFoundException.class, () -> api.getAssessment(
                    course, getAssignmentByName(api, course, "1GroupAssessment"), "DoesntExist"));
        }
        
        @Test
        public void zeroAssessmentsReturnsEmpty() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            assertEquals(Optional.empty(), assertDoesNotThrow(() -> api.getAssessment(course,
                    getAssignmentByName(api, course, "0Assessments"), "student1")));
        }
        
        @Test
        public void oneAssessmentsReturnsAssessment() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            Optional<Assessment> assessment = assertDoesNotThrow(() -> api.getAssessment(course,
                    getAssignmentByName(api, course, "1Assessment"), "student1"));
            
            assertAll(
                () -> assertEquals(Optional.of(buildAssessment(true, null, "some comment")), assessment),
                () -> assertTrue(assessment.get().getManagementId().isPresent())
            );
        }
        
        @Test
        public void assessmentWithPointsAndNonDraft() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            Optional<Assessment> assessment = assertDoesNotThrow(() -> api.getAssessment(course,
                    getAssignmentByName(api, course, "2Assessments"), "student2"));
            
            assertAll(
                () -> assertEquals(Optional.of(buildAssessment(false, 5.5, "other comment")), assessment),
                () -> assertTrue(assessment.get().getManagementId().isPresent())
            );
        }
        
        @Test
        public void groupAssessment() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessments-sose20"));
            
            Optional<Assessment> assessment = assertDoesNotThrow(() -> api.getAssessment(course,
                    getAssignmentByName(api, course, "1GroupAssessment"), "Group01"));
            
            assertAll(
                () -> assertEquals(Optional.of(buildAssessment(true, null, "group comment")), assessment),
                () -> assertTrue(assessment.get().getManagementId().isPresent())
            );
        }
        
    }
    
    @Nested
    public class UploadAssessment {
        

        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> api.uploadAssessment(new Course("Java", "assessmentupdate-sose20"),
                        new Assignment("123", "", State.IN_REVIEW, true), "student1", new Assessment()));
            assertEquals("Not logged in: Authorization header is missing.", e.getMessage());
        }
        
        @Test
        public void notExistingCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            assertThrows(UserNotInCourseException.class, () -> api.uploadAssessment(
                    new Course("NotExisting", "notexisting"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false), "student1", new Assessment()));
        }
        
        @Test
        public void notExistingAssignmentThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            assertThrows(UserNotInCourseException.class, () -> api.uploadAssessment(course,
                    new Assignment("12345678-1234-1234-1234-123456789abc", "doesntexist", State.SUBMISSION, false),
                    "student1", new Assessment()));
        }
        
        @Test
        public void notInCourseThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
            
            assertThrows(UserNotInCourseException.class, () -> api.uploadAssessment(
                    new Course("Java", "assessmentupdate-sose20"),
                    new Assignment("001", "1Assessment", State.SUBMISSION, false), "student1", new Assessment()));
        }
        
        @Test
        public void notATutorThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            assertThrows(UserNotInCourseException.class, () -> api.uploadAssessment(
                    course, getAssignmentByName(api, course, "singleNoPreexisting"), "student1", new Assessment()));
        }
        
        @Test
        public void notExistingUserThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            assertThrows(GroupNotFoundException.class, () -> api.uploadAssessment(
                    course, getAssignmentByName(api, course, "singleNoPreexisting"), "DoesntExist", new Assessment()));
        }
        
        @Test
        public void notExistingGroupThrows() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            assertThrows(GroupNotFoundException.class, () -> api.uploadAssessment(
                    course, getAssignmentByName(api, course, "groupNoPreexisting"), "DoesntExist", new Assessment()));
        }
        
        @Test
        public void newAssesmentCreatedForUser() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            Assignment assignment = getAssignmentByName(api, course, "singleNoPreexisting");
            
            Assessment newAssessment = new Assessment();
            newAssessment.setPoints(4.5);
            newAssessment.setComment("some comment");
            newAssessment.setDraft(false);
            
            assertDoesNotThrow(() -> api.uploadAssessment(course, assignment, "student1", newAssessment));
            
            assertEquals(Optional.of(newAssessment),
                    assertDoesNotThrow(() -> api.getAssessment(course, assignment, "student1")));
        }
        
        @Test
        public void newAssesmentCreatedForGroup() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            Assignment assignment = getAssignmentByName(api, course, "groupNoPreexisting");
            
            Assessment newAssessment = new Assessment();
            newAssessment.setPoints(4.5);
            newAssessment.setComment("some comment");
            newAssessment.setDraft(false);
            
            assertDoesNotThrow(() -> api.uploadAssessment(course, assignment, "Group01", newAssessment));
            
            assertEquals(Optional.of(newAssessment),
                    assertDoesNotThrow(() -> api.getAssessment(course, assignment, "Group01")));
        }
        
        @Test
        public void assesmentUpdatedForUser() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            Assignment assignment = getAssignmentByName(api, course, "singlePreexisting");
            
            Assessment assessment = assertDoesNotThrow(() -> api.getAssessment(course, assignment, "student1"))
                    .orElseThrow();
            assessment.setPoints(4.5);
            assessment.setComment("new comment");
            assessment.setDraft(false);
            
            assertDoesNotThrow(() -> api.uploadAssessment(course, assignment, "student1", assessment));
            
            assertEquals(Optional.of(assessment),
                    assertDoesNotThrow(() -> api.getAssessment(course, assignment, "student1")));
        }
        
        @Test
        public void assesmentUpdatedForGroup() {
            ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
            assertDoesNotThrow(() -> api.login("adam", "123456"));
            
            Course course = assertDoesNotThrow(() -> api.getCourse("assessmentupdate-sose20"));
            
            Assignment assignment = getAssignmentByName(api, course, "groupPreexisting");
            
            Assessment assessment = assertDoesNotThrow(() -> api.getAssessment(course, assignment, "Group01"))
                    .orElseThrow();
            assessment.setPoints(4.5);
            assessment.setComment("new comment");
            assessment.setDraft(false);
            
            assertDoesNotThrow(() -> api.uploadAssessment(course, assignment, "Group01", assessment));
            
            assertEquals(Optional.of(assessment),
                    assertDoesNotThrow(() -> api.getAssessment(course, assignment, "Group01")));
        }
        
    }
    
    private static Assessment buildAssessment(boolean draft, Double points, String comment) {
        Assessment result = new Assessment();
        if (points != null) {
            result.setPoints(points);
        }
        if (comment != null) {
            result.setComment(comment);
        }
        result.setDraft(draft);
        
        return result;
    }
    
    private static Assignment getAssignmentByName(ApiConnection api, Course course, String name) {
        List<Assignment> assignments = assertDoesNotThrow(() -> api.getAssignments(course));
        Assignment assignment = null;
        for (Assignment a : assignments) {
            if (a.getName().equals(name)) {
                assignment = a;
                break;
            }
        }
        assertNotNull(assignment, "Precondition: Assignment " + name + " found");
        return assignment;
    }

}
