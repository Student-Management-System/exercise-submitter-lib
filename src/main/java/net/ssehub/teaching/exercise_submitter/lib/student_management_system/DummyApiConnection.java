package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;

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
    
    @Override
    public void login(String username, String password) throws NetworkException, AuthenticationException {
        
    }

    @Override
    public Course getCourse(String name, String semester)
            throws NetworkException, AuthenticationException, NoSuchElementException {
        
        if (!name.equals("java") || !semester.equals("wise2021")) {
            throw new NoSuchElementException("No course " + name + " in semester " + semester);
        }
        
        return course;
    }

    @Override
    public List<Assignment> getAssignments(Course course) throws NetworkException, AuthenticationException {
        if (course != this.course) {
            throw new IllegalArgumentException("Invalid course " + course);
        }
        return Collections.unmodifiableList(DUMMY_ASSIGNMENTS);
    }
    
    @Override
    public String getGroupName(Course course, Assignment assignment)
            throws NetworkException, AuthenticationException, UserNotInCourseException {
        return "Group01";
    }

}
