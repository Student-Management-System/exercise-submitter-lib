package net.ssehub.teaching.exercise_submitter.lib;

import java.util.List;

public class SubmissionResult {

    private boolean success;
    
    private List<Problem> problems;
    
    
    public SubmissionResult(boolean success, List<Problem> problems) {
        this.success = success;
        this.problems = problems;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<Problem> getProblems() {
        return problems;
    }
    
}
