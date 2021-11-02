package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EncodingUtilsTest {

    private static final Path TESTDATA = Path.of("src", "test", "resources", "EncodingUtilsTest");
    
    @Nested
    public class GetUtf8ConvertedContent {
        
        @Test
        public void cp1252FileIsConverted() {
            Path file = TESTDATA.resolve("cp1252.txt");
            
            byte[] result = assertDoesNotThrow(() -> EncodingUtils.getUtf8ConvertedContent(file));
            
            assertArrayEquals("cp 1252\nöäüÖÄÜß\n".getBytes(StandardCharsets.UTF_8), result);
        }
        
        @Test
        public void iso88591FileIsConverted() {
            Path file = TESTDATA.resolve("ISO-8859-1.txt");
            
            byte[] result = assertDoesNotThrow(() -> EncodingUtils.getUtf8ConvertedContent(file));
            
            assertArrayEquals("ISO-8859-1\nöäüÖÄÜß\n".getBytes(StandardCharsets.UTF_8), result);
        }
        
        @Test
        public void invalidFileNotConverted() {
            Path file = TESTDATA.resolve("invalid.txt");
            
            byte[] result = assertDoesNotThrow(() -> EncodingUtils.getUtf8ConvertedContent(file));
            
            assertThrows(MalformedInputException.class,
                    () ->StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(result)));
        }
        
    }
    
    @Nested
    public class CheckEncoding {
        
        @Test
        public void utf8File() {
            Path file = TESTDATA.resolve("utf-8.txt");

            assertTrue(assertDoesNotThrow(() -> EncodingUtils.checkEncoding(file, StandardCharsets.UTF_8)));
        }
        
        @Test
        public void longUtf8File() {
            // utf-8.long.txt has a three-byte UTF-8 character at the 1024 byte mark
            // this covers the edge case, that the input buffer has remaining bytes after a decoding pass
            Path file = TESTDATA.resolve("utf-8.long.txt");
            
            assertTrue(assertDoesNotThrow(() -> EncodingUtils.checkEncoding(file, StandardCharsets.UTF_8)));
        }
        
        @Test
        public void iso88591File() {
            Path file = TESTDATA.resolve("ISO-8859-1.txt");
            
            assertAll(
                () -> assertTrue(EncodingUtils.checkEncoding(file, StandardCharsets.ISO_8859_1)),
                () -> assertFalse(EncodingUtils.checkEncoding(file, StandardCharsets.UTF_8))
            );
        }
        
        @Test
        public void cp1252File() {
            Path file = TESTDATA.resolve("cp1252.txt");
            
            assertAll(
                () -> assertTrue(EncodingUtils.checkEncoding(file, Charset.forName("cp1252"))),
                () -> assertFalse(EncodingUtils.checkEncoding(file, StandardCharsets.UTF_8))
            );
        }
        
        @Test
        public void invalidFile() {
            // invalid starts as a text file, but then has random bytes that fit neither UTF-8 nor CP-1252
            // there seems to be no way to create an invalid ISO 8859-1 file, though...
            Path file = TESTDATA.resolve("invalid.txt");
            
            assertAll(
                () -> assertFalse(EncodingUtils.checkEncoding(file, StandardCharsets.UTF_8)),
                () -> assertFalse(EncodingUtils.checkEncoding(file, Charset.forName("cp1252")))
            );
        }
        
    }
    
}
