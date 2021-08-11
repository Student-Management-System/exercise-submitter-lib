package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

/**
 * A dummy implementation with fixed data.
 * 
 * @author Adam
 */
public class DummyApiConnection implements IApiConnection {

    public static final List<Assignment> DUMMY_ASSIGNMENTS = Arrays.asList(
            new Assignment("001", "Homework01", State.REVIEWED, true),
            new Assignment("002", "Homework02", State.IN_REVIEW, true),
            new Assignment("003", "Test01", State.IN_REVIEW, false),
            new Assignment("004", "Test02", State.SUBMISSION, false),
            new Assignment("005", "Homework03", State.SUBMISSION, true)
            );
    
    private Course course = new Course("Programmierpraktikum I: Java", "java-wise2021");
    
    private boolean loggedIn;
    
    @Override
    public void login(String username, String password) throws NetworkException, AuthenticationException, ApiException {
        if (!username.equals(password)) {
            throw new AuthenticationException("Invalid credentials (username must equal password)");
        }
        loggedIn = true;
    }

    @Override
    public Course getCourse(String courseId)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        if (!loggedIn) {
            throw new AuthenticationException("Not logged in");
        }
        
        if (!courseId.equals("java-wise2021")) {
            throw new UserNotInCourseException("No course " + courseId);
        }
        
        return course;
    }

    @Override
    public List<Assignment> getAssignments(Course course)
            throws NetworkException, AuthenticationException, UserNotInCourseException, ApiException {
        
        if (!loggedIn) {
            throw new AuthenticationException("Not logged in");
        }
        
        if (course != this.course) {
            throw new UserNotInCourseException("Invalid course " + course);
        }
        
        return Collections.unmodifiableList(DUMMY_ASSIGNMENTS);
    }
    
    @Override
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException, GroupNotFoundException,
            ApiException {
        
        if (!loggedIn) {
            throw new AuthenticationException("Not logged in");
        }
        
        if (course != this.course) {
            throw new UserNotInCourseException("Invalid course " + course);
        }
        
        if (!DUMMY_ASSIGNMENTS.contains(assignment)) {
            throw new GroupNotFoundException("Assignment " + assignment.getName() + " not found");
        }
        
        if (!assignment.isGroupWork()) {
            throw new GroupNotFoundException("Assignment " + assignment.getName() + " is not group work");
        }
        
        return "Group01";
    }

}
