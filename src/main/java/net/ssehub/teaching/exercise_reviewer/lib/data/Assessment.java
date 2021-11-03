package net.ssehub.teaching.exercise_reviewer.lib.data;

import java.io.File;
import java.util.List;
import java.util.Optional;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto.SeverityEnum;
import net.ssehub.studentmgmt.backend_api.model.UserDto;
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
    private Optional<Group> group;
    private Optional<User> user;
    private Optional<User> creator;
    private Optional<User> lastUpdated;

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
     * @param user the user
     * @return the assessment
     */
    public Assessment withUserId(User user) {
        this.user = Optional.ofNullable(user);
        return this;
    }

    /**
     * With creator id.
     *
     * @param creator the creator
     * @return the assessment
     */
    public Assessment withCreatorId(User creator) {
        this.creator = Optional.ofNullable(creator);
        return this;
    }

    /**
     * With last updated by id.
     *
     * @param lastUpdated the last updated
     * @return the assessment
     */
    public Assessment withLastUpdatedById(User lastUpdated) {
        this.lastUpdated = Optional.ofNullable(lastUpdated);
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
     * Converts markerDto to Problem.
     * @param dto
     * @return Problem
     */
    public static Problem markerDtoToProblem(MarkerDto dto) {
        Problem.Severity sev = dto.getSeverity() == SeverityEnum.ERROR 
                ? Problem.Severity.ERROR : Problem.Severity.WARNING;
        Problem problem = new Problem(dto.getComment(), dto.getComment(), sev);
        problem.setFile(new File(dto.getPath()));
        problem.setLine(dto.getStartLineNumber().toBigInteger().intValueExact());
        problem.setColumn(dto.getStartColumn().toBigInteger().intValueExact());
        return problem;
    }
    /**
     *  Converts userDto to User.
     * @param dto
     * @return User
     */
    public static User userDtoToUser(UserDto dto) {
        User user = null;
        if (dto != null) {
            user = new User(dto.getId(), dto.getUsername(), dto.getDisplayName(), dto.getRole().toString());
        } else {
            user = new User("not available", "not available", "not available", "not available");
        }
        return user;
    }
    /**
     * Gets the assessment id.
     *
     * @return the assessment id
     */
    public String getAssessmentId() {
        return assessmentId;
    }

    /**
     * Gets the achived points.
     *
     * @return the achived points
     */
    public Optional<Integer> getAchivedPoints() {
        return achivedPoints;
    }

    /**
     * Sets the achived points.
     *
     * @param achivedPoints the new achived points
     */
    public void setAchivedPoints(Optional<Integer> achivedPoints) {
        this.achivedPoints = achivedPoints;
    }

    /**
     * Gets the comment.
     *
     * @return the comment
     */
    public Optional<String> getComment() {
        return comment;
    }

    /**
     * Sets the comment.
     *
     * @param comment the new comment
     */
    public void setComment(Optional<String> comment) {
        this.comment = comment;
    }

    /**
     * Gets the group.
     *
     * @return the group
     */
    public Optional<Group> getGroup() {
        return group;
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public Optional<User> getUser() {
        return user;
    }

}
