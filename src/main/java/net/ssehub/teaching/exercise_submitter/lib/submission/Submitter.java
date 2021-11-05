package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;
import net.ssehub.teaching.exercise_submitter.server.api.ApiClient;
import net.ssehub.teaching.exercise_submitter.server.api.ApiException;
import net.ssehub.teaching.exercise_submitter.server.api.api.SubmissionApi;
import net.ssehub.teaching.exercise_submitter.server.api.model.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.api.model.CheckMessageDto.TypeEnum;
import net.ssehub.teaching.exercise_submitter.server.api.model.FileDto;
import net.ssehub.teaching.exercise_submitter.server.api.model.SubmissionResultDto;

/**
 * Submits solutions to a given assignment.
 *
 * @author Adam
 * @author Lukas
 */
public class Submitter {

    /**
     * A {@link Predicate} that filters all unwanted files from a submission directory.
     * <p>
     * Parameter should be the relative path of the file inside the submission directory to check.
     * <p>
     * Returns <code>false</code> if the file is unwanted and should be filtered, <code>true</code> otherwise.
     */
    public static final Predicate<Path> WANTED_FILES;
    
    static {
        Predicate<Path> notClassFile = p -> !p.getFileName().toString().endsWith(".class");
        
        Predicate<Path> notClasspathFile = p -> !p.equals(Path.of(".classpath"));
        Predicate<Path> notProjectFile = p -> !p.equals(Path.of(".project"));
        Predicate<Path> notCheckstyleFile = p -> !p.equals(Path.of(".checkstyle"));
        Predicate<Path> notInSettingsDir = p -> !p.startsWith(".settings");
        
        WANTED_FILES = notClassFile
                .and(notClasspathFile)
                .and(notProjectFile)
                .and(notCheckstyleFile)
                .and(notInSettingsDir);
    }
    
    private String courseId;
    
    private String assignmentName;
    
    private String groupName;
    
    private SubmissionApi api;
    
    /**
     * Creates a new submitter for the given assignment.
     * 
     * @param baseUrl The URL of the exercise-submitter-server API.
     * @param courseId the ID of the course to submit to.
     * @param assignmentName The name of the assignment to submit to.
     * @param groupName The name of the group to submit to. May be the students name for non-group assignments.
     * @param token The token to authenticate with. This is the same as used for the student management system. 
     *
     */
    public Submitter(String baseUrl, String courseId, String assignmentName, String groupName, String token) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        client.setAccessToken(token);
        this.api = new SubmissionApi(client);
        
        this.courseId = courseId;
        this.assignmentName = assignmentName;
        this.groupName = groupName;
    }

    /**
     * Converts the given file to a {@link FileDto} for submission.
     * <p>
     * Package visibility for test cases.
     * 
     * @param file The file to submit, relative to the submissionDirectory.
     * @param submissionDirectory The base submission directory.
     * 
     * @return The {@link FileDto} with correct content and path.
     * 
     * @throws UncheckedIOException If reading the file content fails.
     */
    static FileDto pathToFileDto(Path file, Path submissionDirectory) throws UncheckedIOException {
        try {
            FileDto result = new FileDto();
            result.setPath(file.toString().replace('\\', '/'));
            
            Path absoluteFile = submissionDirectory.resolve(file);
            
            byte[] rawContent;
            
            String contentType = Files.probeContentType(absoluteFile);
            if (contentType != null && contentType.startsWith("text")
                    && !EncodingUtils.checkEncoding(absoluteFile, StandardCharsets.UTF_8)) {
                
                rawContent = EncodingUtils.getUtf8ConvertedContent(absoluteFile);
                
            } else {
                rawContent = Files.readAllBytes(absoluteFile);
            }
            
            String base64Content = Base64.getEncoder().encodeToString(rawContent);
            result.setContent(base64Content);
            
            return result;
            
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    /**
     * Converts a {@link SubmissionResultDto} with {@link CheckMessageDto}s
     * to {@link SubmissionResult} with {@link Problem}s.
     * <p>
     * Package visibility for test cases.
     * 
     * @param dto The DTO to convert.
     * 
     * @return The converted {@link SubmissionResult} with all messages.
     */
    static SubmissionResult dtoToSubmissionResult(SubmissionResultDto dto) {
        var problems = dto.getMessages().stream()
            .map(message -> {
                Problem problem = new Problem(message.getCheckName(), message.getMessage(),
                        message.getType() == TypeEnum.WARNING ? Severity.WARNING : Severity.ERROR);
                
                if (message.getFile() != null) {
                    problem.setFile(new File(message.getFile()));
                }
                if (message.getLine() != null) {
                    problem.setLine(message.getLine());
                }
                if (message.getColumn() != null) {
                    problem.setColumn(message.getColumn());
                }
                return problem;
            })
            .collect(Collectors.toList());
        
        return new SubmissionResult(dto.isAccepted(), problems);
    }
    
    /**
     * Submits the given directory.
     *
     * @param directory The directory that contains the solution to be submitted.
     *
     * @return The result of the submission.
     *
     * @throws SubmissionException      If the submission fails.
     * @throws IllegalArgumentException If the given directory is not a directory.
     */
    public SubmissionResult submit(File directory)
            throws SubmissionException, IllegalArgumentException {

        Path submissionDir = directory.toPath();
        
        if (!Files.isDirectory(submissionDir)) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        
        List<FileDto> files;
        try {
            files = Files.walk(submissionDir)
                    .filter(p -> Files.isRegularFile(p))
                    .map(p -> submissionDir.relativize(p))
                    
                    .filter(WANTED_FILES)
                    .map(filepath -> pathToFileDto(filepath, submissionDir))
                    
                    .collect(Collectors.toList());
            
        } catch (IOException e) {
            throw new SubmissionException("Failed to list submission directory content", e.getCause());
            
        } catch (UncheckedIOException e) {
            throw new SubmissionException("Failed to read file content", e.getCause());
        }

        SubmissionResultDto dto;
        try {
            dto = api.submit(courseId, assignmentName, groupName, files);
        } catch (ApiException e) {
            throw new SubmissionException("Failed to upload submission", e);
        }
        
        return dtoToSubmissionResult(dto);
    }
    
}
