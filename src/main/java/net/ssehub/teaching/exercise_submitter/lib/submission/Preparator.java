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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The Class Preparator prepares the files for Upload.
 *
 */
class Preparator implements Closeable {

    private File result;

    /**
     * Instantiates a new preparator.
     *
     * @param directory which one to prepare
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Preparator(File directory) throws IOException {

        if (!directory.isDirectory()) {
            throw new IOException(directory.getName() + " is not a directory");

        }

        this.result = File.createTempFile("exercise_submission", null);
        this.result.delete();
        Preparator.copyDirectory(directory, this.result);
        // this.result.mkdir();

        this.result.deleteOnExit();

    }

    /**
     * Gets the new created Temp Directory.
     *
     * @return the Directory
     */
    public File getResult() {
        return this.result;
    }

    /**
     * Close.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        this.result.delete();

    }

    /**
     * Copy directory.
     *
     * @param sourceDirectory      the source directory location
     * @param destinationDirectory the destination directory location
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        try {
            Path sourcePath = sourceDirectory.toPath();
            Files.walk(sourcePath).forEach(toCopy -> {

                File destination = new File(destinationDirectory, sourcePath.relativize(toCopy).toString());
                if (toCopy.toFile().isFile()) {
                    try {
                        boolean result = checkEncoding(toCopy);
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

    private static boolean checkEncoding(Path source) throws IOException {
        try (InputStream in = Files.newInputStream(source)) {

            int length = 0;
            byte[] bytes = new byte[1024];

            CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder();
            // TODO: do i need this ?
            CharBuffer cb = CharBuffer.wrap("");
            // copy data from input stream to output stream
            while ((length = in.read(bytes)) != -1) {
                dec.decode(ByteBuffer.wrap(bytes), cb, false);
            }
            CoderResult result = dec.decode(ByteBuffer.wrap(bytes), cb, true);

            if (result.isError()) {

                return false;
            }

        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return true;
    }

}
