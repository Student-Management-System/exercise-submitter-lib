package net.ssehub.teaching.exercise_submitter.lib;

import java.util.List;

public class SubmissionResult {

    private boolean accepted;
    
    private List<Problem> problems;
    
    
    public SubmissionResult(boolean accepted, List<Problem> problems) {
        this.accepted = accepted;
        this.problems = problems;
    }
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public List<Problem> getProblems() {
        return problems;
    }
    
}
