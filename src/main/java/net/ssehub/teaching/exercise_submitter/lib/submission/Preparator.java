package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
        this.result.mkdir();
        Preparator.copyDirectory(directory.getAbsolutePath(), this.result.getAbsolutePath());

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
     * @param sourceDirectoryLocation the source directory location
     * @param destinationDirectoryLocation the destination directory location
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation)).forEach(source -> {
            Path destination = Paths.get(destinationDirectoryLocation,
                    source.toString().substring(sourceDirectoryLocation.length()));

            try {
                Files.copy(source, destination);
            } catch (IOException e) {

            }

        });
    }

}
