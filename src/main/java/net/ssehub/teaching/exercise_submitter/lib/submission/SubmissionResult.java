package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.util.List;

public class SubmissionResult {

    private boolean accepted;
    
    private List<Problem> problems;
    
    SubmissionResult(boolean accepted, List<Problem> problems) {
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
