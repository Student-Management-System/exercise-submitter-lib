package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
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

        // start the SVN late, so that only one assignment change event triggers a full update
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
        //check result
        SubmissionResult result = submitter.submit(dir);
        List<Problem> emptylist = new ArrayList<Problem>();
        SubmissionResult resultTest = new SubmissionResult(true,emptylist);
        
        assertEquals(result, resultTest);
        
       //check files on server
        List<String> testFileList = new ArrayList<String>();
        testFileList.add("..");
        testFileList.add(".classpath");
        testFileList.add(".project");
        testFileList.add("Main.java");
        
        
        String responseFileList = SubmitterIT.docker.getHTTPResponseSvnFile(SubmitterIT.homework02id, Optional.empty(), "student1");
        List<String> reponselist = SubmitterIT.docker.handleHtmlResponseGetListElements(responseFileList);
        
        assertEquals(testFileList, reponselist);
        assertEquals(SubmitterIT.docker.getHTTPResponseSvnFile(SubmitterIT.homework02id, Optional.ofNullable(".classpath"), "student1"), 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<classpath>\n"
                + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                + "</classpath>\n");
               
        assertEquals(SubmitterIT.docker.getHTTPResponseSvnFile(SubmitterIT.homework02id, Optional.ofNullable(".project"), "student1"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<projectDescription>\n"
                + "    <name>Works</name>\n"
                + "    <comment></comment>\n"
                + "    <projects>\n"
                + "    </projects>\n"
                + "    <buildSpec>\n"
                + "        <buildCommand>\n"
                + "            <name>org.eclipse.jdt.core.javabuilder</name>\n"
                + "            <arguments>\n"
                + "            </arguments>\n"
                + "        </buildCommand>\n"
                + "    </buildSpec>\n"
                + "    <natures>\n"
                + "        <nature>org.eclipse.jdt.core.javanature</nature>\n"
                + "    </natures>\n"
                + "</projectDescription>\n");
                
        assertEquals(SubmitterIT.docker.getHTTPResponseSvnFile(SubmitterIT.homework02id, Optional.ofNullable("Main.java"), "student1"),
                "\n"
                + "public class Main {\n"
                + "    \n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hello world!\");\n"
                + "    }\n"
                + "}\n");
                
        
        
        });
       
        
    }
    
    // TODO: create test cases for:
    // - pre-existing files on server (overwritten, deleted)
    
    @Test
    public void submitTestwithPreProblems() {
        assertDoesNotThrow(() -> {
            
       
        File dir = new File(TESTDATA, "error");
        //create file above 10mb
        Preparator prep = new Preparator(dir);
        File fileresult = prep.getResult();
        File bigFile = new File(fileresult,"bigfile.txt");
        bigFile.createNewFile();
        
        FileWriter fw = new FileWriter(bigFile);
        for(int i = 0; i < 11000; i++) {
            for(int e = 0; e < 1000; e++) {
                fw.write("W");
            }
            fw.write("\n");
        }
        fw.close();
        
        
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
        
        SubmissionResult result = submitter.submit(fileresult);
        List<Problem> list = new ArrayList<Problem>();
        
        Problem problem = new Problem("file-size","File is too large",Severity.ERROR);
        problem.setFile(new File("bigfile.txt"));
        
        Problem problem1 = new Problem("file-size","Submission size is too large",Severity.ERROR);
        list.add(problem);
        list.add(problem1);
        
        SubmissionResult resultTest = new SubmissionResult(false,list);
        
        assertEquals(result, resultTest);
        
        prep.close();
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
    @Test
    public void submitTestWithAuthFailure() {
            
        File dir = new File(TESTDATA, "Works");
        
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withDummyApiConnection();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("student1");
        fackto.withCourse("java-wise2021");
        
        assertDoesNotThrow(() -> {
        ExerciseSubmitterManager manager = fackto.build();
        
        Assignment assignment =  new Assignment("005", "Homework03", State.SUBMISSION, true);
        
        assertThrows(AuthenticationException.class, () -> {
            Submitter submitter = manager.getSubmitter(assignment);
            submitter.submit(dir);
        });
    
        });
        
    }

}
