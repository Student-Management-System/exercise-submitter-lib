package net.ssehub.teaching.exercise_submitter.lib.replay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;


public class ReplayerTest {
    
    @Test
    public void urlWrongFormatTest() {
        ExerciseSubmitterFactory factory = new ExerciseSubmitterFactory();
        factory.withDummyApiConnection()
                   //wrong svn url Format
                   .withSvnUrl("localhot;5555")
                   .withUsername("username")
                   .withPassword("username")
                   .withCourse("java-wise2021");

        IOException e = assertThrows(IOException.class, () -> {
            ExerciseSubmitterManager manager = factory.build();
            Replayer replayer = manager.getReplayer(new Assignment("005", "Homework03", State.SUBMISSION, true));
            replayer.getVersions();
        });
        
        assertTrue(e.getMessage().equals("Urltype is not supported"));
    }
    
   
}
