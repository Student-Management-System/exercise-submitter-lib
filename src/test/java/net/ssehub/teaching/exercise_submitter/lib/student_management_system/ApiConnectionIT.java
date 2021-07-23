package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionIT {
    
    private static StuMgmtDocker docker;
    
    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("adam", "123456");
        
        docker.createUser("student1", "Bunny123");
        
        String courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
        docker.enrollStudentInCourse(courseId, "student1");
        
        docker.createCourse("notenrolled", "wise2021", "Not Enrolled", "adam");
        
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
        assertThrows(NoSuchElementException.class, () -> api.getCourse("wrongCourse", "wise2021"));
    }
    @Test
    public void getCourseStudentNotEnrolled() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
       
        assertThrows(NoSuchElementException.class,() -> {
            
            Course course = api.getCourse("notenrolled", "wise2021"); 
            assertTrue(course != null);
        
    });
    }
    
    @Test
    public void getStudentEnrolledCourse() {
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("student1", "Bunny123"));
       
        assertDoesNotThrow(() -> {
            
            Course course = api.getCourse("java", "wise2021"); 
            assertTrue(course != null);
        
    });
    }
    
}

