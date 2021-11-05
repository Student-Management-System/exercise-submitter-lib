package net.ssehub.teaching.exercise_submitter.lib.replay;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.replay.Replayer.Version;
import net.ssehub.teaching.exercise_submitter.lib.submission.SubmissionResult;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;

public class ReplayerIT {

    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/ReplayerTest");
    
    private static final File VERSION_1 = new File(TESTDATA, "Version1");
    
    private static final File VERSION_2 = new File(TESTDATA, "Version2");
    
    private static final File TWO_FILES = new File(TESTDATA, "TwoFiles");
    
    private static final File SUB_DIRECTORY = new File(TESTDATA, "SubDirectory");
    
    private static String courseId;

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

        docker.createAssignment(courseId, "noSubmissions", AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.createAssignment(courseId, "twoSubmissions", AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.createAssignment(courseId, "twoFiles", AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.createAssignment(courseId, "subDirectory", AssignmentState.SUBMISSION, Collaboration.GROUP);
        
        submit(VERSION_1, "twoSubmissions", "JP001", "student3");
        try {
            Thread.sleep(1000); // wait one second, as server only accepts one submission per second
        } catch (InterruptedException e) {
        } 
        submit(VERSION_2, "twoSubmissions", "JP001", "student1");
        
        submit(TWO_FILES, "twoFiles", "JP001", "student1");
        
        submit(SUB_DIRECTORY, "subDirectory", "JP001", "student1");
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }

    private static void submit(File submissionDirectory, String assignmentName, String groupName, String user) {
        
        Submitter submitter = new Submitter(docker.getExerciseSubmitterServerUrl(), courseId,
                assignmentName, groupName, docker.getAuthToken(user));
        
        SubmissionResult result = assertDoesNotThrow(() -> submitter.submit(submissionDirectory));
        
        assertTrue(result.isAccepted());
    }
    
    @Nested
    public class GetVersions {
        
        @Test
        public void unauthorizedThrows() {
            Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId, "noSubmissions", "JP001",
                    "invalid_token");
            
            assertThrows(ReplayException.class, () -> replayer.getVersions());
        }
        
        @Test
        public void noVersions() {
            Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId, "noSubmissions", "JP001",
                    docker.getAuthToken("student1"));
            
            List<Version> versionList = assertDoesNotThrow(() -> replayer.getVersions());
            
            assertEquals(Collections.emptyList(), versionList);
        }
        
        @Test
        public void twoVersions() {
            Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"));
            
            List<Version> versionList = assertDoesNotThrow(() -> replayer.getVersions());
            
            assertAll(
                () -> assertEquals(2, versionList.size()),
                () -> assertEquals("student1", versionList.get(0).getAuthor()),
                () -> assertEquals("student3", versionList.get(1).getAuthor()),
                
                // check that there is a reasonable timestamp
                () -> assertTrue(
                        Instant.now().getEpochSecond() - versionList.get(0).getTimestamp().getEpochSecond() < 60),
                () -> assertTrue(
                        Instant.now().getEpochSecond() - versionList.get(1).getTimestamp().getEpochSecond() < 60)
            );
        }
        
    }
    
    @Nested
    public class Replay {
        
        @Test
        public void invalidVersionThrows() {
            Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId, "noSubmissions", "JP001",
                    docker.getAuthToken("student1"));
            
            assertThrows(ReplayException.class, () -> replayer.replay(new Version("student1", Instant.now())));
        }
        
        @Test
        public void invalidTokenThrows() {
            Replayer replayerCorrectToken = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"));
            
            List<Version> versions = assertDoesNotThrow(() -> replayerCorrectToken.getVersions());
            
            Replayer replayerIncorrectToken = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", "invalid_token");
            
            assertThrows(ReplayException.class, () -> replayerIncorrectToken.replay(versions.get(0)));
        }
        
        @Test
        public void latestVersionSingleFile() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                
                File sourceFile = new File(tempDirectory, "Main.java");
                assertAll(
                    () -> assertEquals(1, tempDirectory.listFiles().length),
                    () -> assertTrue(sourceFile.isFile()),
                    () -> assertEquals("\n"
                            + "public class Main {\n"
                            + "    \n"
                            + "    public static void main(String[] args) {\n"
                            + "        System.out.println(\"Hello Revision2!\");\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8))
                );
            }
        }
        
        @Test
        public void previousVersionSingleFile() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory = assertDoesNotThrow(() -> replayer.replay(versions.get(1)));
                
                File sourceFile = new File(tempDirectory, "Main.java");
                assertAll(
                    () -> assertEquals(1, tempDirectory.listFiles().length),
                    () -> assertTrue(sourceFile.isFile()),
                    () -> assertEquals("\n"
                            + "public class Main {\n"
                            + "    \n"
                            + "    public static void main(String[] args) {\n"
                            + "        System.out.println(\"Hello Revision1!\");\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8))
                );
            }
        }
        
        @Test
        public void closeDeletesTemporaryDirectory() throws IOException {
            File tempDirectory;
            
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                tempDirectory = assertDoesNotThrow(() -> replayer.replay(versions.get(1)));
            }
            
            assertFalse(tempDirectory.exists());
        }
        
        @Test
        public void sameVersionCached() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory1 = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                File tempDirectory2 = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                
                assertEquals(tempDirectory1, tempDirectory2);
            }
        }
        
        @Test
        public void differentVersionNotCached() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory1 = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                File tempDirectory2 = assertDoesNotThrow(() -> replayer.replay(versions.get(1)));
                
                assertNotEquals(tempDirectory1, tempDirectory2);
            }
        }
        
        @Test
        public void twoFiles() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoFiles", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                
                File main = new File(tempDirectory, "Main.java");
                File filehandler = new File(tempDirectory, "FileHandler.java");
                assertAll(
                    () -> assertEquals(2, tempDirectory.listFiles().length),
                    () -> assertTrue(main.isFile()),
                    () -> assertEquals("\n"
                            + "public class Main {\n"
                            + "    \n"
                            + "    public static void main(String[] args) {\n"
                            + "        System.out.println(\"Hello World!\");\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(main.toPath(), StandardCharsets.UTF_8)),
                    
                    () -> assertTrue(filehandler.isFile()),
                    () -> assertEquals("public class FileHandler() {\n"
                            + "    public FileHandler() {\n"
                            + "\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(filehandler.toPath(), StandardCharsets.UTF_8))
                );
            }
        }
        
        @Test
        public void subDirectory() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "subDirectory", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                File tempDirectory = assertDoesNotThrow(() -> replayer.replay(versions.get(0)));
                
                File subdir = new File(tempDirectory, "cli");
                File main = new File(subdir, "Main.java");
                assertAll(
                    () -> assertEquals(1, tempDirectory.listFiles().length),
                    () -> assertTrue(subdir.isDirectory()),
                    () -> assertEquals(1, subdir.listFiles().length),
                    () -> assertTrue(main.isFile()),
                    () -> assertEquals("package cli;\n"
                            + "\n"
                            + "public class Main {\n"
                            + "    \n"
                            + "    public static void main(String[] args) {\n"
                            + "        System.out.println(\"Hello World!\");\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(main.toPath(), StandardCharsets.UTF_8))
                );
            }
        }
        
    }
    
    @Nested
    public class ReplayLatest {
        
        @Test
        public void invalidTokenThrows() {
            Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", "invalid_token");
            
            assertThrows(ReplayException.class, () -> replayer.replayLatest());
        }
        
        @Test
        public void singleFile() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                File tempDirectory = assertDoesNotThrow(() -> replayer.replayLatest());
                
                File sourceFile = new File(tempDirectory, "Main.java");
                assertAll(
                    () -> assertEquals(1, tempDirectory.listFiles().length),
                    () -> assertTrue(sourceFile.isFile()),
                    () -> assertEquals("\n"
                            + "public class Main {\n"
                            + "    \n"
                            + "    public static void main(String[] args) {\n"
                            + "        System.out.println(\"Hello Revision2!\");\n"
                            + "    }\n"
                            + "}\n",
                            Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8))
                );
            }
        }
        
        @Test
        public void closeDeletesTemporaryDirectory() throws IOException {
            File tempDirectory;
            
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                tempDirectory = assertDoesNotThrow(() -> replayer.replayLatest());
            }
            
            assertFalse(tempDirectory.exists());
        }
        
    }
    
    @Nested
    public class IsSameContent {
        
        @Test
        public void sameContent() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                assertTrue(assertDoesNotThrow(() -> replayer.isSameContent(VERSION_2, versions.get(0))));
            }
        }
        
        @Test
        public void differentContent() throws IOException {
            try (Replayer replayer = new Replayer(docker.getExerciseSubmitterServerUrl(), courseId,
                    "twoSubmissions", "JP001", docker.getAuthToken("student1"))) {
                
                List<Version> versions = assertDoesNotThrow(() -> replayer.getVersions());
                
                assertFalse(assertDoesNotThrow(() -> replayer.isSameContent(VERSION_1, versions.get(0))));
            }
        }
        
    }

}
