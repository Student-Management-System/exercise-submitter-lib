package net.ssehub.teaching.exercise_reviewer.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_reviewer.lib.data.Submission;
import net.ssehub.teaching.exercise_reviewer.lib.student_management_system.ApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.GroupNotFoundException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.submission.SubmissionException;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;

public class ApiConnectionIT {
    
    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/SubmitterTest");

    private static String courseId = null;
    
    private static Map<String, String> assignmentids = new HashMap<String, String>();
    
    @BeforeAll
    public static void startServer() {
        docker = new StuMgmtDocker();
        docker.createUser("svn", "abcdefgh");
        docker.createUser("adam", "123456");
        docker.createUser("student1", "123456");
        docker.createUser("student2", "123456");
        docker.createUser("student3", "123456");
        docker.createUser("student4", "123456");

        courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam", "svn");

        docker.enrollStudent(courseId, "student1");
        docker.enrollStudent(courseId, "student2");
        docker.enrollStudent(courseId, "student3");
        docker.enrollStudent(courseId, "student4");

        docker.createGroup(courseId, "JP001", "student1", "student3");
        docker.createGroup(courseId, "JP002", "student2", "student4");

        assignmentids.put("Homework01",
                docker.createAssignment(courseId, "Homework01", AssignmentState.INVISIBLE, Collaboration.SINGLE));
        
        
        // start svn

       // docker.startSvn(courseId, "svn");

        docker.changeAssignmentState(courseId, assignmentids.get("Homework01"), AssignmentState.SUBMISSION);
        
        //make test submissions
        
        for (int i = 1; i <= 4; i++) {
            AtomicInteger integer = new AtomicInteger(i);
            assertDoesNotThrow(() -> submit(new File(TESTDATA, "works"),
                    new Assignment(ApiConnectionIT.assignmentids.get("Homework01"), "Homework01",
                    Assignment.State.SUBMISSION, false), "student" + Integer.toString(integer.get())));
        }
        
        
    }
    
    public static void submit(File file, Assignment assignment, String username) 
            throws IllegalArgumentException, SubmissionException,
        NetworkException, GroupNotFoundException, ApiException {
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
      //  fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername(username);
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");
        
        ExerciseSubmitterManager manager = fackto.build();
        
        Submitter submitter = manager.getSubmitter(assignment);
        // check result
        submitter.submit(file);
    }
    
    @AfterAll
    public static void stopServer() {
        docker.close();
    }
    @Disabled
    @Test
    public void getAllSubmissionFromAssignmentNoGroupTest() {
        
        ApiConnection api = new ApiConnection(docker.getAuthUrl(), docker.getStuMgmtUrl());
        assertDoesNotThrow(() -> api.login("adam", "123456"));
        
        List<Submission> submissions =  assertDoesNotThrow(() -> 
            api.getAllSubmissionFromAssignment(courseId, new Assignment(
                ApiConnectionIT.assignmentids.get("Homework01"), "Homework01",
               Assignment.State.SUBMISSION, false)));
       
        assertTrue(submissions.size() == 4);
       
        
       
        
        
    }
}
