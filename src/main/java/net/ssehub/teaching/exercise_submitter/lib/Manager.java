package net.ssehub.teaching.exercise_submitter.lib;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.lib.Assignment.State;

public class Manager {
    
    private static final List<Assignment> DUMMY_ASSIGNMENTS = Arrays.asList(
            new Assignment("Homework01", State.REVIEWED, true),
            new Assignment("Homework02", State.IN_REVIEW, true),
            new Assignment("Test01", State.IN_REVIEW, false),
            new Assignment("Test02", State.SUBMISSION, false),
            new Assignment("Homework03", State.SUBMISSION, true)
            );
    
    public Manager(String user, char[] password) {
    }
    
    public List<Assignment> getAllAssignments() {
        return DUMMY_ASSIGNMENTS;
    }
    
    public List<Assignment> getAssignments(Assignment.State state) {
        return DUMMY_ASSIGNMENTS.stream()
                .filter(assignment -> assignment.getState() == state)
                .collect(Collectors.toList());
    }
    
    public Submitter getSubmitter(Assignment assignment) throws IllegalArgumentException {
        return new Submitter(assignment);
    }
    
    public Replayer getReplayer(Assignment assignment) throws IllegalArgumentException {
        return new Replayer(assignment);
    }
    
}
