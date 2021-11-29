package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;
import net.ssehub.teaching.exercise_submitter.server.api.ApiClient;
import net.ssehub.teaching.exercise_submitter.server.api.ApiException;
import net.ssehub.teaching.exercise_submitter.server.api.api.SubmissionApi;
import net.ssehub.teaching.exercise_submitter.server.api.model.FileDto;

public class SubmitterIT {

    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/SubmitterTest");
    
    private static final File SINGLE_FILE_DIR = new File(TESTDATA, "SingleFile");
    
    private static final File SINGLE_FILE_OVERWRITTEN_DIR = new File(TESTDATA, "SingleFileDifferent");
    
    private static final File TWO_FILE_DIR = new File(TESTDATA, "TwoFiles");
    
    private static final File ECLIPSE_DIR = new File(TESTDATA, "EclipseStructure");
    
    private static final File COMPILATION_ERROR_DIR = new File(TESTDATA, "CompilationError");

    private static String courseId;

    private static Map<String, String> assignmentids = new HashMap<>();

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

        assignmentids.put("submitSingleFile",
                docker.createAssignment(courseId, "submitSingleFile",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        assignmentids.put("eclipseProjectFilesAndClassFilesIgnored",
                docker.createAssignment(courseId, "eclipseProjectFilesAndClassFilesIgnored",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        assignmentids.put("existingFileOverwritten",
                docker.createAssignment(courseId, "existingFileOverwritten",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        
        assignmentids.put("newFileAddedToExistingSubmission",
                docker.createAssignment(courseId, "newFileAddedToExistingSubmission",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        assignmentids.put("existingFileDeleted",
                docker.createAssignment(courseId, "existingFileDeleted",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        assignmentids.put("tooLargeFileRejected",
                docker.createAssignment(courseId, "tooLargeFileRejected",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        
        assignmentids.put("compilationProblemInResult",
                docker.createAssignment(courseId, "compilationProblemInResult",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
        docker.setAssignmentToolConfigString(courseId, assignmentids.get("compilationProblemInResult"),
                "exercise-submitter-checks",
                "[{\"check\":\"javac\"},{\"check\":\"checkstyle\",\"rules\":\"checkstyle.xml\"}]");
        
        assignmentids.put("authFailure",
                docker.createAssignment(courseId, "authFailure",
                        AssignmentState.SUBMISSION, Collaboration.GROUP));
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }

    @Test
    public void submitSingleFile() {
        // setup
        String homeworkname = "submitSingleFile";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(SINGLE_FILE_DIR));

        // check result
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        
        assertAll(
            () -> assertEquals(1, onServer.size()),
            () -> assertEquals("Main.java", onServer.get(0).getPath()),
            () -> assertEquals("\n"
                    + "public class Main {\n"
                    + "    \n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello world!\");\n"
                    + "    }\n"
                    + "}\n",
                    decodeToUtf8(onServer.get(0).getContent()))
        );
    }

    @Test
    public void eclipseProjectFilesAndClassFilesIgnored() {
        // setup
        String homeworkname = "eclipseProjectFilesAndClassFilesIgnored";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(ECLIPSE_DIR));

        // check result
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        Set<String> filepaths = onServer.stream().map(FileDto::getPath).collect(Collectors.toSet());
        
        assertEquals(Set.of("src/test/Main.java", "src/test/Test.java"), filepaths);
    }
    
    @Test
    public void existingFileOverwritten() throws InterruptedException {
        // setup
        String homeworkname = "existingFileOverwritten";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // submit pre-existing file
        assertDoesNotThrow(() -> submitter.submit(SINGLE_FILE_DIR));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(SINGLE_FILE_OVERWRITTEN_DIR));

        // check result
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        
        assertAll(
            () -> assertEquals(1, onServer.size()),
            () -> assertEquals("Main.java", onServer.get(0).getPath()),
            () -> assertEquals("\n"
                    + "public class Main {\n"
                    + "    \n"
                    + "    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Hello overwritten!\");\n"
                    + "    }\n"
                    + "}\n",
                    decodeToUtf8(onServer.get(0).getContent()))
        );
    }

    @Test
    public void newFileAddedToExistingSubmission() throws InterruptedException {
        // setup
        String homeworkname = "newFileAddedToExistingSubmission";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // submit pre-existing file
        assertDoesNotThrow(() -> submitter.submit(SINGLE_FILE_DIR));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(TWO_FILE_DIR));

        // check result
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        Set<String> filepaths = onServer.stream().map(FileDto::getPath).collect(Collectors.toSet());
        
        assertEquals(Set.of("Main.java", "Second.java"), filepaths);
    }

    @Test
    public void existingFileDeleted() throws InterruptedException {
        // setup
        String homeworkname = "existingFileDeleted";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // submit pre-existing file
        assertDoesNotThrow(() -> submitter.submit(TWO_FILE_DIR));
        Thread.sleep(1000); // wait a second, since server stores versions by second-based timestamp

        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(SINGLE_FILE_DIR));

        // check result
        SubmissionResult exptectedEmptyResult = new SubmissionResult(true, Collections.emptyList());

        assertEquals(exptectedEmptyResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        Set<String> filepaths = onServer.stream().map(FileDto::getPath).collect(Collectors.toSet());
        
        assertEquals(Set.of("Main.java"), filepaths);
    }
    
    // #######################################################################

    @Test
    public void tooLargeFileRejected() throws IOException {
        File tempDir = Files.createTempDirectory("SubmitterIT.tooLargeFileRejected").toFile();

        try {
            // create file above 10mb
            File bigFile = new File(tempDir, "bigfile.txt");
            bigFile.createNewFile();
            
            FileWriter fw = new FileWriter(bigFile);
            for (int i = 0; i < 11000; i++) {
                for (int e = 0; e < 1000; e++) {
                    fw.write("W");
                }
                fw.write("\n");
            }
            fw.close();
            
            String homeworkname = "tooLargeFileRejected";
            Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                    courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
            
            // execute
            SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(tempDir));
            
            // check result
            Problem p1 = new Problem("file-size", "Submission size is too large", Severity.ERROR);
            Problem p2 = new Problem("file-size", "File is too large", Severity.ERROR);
            p2.setFile(new File("bigfile.txt"));
            
            SubmissionResult expectedRejectingResult = new SubmissionResult(false, Arrays.asList(p1, p2));
            assertEquals(expectedRejectingResult, result);
            
        } finally {
            Files.walkFileTree(tempDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    @Test
    public void compilationProblemInResult() {
        // setup
        String homeworkname = "compilationProblemInResult";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", docker.getAuthToken("student1"));
        
        // execute
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(COMPILATION_ERROR_DIR));

        // check result
        Problem p1 = new Problem("checkstyle", "Empty if block", Severity.ERROR);
        p1.setFile(new File("Main.java"));
        p1.setLine(4);
        p1.setColumn(27);
        
        Problem p2 = new Problem("checkstyle",
                "'if rcurly' has incorrect indentation level 12, expected level should be 8", Severity.ERROR);
        p2.setFile(new File("Main.java"));
        p2.setLine(6);
        p2.setColumn(13);
        
        Problem p3 = new Problem("javac", "cannot find symbol", Severity.ERROR);
        p3.setFile(new File("Main.java"));
        p3.setLine(7);
        p3.setColumn(9);

        SubmissionResult expectedResult = new SubmissionResult(true, Arrays.asList(p1, p2, p3));
        assertEquals(expectedResult, result);

        // check files on server
        List<FileDto> onServer = getLatestSubmission(homeworkname, "JP001");
        Set<String> filepaths = onServer.stream().map(FileDto::getPath).collect(Collectors.toSet());
        
        assertEquals(Set.of("Main.java"), filepaths);
    }
    
    @Test
    public void authFailure() {
        // setup
        String homeworkname = "authFailure";
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(),
                courseId, homeworkname, "JP001", "invalid_token");

        // execute
        SubmissionException e = assertThrows(SubmissionException.class, () -> submitter.submit(SINGLE_FILE_DIR));
        
        assertAll(
            () -> assertEquals("Failed to upload submission", e.getMessage()),
            () -> assertInstanceOf(ApiException.class, e.getCause()),
            () -> assertEquals("Unauthorized", e.getCause().getMessage())
        );
    }
    
    private List<FileDto> getLatestSubmission(String assignment, String group) {
        ApiClient client = new ApiClient();
        client.setBasePath(docker.getExerciseSubmitterServerUrl());
        client.setAccessToken(docker.getAuthToken("adam"));
        
        SubmissionApi api = new SubmissionApi(client);
        
        return assertDoesNotThrow(() -> api.getLatest(courseId, assignment, group));
    }
    
    private static String decodeToUtf8(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

}
