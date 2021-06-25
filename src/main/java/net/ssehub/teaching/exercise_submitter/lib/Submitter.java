package net.ssehub.teaching.exercise_submitter.lib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.lib.Assignment.State;

public class Submitter {

    private static final List<Assignment> DUMMY_ASSIGNMENTS = Arrays.asList(
            new Assignment("Homework01", State.REVIEWED, true),
            new Assignment("Homework02", State.IN_REVIEW, true),
            new Assignment("Test01", State.IN_REVIEW, false),
            new Assignment("Test02", State.SUBMISSION, false),
            new Assignment("Homework03", State.SUBMISSION, true)
            );
    
    public Submitter(String user, char[] password) {
    }
    
    public List<Assignment> getAllAssignments() {
        return DUMMY_ASSIGNMENTS;
    }
    
    public List<Assignment> getAssignments(Assignment.State state) {
        return DUMMY_ASSIGNMENTS.stream()
                .filter(assignment -> assignment.getState() == state)
                .collect(Collectors.toList());
    }
    
    /**
     * Submits the given directory to the given assignment.
     * 
     * @param directory The directory that contains the solution to be submitted.
     * @param assignment The assignment for which the solution should be submitted.
     * 
     * @return The result of the submission.
     * 
     * @throws SubmissionException If the submission fails.
     * @throws IllegalArgumentException If the given directory is not a directory.
     */
    public SubmissionResult submit(File directory, Assignment assignment)
            throws SubmissionException, IllegalArgumentException {
        
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        
        if (assignment.getState() != State.SUBMISSION) {
            throw new SubmissionException("Assignment " + assignment.getName() + " is not in submission state");
        }
        
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
        }
        
        Optional<File> randomJavaFile;
        try {
            randomJavaFile = Files.walk(directory.toPath())
                    .filter(Files::isRegularFile)
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
            p1.setLine(10);
            
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
