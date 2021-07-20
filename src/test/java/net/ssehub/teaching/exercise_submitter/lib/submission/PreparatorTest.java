package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {

                result = preparator.getResult();
                assertTrue(result.isDirectory());
            }

            assertFalse(result.list() == null);
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

                URI SourceDirPath = source.toURI();
                URI ResultDirPath = result.toURI();

                for (File file : source.listFiles()) {

                    URI sourcePath = file.toURI();
                    URI relativeSourcePath = SourceDirPath.relativize(sourcePath);

                    Files.walk(Paths.get(result.getAbsolutePath())).forEach(otherfile -> {
                        boolean first = false;

                        URI resultPath = otherfile.toUri();
                        URI relativeResultPath = ResultDirPath.relativize(resultPath);

                        if (relativeResultPath != relativeSourcePath && first != false) {
                            assertTrue(false);
                        }
                        first = true;
                    });

                }

            }

        });

    }

}
