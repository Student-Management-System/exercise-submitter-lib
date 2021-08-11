package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionIT {

    private static StuMgmtDocker docker;

    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("adam", "123456");

        docker.createUser("student1", "Bunny123");
        docker.createUser("student2", "abcdefgh");
        docker.createUser("notInCourse", "abcdefgh");

        String javaCourseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
        docker.enrollStudent(javaCourseId, "student1");
        docker.enrollStudent(javaCourseId, "student2");
        
        docker.createGroup(javaCourseId, "AwesomeGroup", "student1", "student2");
        
        docker.createAssignment(javaCourseId, "exercise01", AssignmentState.REVIEWED, Collaboration.SINGLE);
        docker.createAssignment(javaCourseId, "exercise02", AssignmentState.SUBMISSION, Collaboration.GROUP);

        docker.createCourse("notenrolled", "wise2021", "Not Enrolled", "adam");
        
        String allStatesCourse = docker.createCourse("allstates", "wise2021", "All Assignment States", "adam");
        docker.createAssignment(allStatesCourse, "assignment01", AssignmentState.SUBMISSION, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment02", AssignmentState.IN_REVIEW, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment03", AssignmentState.REVIEWED, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment04", AssignmentState.INVISIBLE, Collaboration.SINGLE);
        docker.createAssignment(allStatesCourse, "assignment05", AssignmentState.CLOSED, Collaboration.SINGLE);
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }

    @Test
    public void loginWithWrongUsername() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertThrows(AuthenticationException.class, () -> api.login("wronguser", "123456"));
    }

    @Test
    public void loginWithWrongPassword() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertThrows(AuthenticationException.class, () -> api.login("student1", "123456"));
    }
    
    @Test
    public void loginWrongAuthUrl() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", docker.getStuMgmtUrl());
        assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
    }
    
    @Test
    public void loginWrongMgmtUrl() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), "http://doesnt.exist.local:3000");
        assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
    }

    @Test
    public void loginCorrect() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
    }
    
    @Test
    public void getCourseNotLoggedIn() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertThrows(AuthenticationException.class, () -> api.getCourse("java", "wise2021"));
    }
    
    @Test
    public void getNotExistingCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        assertThrows(UserNotInCourseException.class, () -> api.getCourse("wrongCourse", "wise2021"));
    }

    @Test
    public void getCourseStudentNotEnrolled() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));

        assertThrows(UserNotInCourseException.class, () -> api.getCourse("notenrolled", "wise2021"));
    }

    @Test
    public void getCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));

        
        Course course = assertDoesNotThrow(() -> api.getCourse("java", "wise2021"));
        
        assertAll(
                () -> assertEquals("java-wise2021", course.getId()),
                () -> assertEquals("Programmierpraktikum: Java", course.getName())
        );
    }
    
    @Test
    public void getAssignmentsNotLoggedIn() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertThrows(AuthenticationException.class, () -> api.getAssignments(new Course("Java", "java-wise2021")));
    }
    
    @Test
    public void getAssignmentsFromNotExistingCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));

        assertThrows(UserNotInCourseException.class, () -> api.getAssignments(new Course("NotExisting", "notexisting")));
    }

    @Test
    public void getAssignmentsFromCorrectCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));

        List<Assignment> assignments = assertDoesNotThrow(() -> api.getAssignments(new Course("Java", "java-wise2021")));
        
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
    public void getAssignmentsAllStates( ) {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("adam", "123456"));
        
        Course course = assertDoesNotThrow(() -> api.getCourse("allstates", "wise2021"));
        
        assertAll(
                () -> assertEquals(State.SUBMISSION, getAssignmentByName(api, course, "assignment01").getState()),
                () -> assertEquals(State.IN_REVIEW, getAssignmentByName(api, course, "assignment02").getState()),
                () -> assertEquals(State.REVIEWED, getAssignmentByName(api, course, "assignment03").getState()),
                () -> assertEquals(State.INVISIBLE, getAssignmentByName(api, course, "assignment04").getState()),
                () -> assertEquals(State.CLOSED, getAssignmentByName(api, course, "assignment05").getState())
        );
        
    }
    
    @Test
    public void getGroupNameNotLoggedIn() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertThrows(AuthenticationException.class, () -> api.getGroupName(new Course("Java", "java-wise2021"), new Assignment("001", "exercise01", State.SUBMISSION, false)));
    }
    
    @Test
    public void getGroupNameFromNotExistingCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        
        assertThrows(UserNotInCourseException.class, () -> api.getGroupName(new Course("NotExisting", "notexisting"), new Assignment("001", "exercise01", State.SUBMISSION, false)));
    }
    
    @Test
    public void getGroupNameFromNotExistingAssignment() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        
        Course course = assertDoesNotThrow(() -> api.getCourse("java", "wise2021"));
        
        assertThrows(GroupNotFoundException.class, () -> api.getGroupName(course, new Assignment("12345678-1234-1234-1234-123456789abc", "doesntexist", State.SUBMISSION, false)));
    }
    
    @Test
    public void getGroupNameWhileNotInCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("notInCourse", "abcdefgh"));
        
        assertThrows(UserNotInCourseException.class, () -> api.getGroupName(new Course("Java", "java-wise2021"), new Assignment("001", "exercise01", State.SUBMISSION, false)));
    }
    
    @Test
    public void getGroupNameOfNonGroupAssignment() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        
        Course course = assertDoesNotThrow(() -> api.getCourse("java", "wise2021"));
        Assignment exercise01 = getAssignmentByName(api, course, "exercise01");
        
        assertThrows(GroupNotFoundException.class, () -> api.getGroupName(course, exercise01));
    }
    
    @Test
    public void getGroupNameOfGroupAssignment() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
        
        Course course = assertDoesNotThrow(() -> api.getCourse("java", "wise2021"));
        Assignment exercise02 = getAssignmentByName(api, course, "exercise02");
        
        String groupName = assertDoesNotThrow(() -> api.getGroupName(course, exercise02));
        assertEquals("AwesomeGroup", groupName);
    }
    
    private Assignment getAssignmentByName(ApiConnection api, Course course, String name) {
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
