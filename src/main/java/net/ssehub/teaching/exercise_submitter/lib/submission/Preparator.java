package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;


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
     * @param sourceDirectory the source directory location
     * @param destinationDirectory the destination directory location
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyDirectory(File sourceDirectory, File destinationDirectory)
            throws IOException {
        try {
            Path sourcePath = sourceDirectory.toPath();
            Files.walk(sourcePath).forEach(toCopy -> {
                
                File destination = new File(destinationDirectory, sourcePath.relativize(toCopy).toString());
                try {
                    Files.copy(toCopy, destination.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

}
