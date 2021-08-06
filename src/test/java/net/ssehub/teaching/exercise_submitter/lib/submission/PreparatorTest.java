package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
    public void clearsCopiedNotEmptyDir() {
        File source = new File(TESTDATA, "notEmptyDir");

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
    
    @Test
    public void iso88591ToUtf8() {
        File source = new File(TESTDATA, "encodings");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {

                result = preparator.getResult();
                assertTrue(result.isDirectory());

                File utf8file = new File(result, "ISO-8859-1.txt");
                
                try (FileInputStream fs = new FileInputStream(utf8file)) {
                    byte[] bytes = fs.readAllBytes();
                    
                    //throws exception if charset is not utf-8
                    String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
                    assertEquals("ISO-8859-1\nöäüÖÄÜß\n", content);
                }
               
            }

        });
    }
    
    @Test
    public void cp1252ToUtf8() {
        File source = new File(TESTDATA, "encodings");
        
        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {
                
                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                File utf8file = new File(result, "cp1252.txt");
                
                try (FileInputStream fs = new FileInputStream(utf8file)) {
                    byte[] bytes = fs.readAllBytes();
                    
                    //throws exception if charset is not utf-8
                    String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
                    assertEquals("cp 1252\nöäüÖÄÜß\n", content);
                }
                
            }
            
        });
    }
    
    @Test
    public void notConvertingNonTextFile() {
        File source = new File(TESTDATA, "nonText");
        
        File originalPicture = new File(source, "picture.png");
        
        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator(source)) {
                
                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                File copiedPicture = new File(result, "picture.png");
                
                byte[] originalBytes;
                byte[] copiedBytes;
                try (FileInputStream original = new FileInputStream(originalPicture);
                        FileInputStream copied = new FileInputStream(copiedPicture)) {
                    originalBytes = original.readAllBytes();
                    copiedBytes = copied.readAllBytes();
                }
                
                assertArrayEquals(originalBytes, copiedBytes);
            }
        });
    }
    
    @Test
    public void checkEncoding() throws IOException {
        File encodingsDir = new File(TESTDATA, "encodings");
        File utf8 = new File(encodingsDir, "utf-8.txt");
        File cp1252 = new File(encodingsDir, "cp1252.txt");
        File iso88591 = new File(encodingsDir, "ISO-8859-1.txt");
        // utf-8.long.txt has a three-byte UTF-8 character at the 1024 byte mark
        // this covers the edge case, that the input buffer has remaining bytes after a decoding pass
        File utf8Long = new File(encodingsDir, "utf-8.long.txt");
        
        assertAll(
                () -> assertTrue(assertDoesNotThrow(() -> Preparator.checkEncoding(utf8.toPath(), StandardCharsets.UTF_8))),
                () -> assertTrue(assertDoesNotThrow(() -> Preparator.checkEncoding(cp1252.toPath(), Charset.forName("cp1252")))),
                () -> assertTrue(assertDoesNotThrow(() -> Preparator.checkEncoding(iso88591.toPath(), StandardCharsets.ISO_8859_1))),
                () -> assertTrue(assertDoesNotThrow(() -> Preparator.checkEncoding(utf8Long.toPath(), StandardCharsets.UTF_8))),
                
                () -> assertFalse(assertDoesNotThrow(() -> Preparator.checkEncoding(cp1252.toPath(), StandardCharsets.UTF_8))),
                () -> assertFalse(assertDoesNotThrow(() -> Preparator.checkEncoding(iso88591.toPath(), StandardCharsets.UTF_8)))
        );
        
    }

}
