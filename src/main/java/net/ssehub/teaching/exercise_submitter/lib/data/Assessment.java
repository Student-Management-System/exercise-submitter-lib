package net.ssehub.teaching.exercise_submitter.lib.data;

import java.util.Objects;
import java.util.Optional;

/**
 * An assessment of the submission of a group.
 * 
 * @author Adam
 */
public class Assessment {

    private boolean isDraft;
    
    private Optional<Integer> points;
    
    private Optional<String> comment;
    
    /**
     * Creates a new assessment in draft status.
     */
    public Assessment() {
        this.isDraft = true;
        this.points = Optional.empty();
        this.comment = Optional.empty();
    }

    /**
     * Changes the draft status of this assessment.
     * 
     * @param isDraft The desired draft status.
     * 
     * @throws IllegalStateException If no points have been set yet.
     */
    public void setDraft(boolean isDraft) throws IllegalStateException {
        if (!isDraft && points.isEmpty()) {
            throw new IllegalStateException("Can't remove draft status if points are not set");
        }
        this.isDraft = isDraft;
    }
    
    /**
     * Sets the points of this assessment.
     * 
     * @param points The points of this assessment.
     */
    public void setPoints(int points) {
        this.points = Optional.of(points);
    }

    /**
     * Sets the comment of this assessment.
     * 
     * @param comment The comment of this assessment.
     */
    public void setComment(String comment) {
        this.comment = Optional.of(comment);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(comment, isDraft, points);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Assessment)) {
            return false;
        }
        Assessment other = (Assessment) obj;
        return Objects.equals(comment, other.comment) && isDraft == other.isDraft
                && Objects.equals(points, other.points);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Assessment [isDraft=");
        builder.append(isDraft);
        builder.append(", points=");
        builder.append(points);
        builder.append(", comment=");
        builder.append(comment);
        builder.append("]");
        return builder.toString();
    }
    
}
