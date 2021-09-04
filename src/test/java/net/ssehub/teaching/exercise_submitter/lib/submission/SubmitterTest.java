package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.squareup.okhttp.internal.http.CacheStrategy.Factory;

import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager.Credentials;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManagerTest;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class SubmitterTest {
   @Test
    public void noDirectory() {
       
       ExerciseSubmitterFactory factory = new ExerciseSubmitterFactory();
           factory.withDummyApiConnection()
                  .withAuthUrl("localhost:5555")
                  .withMgmtUrl("localhost:5555")
                  .withSvnUrl("localhost:5555")
                  .withUsername("username")
                  .withPassword("username")
                  .withCourse("java-wise2021");
           
           
        assertThrows(IllegalArgumentException.class, () -> {
           ExerciseSubmitterManager manager = factory.build();
           Submitter submitter = manager.getSubmitter(new Assignment("002","Homework02",State.IN_REVIEW,true));
           submitter.submit(new File("main.java"));
        });
    }
    
}
