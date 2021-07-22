package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class PreparatorTest {

    private final static File TESTDATA = new File("src/test/resources/PreparatorTest");

    @Test
    public void throwsIfDirDoesntExist() {
        IOException ex = assertThrows(IOException.class, () -> new Preparator(new File("doesnt_exist")));
        assertEquals("doesnt_exist is not a directory", ex.getMessage());
    }

    @Test
    public void throwsIfDirIsFile() {
        IOException ex = assertThrows(IOException.class, () -> new Preparator(new File(TESTDATA, "file.txt")));
        assertEquals("file.txt is not a directory", ex.getMessage());
    }

    @Test
    public void prepareEmptyFolder() {
        File source = new File(TESTDATA, "emptyDir");
        source.mkdir();
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator(source)) {
                File result = preparator.getResult();

                assertTrue(result.isDirectory());
                assertEquals(0, result.listFiles().length);
                assertNotEquals(result, source);
            }
        });
    }

    @Test
    public void clearsCopiedEmptyDir() {
        File source = new File(TESTDATA, "emptyDir");
        source.mkdir();
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {

                result = preparator.getResult();
                assertTrue(result.isDirectory());
            }

            assertFalse(result.exists());
        });

    }

    @Test
    public void copiedFilesinEmptyDir() {
        File source = new File(TESTDATA, "notEmptyDir");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {

                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                assertTrue(new File(result, "file.txt").isFile());
                assertEquals(1, result.listFiles().length);
            }
        });

    }

    @Test
    public void copiedAllFilesAndSubDirsinEmptyDir() {
        File source = new File(TESTDATA, "notEmptyDirWithSubDir");
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {

                result = preparator.getResult();
                assertTrue(result.isDirectory());

                // exactly one sub directory
                File subDir = new File(result, "notEmptyDir");
                assertTrue(subDir.isDirectory());
                assertEquals(1, result.listFiles().length);
                
                // exactly one file in sub directory
                assertTrue(new File(subDir, "file.txt").isFile());
                assertEquals(1, subDir.listFiles().length);
            }

        });

    }

}
