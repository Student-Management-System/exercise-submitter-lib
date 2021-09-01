package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;



public class SubmitterIT {

    private static StuMgmtDocker docker;

    private final static File TESTDATA = new File("src/test/resources/SubmitterTest");
    
    private static String homework02id = null;

    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("svn", "abcdefgh");
        docker.createUser("adam", "123456");
        docker.createUser("student1", "123456");
        docker.createUser("student2", "123456");
        docker.createUser("student3", "123456");
        docker.createUser("student4", "123456");

        String courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam", "svn");

        docker.enrollStudent(courseId, "student1");
        docker.enrollStudent(courseId, "student2");
        docker.enrollStudent(courseId, "student3");
        docker.enrollStudent(courseId, "student4");

        docker.createGroup(courseId, "JP001", "student1", "student3");
        docker.createGroup(courseId, "JP002", "student2", "student4");

        String a1 = docker.createAssignment(courseId, "Homework01", AssignmentState.INVISIBLE, Collaboration.GROUP);
        String a2 = docker.createAssignment(courseId, "Homework02", AssignmentState.INVISIBLE, Collaboration.GROUP);
        docker.createAssignment(courseId, "Testat01", AssignmentState.INVISIBLE, Collaboration.SINGLE);

        docker.changeAssignmentState(courseId, a1, AssignmentState.SUBMISSION);
        docker.changeAssignmentState(courseId, a1, AssignmentState.IN_REVIEW);

        // start the SVN late, so that only one assignment change event triggers a full
        // update
        docker.startSvn(courseId, "svn");

        docker.changeAssignmentState(courseId, a2, AssignmentState.SUBMISSION);
        
        homework02id = a2;
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }
    
    @Test 
    public void submitTest() {
        assertDoesNotThrow(() -> {
          
        File dir = new File(TESTDATA, "Works");
        
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");
        
        ExerciseSubmitterManager manager = fackto.build();
        
        
        Assignment assignment = new Assignment(SubmitterIT.homework02id,"Homework02", Assignment.State.SUBMISSION, true);
        Submitter submitter = manager.getSubmitter(assignment);
        
        SubmissionResult result = submitter.submit(dir);
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true,emptylist);
        
        assertEquals(result, resultTest);
        
        });
        
      
        
    }
    //TODO: find a way to get a pre submit problem
    @Disabled 
    public void submitTestwithPreProblems() {
        assertDoesNotThrow(() -> {
            
       
        File dir = new File(TESTDATA, "error");
        
        
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");
        
        ExerciseSubmitterManager manager = fackto.build();
        
        
        Assignment assignment = new Assignment(SubmitterIT.homework02id,"Homework02", Assignment.State.SUBMISSION, true);
        Submitter submitter = manager.getSubmitter(assignment);
        
        SubmissionResult result = submitter.submit(dir);
        List<Problem> list = new ArrayList<Problem>();
        Problem problem = new Problem("eclipse-configuration","Does not contain a valid eclipse project",Severity.ERROR);
        list.add(problem);
        
        SubmissionResult resultTest = new SubmissionResult(false,list);
        
        assertEquals(result, resultTest);
        
        });
        
      
        
    }
    @Test 
    public void submitTestwithPostProblems() {
        assertDoesNotThrow(() -> {
             
        File dir = new File(TESTDATA, "error");
        
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");
        
        ExerciseSubmitterManager manager = fackto.build();
        
        
        Assignment assignment = new Assignment(SubmitterIT.homework02id,"Homework02", Assignment.State.SUBMISSION, true);
        Submitter submitter = manager.getSubmitter(assignment);
        
        SubmissionResult result = submitter.submit(dir);
        List<Problem> list = new ArrayList<Problem>();
        
        Problem problem = new Problem("javac", "cannot find symbol", Severity.ERROR);
        problem.setFile(new File("Main.java"));
        problem.setLine(7);
        problem.setColumn(9);
        list.add(problem);
        
        
        SubmissionResult resultTest = new SubmissionResult(true,list);
        
        assertEquals(result, resultTest);
        
        });
        
      
        
    }

}
