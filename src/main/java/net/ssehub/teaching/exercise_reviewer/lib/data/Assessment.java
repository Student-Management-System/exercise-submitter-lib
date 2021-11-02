package net.ssehub.teaching.exercise_reviewer.lib.data;

import java.util.List;
import java.util.Optional;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem;

/**
 * Creates an assessment from AssessmentDto.
 *
 * @author lukas
 *
 */
public class Assessment {
    private String assessmentId;
    private Assignment assignment;
    private boolean isDraft;

    private Optional<Integer> achivedPoints;
    private Optional<String> comment;
    private Optional<String> groupId;
    private Optional<String> userId;
    private Optional<String> creatorId;
    private Optional<String> lastUpdatedById;

    private Optional<List<Problem>> problems;

    /**
     * Crate a new instance from assessment.
     *
     * @param assessmentId the assessment id
     * @param assignment the assignment
     * @param isDraft the is draft
     */
    public Assessment(String assessmentId, Assignment assignment, boolean isDraft) {
        this.assessmentId = assessmentId;
        this.assignment = assignment;
        this.isDraft = isDraft;
    }

    /**
     * With achieved points.
     *
     * @param points the points
     * @return the assessment
     */
    public Assessment withAchievedPoints(int points) {
        this.achivedPoints = Optional.ofNullable(points);
        return this;
    }

    /**
     * With comment.
     *
     * @param comment the comment
     * @return the assessment
     */
    public Assessment withComment(String comment) {
        this.comment = Optional.ofNullable(comment);
        return this;
    }

    /**
     * With problems.
     *
     * @param problems the problems
     * @return the assessment
     */
    public Assessment withProblems(List<Problem> problems) {
        this.problems = Optional.ofNullable(problems);
        return this;
    }

    /**
     * With user id.
     *
     * @param userId the user id
     * @return the assessment
     */
    public Assessment withUserId(String userId) {
        this.userId = Optional.ofNullable(userId);
        return this;
    }

    /**
     * With creator id.
     *
     * @param creatorId the creator id
     * @return the assessment
     */
    public Assessment withCreatorId(String creatorId) {
        this.creatorId = Optional.ofNullable(creatorId);
        return this;
    }

    /**
     * With last updated by id.
     *
     * @param lastUpdatedById the last updated by id
     * @return the assessment
     */
    public Assessment withLastUpdatedById(String lastUpdatedById) {
        this.lastUpdatedById = Optional.ofNullable(lastUpdatedById);
        return this;
    }

    /**
     * Assignment dto to assignment.
     *
     * @param dto the dto
     * @return the assignment
     */
    public static Assignment assignmentDtoToAssignment(AssignmentDto dto) {
        State state = null;
        switch (dto.getState()) {
        case EVALUATED:
            state = State.REVIEWED;
            break;
        case INVISIBLE:
            state = State.INVISIBLE;
            break;
        case IN_PROGRESS:
            state = State.SUBMISSION;
            break;
        case IN_REVIEW:
            state = State.IN_REVIEW;
            break;
        case CLOSED:
            state = State.CLOSED;
            break;
        default:
            state = State.INVISIBLE;
            break;
        }

        boolean groupwork = dto.getCollaboration() != CollaborationEnum.SINGLE ? true : false;

        return new Assignment(dto.getId(), dto.getName(), state, groupwork);
    }

    /**
     * Gets the assessment id.
     *
     * @return the assessment id
     */
    public String getAssessmentId() {
        return assessmentId;
    }

}
