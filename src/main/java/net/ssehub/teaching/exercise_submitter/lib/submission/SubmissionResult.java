package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.util.List;
import java.util.Objects;

/**
 * The result of a submission to the SVN repository.
 * 
 * @author Adam
 */
public class SubmissionResult {

    private boolean accepted;
    
    private List<Problem> problems;

    /**
     * Creates a new {@link SubmissionResult}.
     * 
     * @param accepted Whether the submission was accepted.
     * @param problems A list of {@link Problem}s in the submission, as detected by the server. Must not be
     *      <code>null</code>. Can be an empty list.
     */
    SubmissionResult(boolean accepted, List<Problem> problems) {
        this.accepted = accepted;
        this.problems = problems;
    }
    
    /**
     * Whether the submission was accepted by the server.
     * 
     * @return Whether the submission was accepted.
     */
    public boolean isAccepted() {
        return accepted;
    }
    
    /**
     * A list of {@link Problem}s that the server detected in the submission.
     * 
     * @return The list of problems. Never <code>null</code>, but may be an empty list.
     */
    public List<Problem> getProblems() {
        return problems;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.accepted, this.problems);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (this.getClass() != obj.getClass())) {
            return false;
        }

        SubmissionResult result = (SubmissionResult) obj;
        if (!this.accepted == result.accepted) {
            return false;
        }
        if (!this.problems.equals(result.problems)) {
            return false;
        }
       
        return true;
    }

    
}
