package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager.Credentials;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;



public class SubmitterIT {

    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/SubmitterTest");
    
    private static String courseId = null;
    
    private static Map<String, String> assignmentids = new HashMap<String, String>();

    @BeforeAll
    public static void setupServers() {
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

        assignmentids.put("submitTest",
                docker.createAssignment(courseId, "submitTest",
                        AssignmentState.INVISIBLE, Collaboration.GROUP));
        assignmentids.put("submitTestEclipse",
                docker.createAssignment(courseId, "submitTestEclipse",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesOverwritten",
                docker.createAssignment(courseId, "submitTestExistingFilesOverwritten",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesDeleted",
                docker.createAssignment(courseId, "submitTestExistingFilesDeleted",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestExistingFilesAdded",
                docker.createAssignment(courseId, "submitTestExistingFilesAdded",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestwithPreProblems",
                docker.createAssignment(courseId, "submitTestwithPreProblems",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        assignmentids.put("submitTestwithPostProblem",
                docker.createAssignment(courseId, "submitTestwithPostProblem", 
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        

        // start the SVN late, so that only one assignment change event triggers a full update
        docker.startSvn(courseId, "svn");

        docker.changeAssignmentState(courseId, assignmentids.get("submitTest"), AssignmentState.SUBMISSION);
        
      
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
        Submitter submitter = new Submitter(docker.getSvnUrl() + homeworkname + "/JP001/",
                new Credentials("student1", "123456".toCharArray()));
        
        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(dir));
        
        // check
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());
        
        assertEquals(exptectedEmptyResult, result);
        
        // check files on server
        Set<String> testFileList = new HashSet<>();
        testFileList.add(".classpath");
        testFileList.add(".project");
        testFileList.add("Main.java");
        
        
        Set<String> reponselist = docker.getSvnDirectoryContent(homeworkname + "/JP001");
        
        assertEquals(testFileList, reponselist);
        assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.classpath"), 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<classpath>\n"
                + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER"
                    + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                + "</classpath>\n");
               
        assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.project"),
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
                
        assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/Main.java"),
                "\n"
                + "public class Main {\n"
                + "    \n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hello world!\");\n"
                + "    }\n"
                + "}\n");
    }
    @Test 
    public void submitTestWithEclipseProjectStructure() {
        
        assertDoesNotThrow(() -> {
            
            File dir = new File(TESTDATA, "WorksEclipse");
            
            ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
            fackto.withAuthUrl(docker.getAuthUrl());
            fackto.withMgmtUrl(docker.getStuMgmtUrl());
            fackto.withSvnUrl(docker.getSvnUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestEclipse";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
            Submitter submitter = manager.getSubmitter(assignment);
            //check result
            SubmissionResult result = submitter.submit(dir);
            List<Problem> emptylist = new ArrayList<Problem>();
            SubmissionResult resultTest = new SubmissionResult(true, emptylist);
            
            assertEquals(result, resultTest);
            
           //check files on server
            Set<String> testFileList = new HashSet<>();
            testFileList.add(".settings/");
            testFileList.add("bin/");
            testFileList.add("src/");
            testFileList.add(".classpath");
            testFileList.add(".project");
                 
            Set<String> reponselist = docker.getSvnDirectoryContent(homeworkname + "/JP001");
            
            Set<String> testFileListinSRC = new HashSet<>();
            testFileListinSRC.add("test/");
             
            Set<String> reponselistinSRC = docker.getSvnDirectoryContent(homeworkname + "/JP001/src");
            
            Set<String> testFileListinTest = new HashSet<>();
            testFileListinTest.add("Main.java");
            testFileListinTest.add("Test.java");
            
             
            Set<String> reponselistinTest = docker.getSvnDirectoryContent(homeworkname + "/JP001/src/test");
            
            assertEquals(testFileList, reponselist);
            assertEquals(testFileListinSRC, reponselistinSRC);
            assertEquals(testFileListinTest, reponselistinTest);
            String test = docker.getSvnFileOverHttp(homeworkname + "/JP001/.project");
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.classpath"), 
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<classpath>\n"
                    + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                    + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER"
                        + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                    + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                    + "</classpath>\n");
                   
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.project"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<projectDescription>\n"
                    + "    <name>test</name>\n"
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
                    + "</projectDescription>\n"
                    + "");
                    
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/src/test/Main.java"),
                  "package test;\n"
                  + "\n"
                  + "public class Main {\n"
                  + "    public static void main(String[] args) {\n"
                  + "        System.out.println(\"test\");\n"
                  + "        System.out.println(\"test2\");\n"
                  + "        System.out.println(\"test3\");\n"
                  + "        System.out.println(\"test4\"); \n"
                  + "    }\n"
                  + "\n"
                  + "}\n"
                  + "");
            
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/src/test/Test.java"),
                   "package test;\n"
                   + "\n"
                   + "public class Test {\n"
                   + "    private int test = 10;\n"
                   + "}\n"
                   + "");
        });
        
        
    }
    @Test
    public void submitTestExistingFilesOverwritten() {
        
        assertDoesNotThrow(() -> {
            
            File dir = new File(TESTDATA, "Works");
            File overwrite = new File(TESTDATA, "WorksOverwrite");
            
            ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
            fackto.withAuthUrl(docker.getAuthUrl());
            fackto.withMgmtUrl(docker.getStuMgmtUrl());
            fackto.withSvnUrl(docker.getSvnUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestExistingFilesOverwritten";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
            Submitter submitter = manager.getSubmitter(assignment);
            //simul pre existing file
            submitter.submit(overwrite);
            //check result
            SubmissionResult result = submitter.submit(dir);
            List<Problem> emptylist = new ArrayList<Problem>();
            SubmissionResult resultTest = new SubmissionResult(true, emptylist);
            
            assertEquals(result, resultTest);
            
           //check files on server
            Set<String> testFileList = new HashSet<>();
            testFileList.add(".classpath");
            testFileList.add(".project");
            testFileList.add("Main.java");
            
            
            Set<String> reponselist = docker.getSvnDirectoryContent(homeworkname + "/JP001");
            
            assertEquals(testFileList, reponselist);
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.classpath"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<classpath>\n"
                    + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                    + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/"
                            + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                    + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                    + "</classpath>\n");
                   
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.project"),
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
                    
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/Main.java"),
                    "\n"
                    + "public class Main {\n"
                    + "    \n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello world!\");\n"
                    + "    }\n"
                    + "}\n");
                    
            
            
        });
    }
    @Test
    public void submitTestExistingFilesAdded() {
        
        assertDoesNotThrow(() -> {
            
            File dir = new File(TESTDATA, "Works");
            File added = new File(TESTDATA, "WorksAdded");
            
            ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
            fackto.withAuthUrl(docker.getAuthUrl());
            fackto.withMgmtUrl(docker.getStuMgmtUrl());
            fackto.withSvnUrl(docker.getSvnUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestExistingFilesAdded";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
            Submitter submitter = manager.getSubmitter(assignment);
            
            //making based submit
            submitter.submit(dir);
            
            //adding files
            SubmissionResult result = submitter.submit(added);
            List<Problem> emptylist = new ArrayList<Problem>();
            SubmissionResult resultTest = new SubmissionResult(true, emptylist);
            
            assertEquals(result, resultTest);
            
           //check files on server
            Set<String> testFileList = new HashSet<>();
            testFileList.add(".classpath");
            testFileList.add(".project");
            testFileList.add("Main.java");
            testFileList.add("Second.java");
            
            
            Set<String> reponselist = docker.getSvnDirectoryContent(homeworkname + "/JP001");
            
            assertEquals(testFileList, reponselist);
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.classpath"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<classpath>\n"
                    + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                    + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/"
                            + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                    + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                    + "</classpath>\n");
                   
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.project"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<projectDescription>\n"
                    + "    <name>WorksAdded</name>\n"
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
                    
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/Main.java"),
                    "\n"
                    + "public class Main {\n"
                    + "    \n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello world!\");\n"
                    + "    }\n"
                    + "}\n");
            
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/Second.java"),
                        "\n"
                        + "public class Second {\n"
                        + "    \n"
                        + "    public Second() {\n"
                        + "        \n"
                        + "    }\n"
                        + "}\n"
                        + ""); 
          
        });
    }
    @Test
    public void submitTestExistingFilesDeleted() {
        assertDoesNotThrow(() -> {
            
            File dir = new File(TESTDATA, "Works");
            File delete = new File(TESTDATA, "WorksDelete");
            
            ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
            fackto.withAuthUrl(docker.getAuthUrl());
            fackto.withMgmtUrl(docker.getStuMgmtUrl());
            fackto.withSvnUrl(docker.getSvnUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestExistingFilesDeleted";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
           
            Submitter submitter = manager.getSubmitter(assignment);
            //simul pre existing file
            submitter.submit(delete);
            //check result
            SubmissionResult result = submitter.submit(dir);
            List<Problem> emptylist = new ArrayList<Problem>();
            SubmissionResult resultTest = new SubmissionResult(true, emptylist);
            
            assertEquals(result, resultTest);
            
           //check files on server
            Set<String> testFileList = new HashSet<>();
            testFileList.add(".classpath");
            testFileList.add(".project");
            testFileList.add("Main.java");
            
            
            Set<String> reponselist = docker.getSvnDirectoryContent(homeworkname + "/JP001");
            
            assertEquals(testFileList, reponselist);
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.classpath"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<classpath>\n"
                    + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                    + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/"
                            + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                    + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                    + "</classpath>\n");
                   
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/.project"),
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
                    
            assertEquals(docker.getSvnFileOverHttp(homeworkname + "/JP001/Main.java"),
                    "\n"
                    + "public class Main {\n"
                    + "    \n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello world!\");\n"
                    + "    }\n"
                    + "}\n");
                    
            
        });
    }
    
    
    @Test
    public void submitTestwithPreProblems() {
        assertDoesNotThrow(() -> {
            
       
            File dir = new File(TESTDATA, "error");
            //create file above 10mb
            SubmissionDirectory prep = new SubmissionDirectory();
            prep.prepareDir(dir);
            File fileresult = prep.getResult();
            File bigFile = new File(fileresult, "bigfile.txt");
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
            fackto.withSvnUrl(docker.getSvnUrl());
            fackto.withUsername("student1");
            fackto.withPassword("123456");
            fackto.withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = fackto.build();
            
            String homeworkname = "submitTestwithPreProblems";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
           
            Submitter submitter = manager.getSubmitter(assignment);
            
            SubmissionResult result = submitter.submit(fileresult);
            List<Problem> list = new ArrayList<Problem>();
            
            Problem problem = new Problem("file-size", "File is too large", Severity.ERROR);
            problem.setFile(new File("bigfile.txt"));
            
            Problem problem1 = new Problem("file-size", "Submission size is too large", Severity.ERROR);
            list.add(problem);
            list.add(problem1);
            
            SubmissionResult resultTest = new SubmissionResult(false, list);
            
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
            
            String homeworkname = "submitTestwithPostProblem";
            String homeworkid = assignmentids.get(homeworkname);
            Assignment assignment = new Assignment(homeworkid, homeworkname, Assignment.State.SUBMISSION, true);
            
            Submitter submitter = manager.getSubmitter(assignment);
            
            SubmissionResult result = submitter.submit(dir);
            List<Problem> list = new ArrayList<Problem>();
            
            Problem problem = new Problem("javac", "cannot find symbol", Severity.ERROR);
            problem.setFile(new File("Main.java"));
            problem.setLine(7);
            problem.setColumn(9);
            list.add(problem);
            
            
            SubmissionResult resultTest = new SubmissionResult(true, list);
            
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
