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
        Assignment assignment = DummyApiConnection.DUMMY_ASSIGNMENTS.get(4);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals("http://127.0.0.1/java/abgabe/Homework03/Group01/", assertDoesNotThrow(() -> manager.getSvnUrl(assignment)));
    }
    
    @Test
    public void svnUrlSingleAssignment() {
        Assignment assignment = DummyApiConnection.DUMMY_ASSIGNMENTS.get(3);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals("http://127.0.0.1/java/abgabe/Test02/teststudent1/", assertDoesNotThrow(() -> manager.getSvnUrl(assignment)));
    }
    
    @Test
    public void isSubmittableSimpleStudent() {
        Assignment as1 = new Assignment("001", "some-assignment", State.SUBMISSION, true);
        Assignment as2 = new Assignment("001", "some-assignment", State.IN_REVIEW, true);
        Assignment as3 = new Assignment("001", "some-assignment", State.REVIEWED, true);
        Assignment as4 = new Assignment("001", "some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals(true, manager.isSubmittable(as1));
        assertEquals(false, manager.isSubmittable(as2));
        assertEquals(false, manager.isSubmittable(as3));
        assertEquals(false, manager.isSubmittable(as4));
    }
    
    @Test
    public void isReplayableSimpleStudent() {
        Assignment as1 = new Assignment("001", "some-assignment", State.SUBMISSION, true);
        Assignment as2 = new Assignment("001", "some-assignment", State.IN_REVIEW, true);
        Assignment as3 = new Assignment("001", "some-assignment", State.REVIEWED, true);
        Assignment as4 = new Assignment("001", "some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals(true, manager.isReplayable(as1));
        assertEquals(false, manager.isReplayable(as2));
        assertEquals(true, manager.isReplayable(as3));
        assertEquals(false, manager.isReplayable(as4));
    }
    
    @Test
    public void getSubmitterNonSubmittableThrows() {
        Assignment assignment = new Assignment("001", "some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertThrows(IllegalArgumentException.class, () -> manager.getSubmitter(assignment));
    }
    
    @Test
    public void getSubmitterSubmittableDoesNotThrow() {
        Assignment assignment = DummyApiConnection.DUMMY_ASSIGNMENTS.get(4);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        Submitter submitter = assertDoesNotThrow(() -> manager.getSubmitter(assignment));
        assertNotNull(submitter);
    }
    
    @Test
    public void getReplayerNonReplaybleThrows() {
        Assignment assignment = new Assignment("001", "some-assignment", State.INVISIBLE, true);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertThrows(IllegalArgumentException.class, () -> manager.getReplayer(assignment));
    }
    
    @Test
    public void getReplayerReplayableDoesNotThrow() {
        Assignment assignment = DummyApiConnection.DUMMY_ASSIGNMENTS.get(0);
        
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        Replayer replayer = assertDoesNotThrow(() -> manager.getReplayer(assignment));
        assertNotNull(replayer);
    }
    
    @Test
    public void getAllAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals(DummyApiConnection.DUMMY_ASSIGNMENTS, assertDoesNotThrow(() -> manager.getAllAssignments()));
    }
    
    @Test
    public void getSubmittableAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals(Arrays.asList(
                new Assignment("004", "Test02", State.SUBMISSION, false),
                new Assignment("005", "Homework03", State.SUBMISSION, true)
        ), assertDoesNotThrow(() -> manager.getAllSubmittableAssignments()));
    }
    
    @Test
    public void getReplayableAssignmentsDummyData() {
        ExerciseSubmitterManager manager = assertDoesNotThrow(() -> new ExerciseSubmitterFactory()
                .withUsername("teststudent1")
                .withPassword("teststudent1")
                .withCourse("java-wise2021")
                .withDummyApiConnection()
                .build());
        
        assertEquals(Arrays.asList(
                new Assignment("001", "Homework01", State.REVIEWED, true),
                new Assignment("004", "Test02", State.SUBMISSION, false),
                new Assignment("005", "Homework03", State.SUBMISSION, true)
                ), assertDoesNotThrow(() -> manager.getAllReplayableAssignments()));
    }
    
}
