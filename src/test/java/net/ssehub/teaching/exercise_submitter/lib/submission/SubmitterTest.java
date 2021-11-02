package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
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
        
        @Test
        public void cp1252TextContentConvertedToUtf8() {
            Path submissionDir = TESTDATA.resolve("Encoding");
            
            FileDto result = assertDoesNotThrow(
                () -> Submitter.pathToFileDto(Path.of("cp1252.txt"), submissionDir));
            
            byte[] decodedContent = Base64.getDecoder().decode(result.getContent());
            
            assertEquals("cp 1252\nöäüÖÄÜß\n", new String(decodedContent, StandardCharsets.UTF_8));
        }
        
        @Test
        public void utf8TextFile() {
            Path submissionDir = TESTDATA.resolve("Encoding");
            
            FileDto result = assertDoesNotThrow(
                () -> Submitter.pathToFileDto(Path.of("utf-8.txt"), submissionDir));
            
            byte[] decodedContent = Base64.getDecoder().decode(result.getContent());
            
            assertEquals("UTF-8\nöäüÖÄÜß\n", new String(decodedContent, StandardCharsets.UTF_8));
        }
        
        @Test
        public void nonTextFile() {
            Path submissionDir = TESTDATA.resolve("Encoding");
            
            FileDto result = assertDoesNotThrow(
                () -> Submitter.pathToFileDto(Path.of("non-text.png"), submissionDir));
            
            assertEquals(
                    "iVBORw0KGgoAAAANSUhEUgAAAG4AAAAOCAYAAADOrymhAAABhWlDQ1BJQ0MgcHJvZmlsZQAAKJF9"
                    + "kT1Iw0AcxV9TtUUqDnYoopChOlkQFXGUKhbBQmkrtOpgcumH0KQhSXFxFFwLDn4sVh1cnHV1cBUE"
                    + "wQ8QNzcnRRcp8X9JoUWMB8f9eHfvcfcOEBoVpppd44CqWUY6ERdz+RUx8IogehDCMCISM/VkZiEL"
                    + "z/F1Dx9f72I8y/vcn6NPKZgM8InEs0w3LOJ14ulNS+e8TxxmZUkhPiceM+iCxI9cl11+41xyWOCZ"
                    + "YSObniMOE4ulDpY7mJUNlXiKOKqoGuULOZcVzluc1UqNte7JXxgqaMsZrtMcQgKLSCIFETJq2EAF"
                    + "FmK0aqSYSNN+3MM/6PhT5JLJtQFGjnlUoUJy/OB/8Ltbszg54SaF4kD3i21/jACBXaBZt+3vY9tu"
                    + "ngD+Z+BKa/urDWDmk/R6W4seAf3bwMV1W5P3gMsdIPKkS4bkSH6aQrEIvJ/RN+WBgVugd9XtrbWP"
                    + "0wcgS10t3QAHh8BoibLXPN4d7Ozt3zOt/n4AUhhymn38C2YAAAAGYktHRAD/AP8A/6C9p5MAAAAJ"
                    + "cEhZcwAALiMAAC4jAXilP3YAAAAHdElNRQflCAYMHTBP6ZFYAAAAGXRFWHRDb21tZW50AENyZWF0"
                    + "ZWQgd2l0aCBHSU1QV4EOFwAAAtVJREFUWMPtWDFv2kAU/lL1X2AEOIMz0X+QDoGBM84UZW5UCYaW"
                    + "KLYZs7XKVJGobSQfUtUddQIRBpuh+QdhigcfQ/o/Xgcb18EG3ECiVPW33b2799539969Z28RESHD"
                    + "P4cX2RH85xc3NlV0xdM5/hj2HqzT4dDyEop5FcW8hLYTFQp01fk5YMoNaKoKTVVjsjR4GbGOdv4N"
                    + "3NNrDJpyML5C7a6DvZVqBLzbp4y3x7D3cJ3j0Qco339hUEmSymh8uQbk+7Ol5nt8rgLichejtTOu"
                    + "/Aro25gm8uJoBxGiqUYkMh201V2cTW5w1grkZsoQcgxoeSnUOU63abk9wdFWJRQT/YxklODQ8jOb"
                    + "D+Tg+Fnztgf8OIqfzdj054qvdxOySkZJlhfE0CIOEVAIm0xJJ9NgZNp/xg4REXnEGSPuBUs9i+qh"
                    + "bLY2Ik+F+zqFxahg2Cn3LrLnEWe5wP/AT2aRCLfpVGAWicC2aafRuRqOkZvTtcSnlXtXcAgQq3G1"
                    + "d/twR3PhIWwMJgq2ZwEiV6GVXXhr1RgZjeEQjUBnaVvZwGtnYzA5RK0S8RN92DM/Kx182+nj0vyK"
                    + "wY6OT5Vn2HWs4hCvcZGFty10hX5/vqxA3rCPU27guO/6g8kNcMg2oLWHC9XFRThWoEWkezUFF0cu"
                    + "tJ+dZ9wzLueQfHGQ0dAVFC+vcBCdnrgQAEobiyyO44/Ayd3Qb34cA8XRJhQf4mS4qKES6J67UE4V"
                    + "DFoc1WFzc3w2imUcln0OVBgOei7ccvRp7CF8QYWNAfZRXTcFwywW6J731ucb89NBW+VhszXlLf+J"
                    + "bHZwstPHMRfP785WcFjYnMwaDmExKkQbEM8ikzGqM0Z1pseKuL8+R3UpRwUjXkwTi7bBqCAxqjOL"
                    + "HM+iupSjesoGZaE9zyKT5agw72eKhurvOSxpTmw9OCtfp8+TkWl5yTyiskUcItjKfnllv7wyZBeX"
                    + "YRV+A3n/xj14kQXOAAAAAElFTkSuQmCC", result.getContent());
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
