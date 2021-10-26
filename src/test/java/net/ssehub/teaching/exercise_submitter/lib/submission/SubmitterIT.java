package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;

public class SubmitterIT {

    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/SubmitterTest");

    private static String courseId = null;

    private static Map<String, String> assignmentids = new HashMap<String, String>();

    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("adam", "123456");
        docker.createUser("student1", "123456");
        docker.createUser("student2", "123456");
        docker.createUser("student3", "123456");
        docker.createUser("student4", "123456");

        courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
        docker.enableExerciseSubmissionServer(courseId);

        docker.enrollStudent(courseId, "student1");
        docker.enrollStudent(courseId, "student2");
        docker.enrollStudent(courseId, "student3");
        docker.enrollStudent(courseId, "student4");

        docker.createGroup(courseId, "JP001", "student1", "student3");
        docker.createGroup(courseId, "JP002", "student2", "student4");

        assignmentids.put("submitTest",
                docker.createAssignment(courseId, "submitTest", AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestEclipse", docker.createAssignment(courseId, "submitTestEclipse",
                AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesOverwritten", docker.createAssignment(courseId,
                "submitTestExistingFilesOverwritten", AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesDeleted", docker.createAssignment(courseId,
                "submitTestExistingFilesDeleted", AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesAdded", docker.createAssignment(courseId,
                "submitTestExistingFilesAdded", AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestwithPreProblems", docker.createAssignment(courseId, "submitTestwithPreProblems",
                AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestwithPostProblem", docker.createAssignment(courseId, "submitTestwithPostProblem",
                AssignmentState.SUBMISSION, Collaboration.GROUP));
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }

    @Test
    public void submitTest() {
        // setup
        File dir = new File(TESTDATA, "Works");
        String homeworkname = "submitTest";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));

        // check
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // TODO: check files on server
    }

    @Test
    public void submitTestWithEclipseProjectStructure() {
        File dir = new File(TESTDATA, "WorksEclipse");

        String homeworkname = "submitTestEclipse";
        
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));

        // check result
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true, emptylist);

        assertEquals(resultTest, result);

        // TODO: check files on server
    }

    @Test
    public void submitTestExistingFilesOverwritten() throws InterruptedException {

        File dir = new File(TESTDATA, "Works");
        File overwrite = new File(TESTDATA, "WorksOverwrite");

        String homeworkname = "submitTestExistingFilesOverwritten";

        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // simul pre existing file
        assertDoesNotThrow(() -> submitter.submit(overwrite));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp
        
        // check result
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true, emptylist);

        assertEquals(resultTest, result);

        // TODO: check files on server
    }

    @Test
    public void submitTestExistingFilesAdded() throws InterruptedException {

        File dir = new File(TESTDATA, "Works");
        File added = new File(TESTDATA, "WorksAdded");

        String homeworkname = "submitTestExistingFilesAdded";

        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // making base submit
        assertDoesNotThrow(() -> submitter.submit(dir));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp

        // adding files
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(added));
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true, emptylist);

        assertEquals(resultTest, result);

        // TODO: check files on server
    }

    @Test
    public void submitTestExistingFilesDeleted() throws InterruptedException {

        File dir = new File(TESTDATA, "Works");
        File delete = new File(TESTDATA, "WorksDelete");

        String homeworkname = "submitTestExistingFilesDeleted";

        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // simul pre existing file
        assertDoesNotThrow(() -> submitter.submit(delete));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp
        
        // check result
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true, emptylist);

        assertEquals(resultTest, result);

        // TODO: check files on server
    }

    @Test
    public void submitTestwithPreProblems() throws ApiException, IOException {
        File dir = new File(TESTDATA, "error");
        File bigFile = new File(dir, "bigfile.txt");

        try {
            // create file above 10mb
            bigFile.createNewFile();
            
            FileWriter fw = new FileWriter(bigFile);
            for (int i = 0; i < 11000; i++) {
                for (int e = 0; e < 1000; e++) {
                    fw.write("W");
                }
                fw.write("\n");
            }
            fw.close();
            
            ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
            fackto.withAuthUrl(docker.getAuthUrl());
            fackto.withMgmtUrl(docker.getStuMgmtUrl());
            fackto.withExerciseSubmitterServerUrl(docker.getExerciseSubmitterServerUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestwithPreProblems";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
            Submitter submitter = manager.getSubmitter(assignment);
            
            SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
            List<Problem> list = new ArrayList<Problem>();
            
            Problem p1 = new Problem("file-size", "Submission size is too large", Severity.ERROR);
            list.add(p1);
            
            Problem p2 = new Problem("file-size", "File is too large", Severity.ERROR);
            p2.setFile(new File("bigfile.txt"));
            list.add(p2);
            
            SubmissionResult resultTest = new SubmissionResult(false, list);
            
            assertEquals(resultTest, result);
            
        } finally {
            bigFile.delete();
        }
        
    }

    @Test
    public void submitTestwithPostProblems() {

        File dir = new File(TESTDATA, "error");

        String homeworkname = "submitTestwithPostProblem";
        
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));

        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
        List<Problem> list = new ArrayList<Problem>();

        Problem p1 = new Problem("checkstyle", "Empty if block", Severity.ERROR);
        p1.setFile(new File("Main.java"));
        p1.setLine(4);
        p1.setColumn(27);
        list.add(p1);
        
        Problem p2 = new Problem("checkstyle",
                "'if rcurly' has incorrect indentation level 12, expected level should be 8", Severity.ERROR);
        p2.setFile(new File("Main.java"));
        p2.setLine(6);
        p2.setColumn(13);
        list.add(p2);
        
        Problem p3 = new Problem("javac", "cannot find symbol", Severity.ERROR);
        p3.setFile(new File("Main.java"));
        p3.setLine(7);
        p3.setColumn(9);
        list.add(p3);

        SubmissionResult resultTest = new SubmissionResult(true, list);

        assertEquals(resultTest, result);

    }

    @Test
    public void submitTestWithAuthFailure() {

        File dir = new File(TESTDATA, "Works");
        
        String homeworkname = "submitTest";

        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", "random_token");

        assertThrows(SubmissionException.class, () -> {
            submitter.submit(dir);
        });

    }

}
