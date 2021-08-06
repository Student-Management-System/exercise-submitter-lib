package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Prepares a folder for submission. Creates a copy of the folder in a temporary location, see {@link #getResult()}.
 * This temporary folder can be submitted.
 * 
 * @author Adam
 * @author Lukas
 */
class Preparator implements Closeable {

    private File result;

    /**
     * Instantiates a new preparator.
     *
     * @param directory The directory to prepare for submission.
     * 
     * @throws IOException If IO fails during preparation.
     */
    public Preparator(File directory) throws IOException {

        if (!directory.isDirectory()) {
            throw new IOException(directory.getName() + " is not a directory");

        }

        this.result = File.createTempFile("exercise_submission", null);
        this.result.delete();
        copyAndPrepareDirectory(directory, this.result);

        this.result.deleteOnExit();

    }

    /**
     * Gets the temporary directory where the preparation result is located.
     *
     * @return The prepared folder.
     */
    public File getResult() {
        return this.result;
    }

    /**
     * Removes the temporary copy created by this preparator.
     *
     * @throws IOException If deleting the temporary copy fails.
     */
    @Override
    public void close() throws IOException {
        try {
            Files.walk(this.result.toPath())
                    .sorted(Comparator.reverseOrder())
                    .forEach(t -> {
                        try {
                            Files.delete(t);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Copy directory.
     *
     * @param sourceDirectory      the source directory location
     * @param destinationDirectory the destination directory location
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyAndPrepareDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        try {
            Path sourcePath = sourceDirectory.toPath();
            Files.walk(sourcePath).forEach(toCopy -> {

                File destination = new File(destinationDirectory, sourcePath.relativize(toCopy).toString());
                if (toCopy.toFile().isFile()) {
                    try {
                        boolean result = checkEncoding(toCopy, StandardCharsets.UTF_8);
                        if (result) {
                            Files.copy(toCopy, destination.toPath());

                        } else {
                            try (InputStream in = Files.newInputStream(toCopy);
                                    OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(destination),
                                            StandardCharsets.UTF_8)) {

                                int length = 0;
                                byte[] bytes = new byte[1024];

                                // copy data from input stream to output stream
                                while ((length = in.read(bytes)) != -1) {
                                    String readable = Arrays.toString(bytes);
                                    out.write(readable.toCharArray());

                                }
                                out.flush();

                            }
                        }
                    } catch (IOException e) {

                        throw new UncheckedIOException(e);
                    }

                } else {
                    try {
                        Files.copy(toCopy, destination.toPath());
                    } catch (IOException e) {

                        throw new UncheckedIOException(e);
                    }
                }

            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Checks if the given file has the given encoding.
     * <p>
     * Package visibility for test cases.
     * 
     * @param file The file to check.
     * @param encoding The encoding to check.
     * 
     * @return Whether the file has the given encoding.
     * 
     * @throws IOException If reading the file fails.
     */
    static boolean checkEncoding(Path file, Charset encoding) throws IOException {
        
        boolean foundError = false;
        CharsetDecoder decoder = encoding.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        
        try (ByteChannel stream = Files.newByteChannel(file)) {

            ByteBuffer inBuffer = ByteBuffer.allocate(1024);
            CharBuffer outBuffer = CharBuffer.allocate(1024);
            
            int numRead = 0;
            int remaining = 0;
            while ((numRead = stream.read(inBuffer)) != -1) {
                inBuffer.position(0);
                inBuffer.limit(numRead + remaining);
                
                CoderResult result = decoder.decode(inBuffer, outBuffer, false);
                outBuffer.clear(); // discard characters, we are not interested in them
                
                if (result.isError()) {
                    foundError = true;
                    break;
                }
                
                // copy the remaining bytes to the start of the buffer
                // this may happen if we are, e.g., in the middle of an utf-8 character
                remaining = inBuffer.remaining();
                for (int i = 0; inBuffer.remaining() > 0; i++) {
                    inBuffer.put(i, inBuffer.get());
                }
                inBuffer.position(remaining);
            }

            if (!foundError) {
                // read the last remaining bytes
                inBuffer.limit(remaining);
                CoderResult result = decoder.decode(inBuffer, outBuffer, true);
                foundError = result.isError();
            }
        }
        
        return !foundError;
    }

}
