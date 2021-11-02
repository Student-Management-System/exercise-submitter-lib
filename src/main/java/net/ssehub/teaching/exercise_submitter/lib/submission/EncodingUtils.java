package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility method for text file encodings.
 * 
 * @author Adam
 */
public class EncodingUtils {

    /**
     * List of {@link Charset}s to check when converting files that are not UTF-8.
     * Used in {@link #getUtf8ConvertedContent(Path)}.
     */
    private static final Charset[] CHARSETS_TO_CHECK;

    static {
        List<Charset> charsets = new LinkedList<>();
        if (Charset.isSupported("cp1252")) {
            charsets.add(Charset.forName("cp1252"));
        }
        // don't include StandardCharsets.ISO_8859_1 here, as it accepts all files
        CHARSETS_TO_CHECK = charsets.toArray(new Charset[charsets.size()]);
    }
    
    /**
     * No instances.
     */
    private EncodingUtils() {
    }
    
    /**
     * Returns the content of the given text file as UTF-8 encoded bytes. This method tries a few different charsets;
     * if all fail, the file content will be returned as-is.
     * 
     * @param file The text file which is not UTF-8 encoded.
     * 
     * @return The content of the file, encoded as UTF-8.
     * 
     * @throws IOException If reading the file fails.
     */
    public static byte[] getUtf8ConvertedContent(Path file) throws IOException {
        
        byte[] content = Files.readAllBytes(file);
        
        for (Charset charset : CHARSETS_TO_CHECK) { // break; when correct charset is found
            
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            
            try {
                CharBuffer textcontent = decoder.decode(ByteBuffer.wrap(content));
                
                ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(textcontent);
                
                content = new byte[encoded.limit()];
                encoded.get(content);
                break;
                
            } catch (MalformedInputException | UnmappableCharacterException e) {
                // invalid charset mapping, ignore and try next one
            }
        }
        
        return content;
    }
    
    /**
     * Checks if the given file has the given encoding.
     *
     * @param file The file to check.
     * @param encoding The encoding to check.
     *
     * @return Whether the file has the given encoding.
     *
     * @throws IOException If reading the file fails.
     */
    public static boolean checkEncoding(Path file, Charset encoding) throws IOException {
        
        boolean foundError = false;
        CharsetDecoder decoder = encoding.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (ByteChannel stream = Files.newByteChannel(file)) {

            ByteBuffer inBuffer = ByteBuffer.allocate(1024);
            CharBuffer outBuffer = CharBuffer.allocate(1024);

            while (stream.read(inBuffer) != -1) {
                inBuffer.flip();

                CoderResult result = decoder.decode(inBuffer, outBuffer, false);
                outBuffer.clear(); // discard characters, we are not interested in them

                if (result.isError()) {
                    foundError = true;
                    break;
                }

                // copy the remaining bytes to the start of the buffer
                // this may happen if we are, e.g., in the middle of an utf-8 character
                int remaining = inBuffer.remaining();
                for (int i = 0; inBuffer.remaining() > 0; i++) {
                    inBuffer.put(i, inBuffer.get());
                }
                inBuffer.position(remaining);
            }

            if (!foundError) {
                // read the last remaining bytes
                inBuffer.flip();
                CoderResult result = decoder.decode(inBuffer, outBuffer, true);
                foundError = result.isError();
            }
        }

        return !foundError;
    }
    
}
