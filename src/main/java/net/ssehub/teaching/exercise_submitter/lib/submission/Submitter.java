package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;

/**
 * Submits solutions to a given SVN exercise submission location.
 * 
 * @author Adam
 */
public class Submitter {

    private String url;
    
    /**
     * Creates a new submitter for the given SVN location.
     * 
     * @param url The URL of the homework folder that should be submitted to.
     */
    public Submitter(String url) {
        this.url = url;
    }
    
    /**
     * Submits the given directory.
     * 
     * @param directory The directory that contains the solution to be submitted.
     * 
     * @return The result of the submission.
     * 
     * @throws SubmissionException If the submission fails.
     * @throws IllegalArgumentException If the given directory is not a directory.
     */
    public SubmissionResult submit(File directory)
            throws SubmissionException, IllegalArgumentException {
        
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
        }
        
        Optional<File> randomJavaFile;
        try {
            randomJavaFile = Files.walk(directory.toPath())
                    .filter(Files::isRegularFile)
                    .map(p -> directory.toPath().relativize(p))
                    .map(Path::toFile)
                    .filter(f -> f.getName().endsWith(".java"))
                    .findFirst();
        } catch (IOException e) {
            throw new SubmissionException(e);
        }
        
        SubmissionResult result;
        
        if (randomJavaFile.isPresent()) {
            List<Problem> problems = new LinkedList<>();
            
            Problem p1 = new Problem("javac", "Missing ';'", Severity.ERROR);
            p1.setFile(randomJavaFile.get());
            p1.setLine(5);
            p1.setColumn(10);
            
            Problem p2 = new Problem("checkstyle", "Some checkstyle message", Severity.WARNING);
            p2.setFile(randomJavaFile.get());
            p2.setLine(10);
            
            problems.add(p1);
            problems.add(p2);
            
            result = new SubmissionResult(true, new LinkedList<>(problems));
            
        } else {
            Problem p1 = new Problem("javac", "No Java files found", Severity.ERROR);
            
            result = new SubmissionResult(false, Arrays.asList(p1));
        }
        
        return result;
    }
    
}
