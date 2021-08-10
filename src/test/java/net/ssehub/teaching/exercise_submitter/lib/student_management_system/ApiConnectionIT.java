package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionIT {

    private static StuMgmtDocker docker;

    private static String javaCourseId;
    
    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("adam", "123456");

        docker.createUser("student1", "Bunny123");

        javaCourseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
        docker.enrollStudent(javaCourseId, "student1");

        docker.createCourse("notenrolled", "wise2021", "Not Enrolled", "adam");
        docker.createAssignment(javaCourseId, "exercise01", AssignmentState.SUBMISSION, Collaboration.SINGLE);

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
    public void loginCorrect() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
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
    public void getStudentEnrolledCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));

        
        Course course = assertDoesNotThrow(() -> api.getCourse("java", "wise2021"));
        
        assertAll(
                () -> assertEquals("java-wise2021", course.getId()),
                () -> assertEquals("Programmierpraktikum: Java", course.getName())
        );
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
        
        assertAll(
                () -> assertEquals(1, assignments.size()),
                () -> assertEquals("exercise01", assignments.get(0).getName()),
                () -> assertEquals(Assignment.State.SUBMISSION, assignments.get(0).getState()),
                () -> assertEquals(false, assignments.get(0).isGroupWork())
        );
    }

}
