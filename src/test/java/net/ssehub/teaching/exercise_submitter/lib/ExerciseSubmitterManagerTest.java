package net.ssehub.teaching.exercise_submitter.lib;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.replay.Replayer;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.DummyApiConnection;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;

public class ExerciseSubmitterManagerTest {

    @Test
    public void svnUrlGroupAssignment() {
        Assignment assignment = new Assignment("some-assignment", State.SUBMISSION, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals("http://127.0.0.1/java/abgabe/some-assignment/Group01/", manager.getSvnUrl(assignment));
    }
    
    @Test
    public void svnUrlSingleAssignment() {
        Assignment assignment = new Assignment("some-assignment", State.SUBMISSION, false);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals("http://127.0.0.1/java/abgabe/some-assignment/teststudent1/", manager.getSvnUrl(assignment));
    }
    
    @Test
    public void isSubmittableSimpleStudent() {
        Assignment as1 = new Assignment("some-assignment", State.SUBMISSION, true);
        Assignment as2 = new Assignment("some-assignment", State.IN_REVIEW, true);
        Assignment as3 = new Assignment("some-assignment", State.REVIEWED, true);
        Assignment as4 = new Assignment("some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals(true, manager.isSubmittable(as1));
        assertEquals(false, manager.isSubmittable(as2));
        assertEquals(false, manager.isSubmittable(as3));
        assertEquals(false, manager.isSubmittable(as4));
    }
    
    @Test
    public void isReplayableSimpleStudent() {
        Assignment as1 = new Assignment("some-assignment", State.SUBMISSION, true);
        Assignment as2 = new Assignment("some-assignment", State.IN_REVIEW, true);
        Assignment as3 = new Assignment("some-assignment", State.REVIEWED, true);
        Assignment as4 = new Assignment("some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals(true, manager.isReplayable(as1));
        assertEquals(false, manager.isReplayable(as2));
        assertEquals(true, manager.isReplayable(as3));
        assertEquals(false, manager.isReplayable(as4));
    }
    
    @Test
    public void getSubmitterNonSubmittableThrows() {
        Assignment assignment = new Assignment("some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertThrows(IllegalArgumentException.class, () -> manager.getSubmitter(assignment));
    }
    
    @Test
    public void getSubmitterSubmittableDoesNotThrow() {
        Assignment assignment = new Assignment("some-assignment", State.SUBMISSION, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        Submitter submitter = assertDoesNotThrow(() -> manager.getSubmitter(assignment));
        assertNotNull(submitter);
    }
    
    @Test
    public void getReplayerNonReplaybleThrows() {
        Assignment assignment = new Assignment("some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertThrows(IllegalArgumentException.class, () -> manager.getReplayer(assignment));
    }
    
    @Test
    public void getReplayerReplayableDoesNotThrow() {
        Assignment assignment = new Assignment("some-assignment", State.REVIEWED, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        Replayer replayer = assertDoesNotThrow(() -> manager.getReplayer(assignment));
        assertNotNull(replayer);
    }
    
    @Test
    public void getAllAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals(DummyApiConnection.DUMMY_ASSIGNMENTS, assertDoesNotThrow(() -> manager.getAllAssignments()));
    }
    
    @Test
    public void getSubmittableAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals(Arrays.asList(
                new Assignment("Test02", State.SUBMISSION, false),
                new Assignment("Homework03", State.SUBMISSION, true)
        ), assertDoesNotThrow(() -> manager.getAllSubmittableAssignments()));
    }
    
    @Test
    public void getReplayableAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterManager("teststudent1", "pw", "java", "wise2021"));
        
        assertEquals(Arrays.asList(
                new Assignment("Homework01", State.REVIEWED, true),
                new Assignment("Test02", State.SUBMISSION, false),
                new Assignment("Homework03", State.SUBMISSION, true)
                ), assertDoesNotThrow(() -> manager.getAllReplayableAssignments()));
    }
    
}
