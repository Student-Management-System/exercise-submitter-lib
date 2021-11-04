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
    
    private Optional<String> managementId;
    
    private Optional<Double> points;
    
    private Optional<String> comment;
    
    /**
     * Creates a new assessment in draft status.
     */
    public Assessment() {
        this.isDraft = true;
        this.managementId = Optional.empty();
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
     * Returns if this assessment is a draft.
     * 
     * @return The draft status of this assessment.
     */
    public boolean isDraft() {
        return isDraft;
    }
    
    /**
     * Sets the points of this assessment.
     * 
     * @param points The points of this assessment.
     */
    public void setPoints(double points) {
        this.points = Optional.of(points);
    }
    
    /**
     * Returns the points awarded by this assessment.
     * 
     * @return The points of this assessment.
     */
    public Optional<Double> getPoints() {
        return points;
    }

    /**
     * Sets the comment of this assessment.
     * 
     * @param comment The comment of this assessment.
     */
    public void setComment(String comment) {
        this.comment = Optional.of(comment);
    }

    /**
     * Returns the comment of this assignment.
     * 
     * @return The comment of this assignment.
     */
    public Optional<String> getComment() {
        return comment;
    }
    
    /**
     * Sets the ID of this assessment in the student management system.
     * 
     * @param managementId The ID of this assessment.
     */
    public void setManagementId(String managementId) {
        this.managementId = Optional.of(managementId);
    }
    
    /**
     * Returns the ID of this assessment in the student management system.
     * 
     * @return The ID; if this is {@link Optional#empty()}, this assessment does not exist in the management system yet.
     */
    public Optional<String> getManagementId() {
        return managementId;
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
