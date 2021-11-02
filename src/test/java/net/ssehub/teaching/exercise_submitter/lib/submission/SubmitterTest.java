package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;
import net.ssehub.teaching.exercise_submitter.server.api.model.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.api.model.CheckMessageDto.TypeEnum;
import net.ssehub.teaching.exercise_submitter.server.api.model.FileDto;
import net.ssehub.teaching.exercise_submitter.server.api.model.SubmissionResultDto;

public class SubmitterTest {
    
    private static final Path TESTDATA = Path.of("src", "test", "resources", "SubmitterTest");
    
    @Nested
    public class Submit {
        
        @Test
        public void noDirectoryThrows() {
            
            ExerciseSubmitterFactory factory = new ExerciseSubmitterFactory();
            factory.withDummyApiConnection()
                .withAuthUrl("localhost:5555")
                .withMgmtUrl("localhost:5555")
                .withExerciseSubmitterServerUrl("localhost:5555")
                .withUsername("username")
                .withPassword("username")
                .withCourse("java-wise2021");
            
            ExerciseSubmitterManager manager = assertDoesNotThrow(() -> factory.build());
            Submitter submitter = assertDoesNotThrow(
                () -> manager.getSubmitter(new Assignment("005", "Homework03", State.SUBMISSION, true)));
            
            assertThrows(IllegalArgumentException.class, () -> submitter.submit(new File("main.java")));
        }
        
    }
    
    @Nested
    public class PathToFileDto {
        
        @Test
        public void submissionDirDoesnExistThrows() {
            Path doesntExist = TESTDATA.resolve("doesn_exist");
            
            assertThrows(UncheckedIOException.class,
                () -> Submitter.pathToFileDto(Path.of("Main.java"), doesntExist));
        }
        
        @Test
        public void fileDoesnExistThrows() {
            Path submissionDir = TESTDATA.resolve("SingleFile");
            
            assertThrows(UncheckedIOException.class,
                () -> Submitter.pathToFileDto(Path.of("DoesntExist.java"), submissionDir));
        }
        
        @Test
        public void fileIsDirectoryThrows() {
            Path submissionDir = TESTDATA.resolve("EclipseStructure");
            
            assertThrows(UncheckedIOException.class,
                () -> Submitter.pathToFileDto(Path.of("src"), submissionDir));
        }
        
        @Test
        public void relativePathSet() {
            Path submissionDir = TESTDATA.resolve("EclipseStructure");
            
            FileDto result = assertDoesNotThrow(
                () -> Submitter.pathToFileDto(Path.of("src", "test", "Main.java"), submissionDir));
            
            assertEquals("src/test/Main.java", result.getPath());
        }
        
        @Test
        public void contentBase64Encoded() {
            Path submissionDir = TESTDATA.resolve("SingleFile");
            
            FileDto result = assertDoesNotThrow(
                () -> Submitter.pathToFileDto(Path.of("Main.java"), submissionDir));
            
            assertEquals("CnB1YmxpYyBjbGFzcyBNYWluIHsKICAgIAogICAgcHVibGljIHN0YXRpYyB2b2lkIG1haW4oU3Ry"
                    + "aW5nW10gYXJncykgewogICAgICAgIFN5c3RlbS5vdXQucHJpbnRsbigiSGVsbG8gd29ybGQhIik7"
                    + "CiAgICB9Cn0K", result.getContent());
        }
        
    }
    
    @Nested
    public class DtoToSubmissionResult {
        
        @Test
        public void acceptedCopied() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(true);
            dto.setMessages(Collections.emptyList());
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertEquals(true, sr.isAccepted());
        }
        
        @Test
        public void notAcceptedCopied() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(false);
            dto.setMessages(Collections.emptyList());
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertEquals(false, sr.isAccepted());
        }
        
        @Test
        public void noMessages() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(true);
            dto.setMessages(Collections.emptyList());
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertEquals(Collections.emptyList(), sr.getProblems());
        }
        
        @Test
        public void singleMessageCheckNameSeverityMessage() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(true);
            dto.setMessages(Arrays.asList(
                    new CheckMessageDto().checkName("javac").type(TypeEnum.ERROR).message("Mock Message")));
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertAll(
                () -> assertEquals(1, sr.getProblems().size()),
                () -> assertEquals("javac", sr.getProblems().get(0).getCheckName()),
                () -> assertEquals(Severity.ERROR, sr.getProblems().get(0).getSeverity()),
                () -> assertEquals("Mock Message", sr.getProblems().get(0).getMessage()),
                () -> assertFalse(sr.getProblems().get(0).getFile().isPresent()),
                () -> assertFalse(sr.getProblems().get(0).getLine().isPresent()),
                () -> assertFalse(sr.getProblems().get(0).getColumn().isPresent())
            );
        }
        
        @Test
        public void multipleMessagesCheckNameSeverityMessage() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(true);
            dto.setMessages(Arrays.asList(
                    new CheckMessageDto().checkName("javac").type(TypeEnum.ERROR).message("Mock Message"),
                    new CheckMessageDto().checkName("checkstyle").type(TypeEnum.WARNING).message("Some message")));
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertAll(
                () -> assertEquals(2, sr.getProblems().size()),
                
                () -> assertEquals("javac", sr.getProblems().get(0).getCheckName()),
                () -> assertEquals(Severity.ERROR, sr.getProblems().get(0).getSeverity()),
                () -> assertEquals("Mock Message", sr.getProblems().get(0).getMessage()),
                () -> assertFalse(sr.getProblems().get(0).getFile().isPresent()),
                () -> assertFalse(sr.getProblems().get(0).getLine().isPresent()),
                () -> assertFalse(sr.getProblems().get(0).getColumn().isPresent()),
                
                () -> assertEquals("checkstyle", sr.getProblems().get(1).getCheckName()),
                () -> assertEquals(Severity.WARNING, sr.getProblems().get(1).getSeverity()),
                () -> assertEquals("Some message", sr.getProblems().get(1).getMessage()),
                () -> assertFalse(sr.getProblems().get(1).getFile().isPresent()),
                () -> assertFalse(sr.getProblems().get(1).getLine().isPresent()),
                () -> assertFalse(sr.getProblems().get(1).getColumn().isPresent())
            );
        }
        
        @Test
        public void singleMessageFileLineColumn() {
            SubmissionResultDto dto = new SubmissionResultDto();
            dto.setAccepted(true);
            dto.setMessages(Arrays.asList(
                    new CheckMessageDto().checkName("javac").type(TypeEnum.ERROR).message("Mock Message")
                            .file("dir/File.java").line(5).column(3)));
            
            SubmissionResult sr = Submitter.dtoToSubmissionResult(dto);
            
            assertAll(
                () -> assertEquals(1, sr.getProblems().size()),
                () -> assertEquals("javac", sr.getProblems().get(0).getCheckName()),
                () -> assertEquals(Severity.ERROR, sr.getProblems().get(0).getSeverity()),
                () -> assertEquals("Mock Message", sr.getProblems().get(0).getMessage()),
                () -> assertEquals(new File("dir/File.java"), sr.getProblems().get(0).getFile().get()),
                () -> assertEquals(5, sr.getProblems().get(0).getLine().get()),
                () -> assertEquals(3, sr.getProblems().get(0).getColumn().get())
            );
        }
        
    }

}
