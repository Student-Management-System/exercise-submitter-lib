package net.ssehub.teaching.exercise_submitter.lib.replay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ReplayerTest {

    private static final Path TESTDATA = Path.of("src", "test", "resources", "ReplayerTest");
    
    @Nested
    public class FileContentEqual {
        
        @Test
        public void fileAndDirectoryNotEqual() {
            Path directory = TESTDATA.resolve(Path.of("Version1"));
            Path file = TESTDATA.resolve(Path.of("textfile.txt"));
            assertFalse(assertDoesNotThrow(() -> Replayer.fileContentEqual(file, directory)));
        }
        
        @Test
        public void directoryAndFileNotEqual() {
            Path directory = TESTDATA.resolve(Path.of("Version1"));
            Path file = TESTDATA.resolve(Path.of("textfile.txt"));
            assertFalse(assertDoesNotThrow(() -> Replayer.fileContentEqual(directory, file)));
        }
        
        @Test
        public void filesSameContentEqual() {
            Path file1 = TESTDATA.resolve(Path.of("textfile.txt"));
            Path file2 = TESTDATA.resolve(Path.of("identical_textfile.txt"));
            assertTrue(assertDoesNotThrow(() -> Replayer.fileContentEqual(file1, file2)));
        }
        
        @Test
        public void filesDifferentContentNotEqual() {
            Path file1 = TESTDATA.resolve(Path.of("textfile.txt"));
            Path file2 = TESTDATA.resolve(Path.of("different_textfile.txt"));
            assertFalse(assertDoesNotThrow(() -> Replayer.fileContentEqual(file1, file2)));
        }
        
    }
    
    @Nested
    public class PathContentEqual {
        
        @Test
        public void fileAndDirectoryNotEqual() {
            Path directory = TESTDATA.resolve(Path.of("Version1"));
            Path file = TESTDATA.resolve(Path.of("textfile.txt"));
            assertFalse(assertDoesNotThrow(() -> Replayer.directoryContentEqual(file, directory)));
        }
        
        @Test
        public void directoryAndFileNotEqual() {
            Path directory = TESTDATA.resolve(Path.of("Version1"));
            Path file = TESTDATA.resolve(Path.of("textfile.txt"));
            assertFalse(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory, file)));
        }
        
        @Test
        public void differentFilesNotEqual() {
            Path directory1 = TESTDATA.resolve(Path.of("Version1"));
            Path directory2 = TESTDATA.resolve(Path.of("TwoFiles"));
            assertFalse(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
        @Test
        public void sameFilesButDifferentContentNotEqual() {
            Path directory1 = TESTDATA.resolve(Path.of("Version1"));
            Path directory2 = TESTDATA.resolve(Path.of("Version2"));
            assertFalse(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
        @Test
        public void sameFilesEqual() {
            Path directory1 = TESTDATA.resolve(Path.of("Version1"));
            Path directory2 = TESTDATA.resolve(Path.of("Version1"));
            assertTrue(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
        @Test
        public void sameFilesInSubDirEqual() {
            Path directory1 = TESTDATA.resolve(Path.of("SubDirectory"));
            Path directory2 = TESTDATA.resolve(Path.of("SubDirectory"));
            assertTrue(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
        @Test
        public void filesWithDifferentContentInSubDirNotEqual() {
            Path directory1 = TESTDATA.resolve(Path.of("SubDirectory"));
            Path directory2 = TESTDATA.resolve(Path.of("SubDirectoryDifferentContent"));
            assertFalse(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
        @Test
        public void unwantedFilesIgnored() {
            Path directory1 = TESTDATA.resolve(Path.of("EclipseStructure"));
            Path directory2 = TESTDATA.resolve(Path.of("EclipseStructureWithoutEclipseFiles"));
            assertTrue(assertDoesNotThrow(() -> Replayer.directoryContentEqual(directory1, directory2)));
        }
        
    }
    
}
