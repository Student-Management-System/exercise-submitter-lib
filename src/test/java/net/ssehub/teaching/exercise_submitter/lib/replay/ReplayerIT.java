package net.ssehub.teaching.exercise_submitter.lib.replay;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterFactory;
import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.replay.Replayer.Version;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.ApiException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.GroupNotFoundException;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.NetworkException;
import net.ssehub.teaching.exercise_submitter.lib.submission.SubmissionException;
import net.ssehub.teaching.exercise_submitter.lib.submission.SubmissionResult;
import net.ssehub.teaching.exercise_submitter.lib.submission.Submitter;

public class ReplayerIT {

    private static StuMgmtDocker docker;

    private static final File TESTDATA = new File("src/test/resources/ReplayerTest");

    private static String courseId = null;

    private static Map<String, String> assignmentids = new HashMap<String, String>();

    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("svn", "abcdefgh");
        docker.createUser("adam", "123456");
        docker.createUser("student1", "123456");
        docker.createUser("student2", "123456");
        docker.createUser("student3", "123456");
        docker.createUser("student4", "123456");

        courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam", "svn");

        docker.enrollStudent(courseId, "student1");
        docker.enrollStudent(courseId, "student2");
        docker.enrollStudent(courseId, "student3");
        docker.enrollStudent(courseId, "student4");

        docker.createGroup(courseId, "JP001", "student1", "student3");
        docker.createGroup(courseId, "JP002", "student2", "student4");

        assignmentids.put("Homework01",
                docker.createAssignment(courseId, "Homework01", AssignmentState.INVISIBLE, Collaboration.GROUP));

        // start svn

        docker.startSvn(courseId, "svn");

        docker.changeAssignmentState(courseId, assignmentids.get("Homework01"), AssignmentState.SUBMISSION);

    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }

    public static void submit(File file, Assignment assignment) throws IllegalArgumentException, SubmissionException,
            NetworkException, GroupNotFoundException, ApiException {
        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");

        ExerciseSubmitterManager manager = fackto.build();

        Submitter submitter = manager.getSubmitter(assignment);
        // check result
        submitter.submit(file);
    }

    @Test
    public void getVersionListTest() {
        // create version history
        File maindir = new File(TESTDATA, "VersionFiles");
        Assignment assignment = new Assignment(assignmentids.get("Homework01"), "Homework01",
                Assignment.State.SUBMISSION, true);

        for (int i = 1; i <= 2; i++) {
            File dir = new File(maindir, "Version" + i);
            assertDoesNotThrow(() -> { 
                submit(dir, assignment);
                Thread.sleep(4000);
              //cause svn doesnt like fast changes
            }
            );
            
           
        }

        ExerciseSubmitterFactory fackto = new ExerciseSubmitterFactory();
        fackto.withAuthUrl(docker.getAuthUrl());
        fackto.withMgmtUrl(docker.getStuMgmtUrl());
        fackto.withSvnUrl(docker.getSvnUrl());
        fackto.withUsername("student1");
        fackto.withPassword("123456");
        fackto.withCourse("java-wise2021");

        assertDoesNotThrow(() -> {

            ExerciseSubmitterManager manager = fackto.build();

            Replayer replayer = manager.getReplayer(assignment);
            List<Version> versions = replayer.getVersions();
            
            assertAll(
                ()->assertTrue(versions.size() == 3),
                ()->assertTrue(versions.get(0).getAuthor().equals("student1")),
                ()-> assertTrue(versions.get(1).getAuthor().equals("student1"))
            );

        });

    }

}
