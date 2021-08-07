package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Prepares a folder for submission. Creates a copy of the folder in a temporary
 * location, see {@link #getResult()}. This temporary folder can be submitted.
 * <p>
 * Preparation does the following tasks:
 * <ul>
 * <li>Try to convert all text files to UTF-8, if they are not already
 * UTF-8.</li>
 * </ul>
 *
 * @author Adam
 * @author Lukas
 */
class Preparator implements Closeable {

    /**
     * List of {@link Charset}s to check when reading files that are not UTF-8. If a
     * non-UTF-8 file is found, but can be read using any of these charsets, then we
     * convert it to UTF-8.
     */
    private static final Charset[] CHARSETS_TO_CHECK;

    static {
        List<Charset> charsets = new LinkedList<>();
        charsets.add(StandardCharsets.ISO_8859_1);
        if (Charset.isSupported("cp1252")) {
            charsets.add(Charset.forName("cp1252"));
        }
        CHARSETS_TO_CHECK = charsets.toArray(new Charset[charsets.size()]);
    }

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
        copyAndPrepareDirectory(directory.toPath(), this.result.toPath());
        this.createEclipseProjectFiles(this.result);
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
            Files.walk(this.result.toPath()).sorted(Comparator.reverseOrder()).forEach(t -> {
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
     * Copies and prepares the given source directory.
     *
     * @param sourceDirectory      The source directory to copy.
     * @param destinationDirectory The destination where to put the prepared copy.
     *
     * @throws IOException If reading the source or writing the destination fails.
     */
    private static void copyAndPrepareDirectory(Path sourceDirectory, Path destinationDirectory) throws IOException {
        try {
            Files.walk(sourceDirectory).forEach(sourceFile -> {
                Path destinationFile = destinationDirectory.resolve(sourceDirectory.relativize(sourceFile));
                try {
                    preparePath(sourceFile, destinationFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void createEclipseProjectFiles(File destination) throws IOException {
        File classpath = new File(destination, ".classpath");
        final String RESOURCE_PATH = "net/ssehub/teaching/exercise_submitter/lib/preparator";

        if (!classpath.exists()) {
            try (InputStream input = this.getClass().getClassLoader()
                    .getResourceAsStream(RESOURCE_PATH + "/.classpath")) {
                Files.copy(input, Path.of(destination.toString() + "/.classpath"), StandardCopyOption.REPLACE_EXISTING);

            }

        }

        File projekt = new File(destination, ".project");
        if (!projekt.exists()) {
            try (InputStream input = this.getClass().getClassLoader()
                    .getResourceAsStream(RESOURCE_PATH + "/.project")) {
                Files.copy(input, Path.of(destination.toString() + "/.project"), StandardCopyOption.REPLACE_EXISTING);

            }
        }

    }

    /**
     * Copies and prepares a single element. If source is a directory, an empty
     * directory is created at destination. If source is a file, a prepared file is
     * created at destination.
     *
     * @param sourceFile      The source file to copy (may be a file or a
     *                        directory).
     * @param destinationFile The destination to copy to.
     *
     * @throws IOException If reading or writing the files fails.
     */
    private static void preparePath(Path sourceFile, Path destinationFile) throws IOException {
        if (Files.isRegularFile(sourceFile)) {
            prepareFile(sourceFile, destinationFile);

        } else {
            Files.copy(sourceFile, destinationFile);
        }
    }

    /**
     * Prepares a file. If the encoding is wrong, it is converted to UTF-8.
     *
     * @param sourceFile      The source file to prepare.
     * @param destinationFile The location where to put the prepared copy.
     *
     * @throws IOException If reading or writing the file fails.
     */
    private static void prepareFile(Path sourceFile, Path destinationFile) throws IOException {
        String contentType = Files.probeContentType(sourceFile);
        boolean isTextFile = contentType != null && contentType.startsWith("text");
        if (!isTextFile || checkEncoding(sourceFile, StandardCharsets.UTF_8)) {
            Files.copy(sourceFile, destinationFile);

        } else {

            boolean success = false;
            for (Charset charset : CHARSETS_TO_CHECK) {
                if (checkEncoding(sourceFile, charset)) {

                    try (FileReader in = new FileReader(sourceFile.toFile(), charset);
                            FileWriter out = new FileWriter(destinationFile.toFile(), StandardCharsets.UTF_8)) {
                        in.transferTo(out);
                    }

                    success = true;
                    break;
                }
            }

            if (!success) {
                // TODO: some kind of warning message?
                Files.copy(sourceFile, destinationFile);
            }
        }
    }

    /**
     * Checks if the given file has the given encoding.
     * <p>
     * Package visibility for test cases.
     *
     * @param file     The file to check.
     * @param encoding The encoding to check.
     *
     * @return Whether the file has the given encoding.
     *
     * @throws IOException If reading the file fails.
     */
    static boolean checkEncoding(Path file, Charset encoding) throws IOException {

        boolean foundError = false;
        CharsetDecoder decoder = encoding.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
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
