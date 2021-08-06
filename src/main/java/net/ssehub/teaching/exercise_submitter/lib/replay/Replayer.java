package net.ssehub.teaching.exercise_submitter.lib.replay;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Replays versions from the SVN version history of an exercise submission.
 * 
 * @author Adam
 */
public class Replayer implements Closeable {
    
    private String url;

    /**
     * Creates a new replayer for the given SVN URL.
     * 
     * @param url The URL to the homework submission folder.
     */
    public Replayer(String url) {
        this.url = url;
    }
    
    /**
     * Represents a version in the homework submission history.
     */
    public static class Version {
        
        private String author;
        
        private LocalDateTime timestamp;

        /**
         * Creates a new version.
         * 
         * @param author The author that created this version.
         * @param timestamp The timestamp of this version.
         */
        Version(String author, LocalDateTime timestamp) {
            this.author = author;
            this.timestamp = timestamp;
        }
        
        /**
         * Returns the author that created this version (i.e. who committed this version).
         * 
         * @return The username of the author of this version.
         */
        public String getAuthor() {
            return author;
        }
        
        /**
         * Returns the timestamp when this version was created.
         * 
         * @return The timestamp of this verison.
         */
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
    }
    
    /**
     * Returns a list of all versions that were submitted to the assignment. The entries are sorted with the most
     * recent versions first.
     * 
     * @return The list of versions.
     */
    public List<Version> getVersions() {
        return Arrays.asList(
                new Version("student2", LocalDateTime.of(2021, 06, 24, 18, 32)),
                new Version("student1", LocalDateTime.of(2021, 06, 24, 6, 12))
                );
    }
    
    /**
     * Replays the given version to a temporary directory. The directory will be cleared when this
     * {@link Replayer} is closed.
     * 
     * @param version The version to replay. See {@link #getVersions()}.
     * 
     * @return A temporary directory with the submission content.
     */
    public File replay(Version version) {
        return null;
    }
    
    /**
     * Checks if the given directory has the same content as the given submitted version.
     * 
     * @param directory A directory that will be checked.
     * @param version The version which content will be compared to the given directory.
     * 
     * @return Whether the given directory and the submitted version have the same content.
     */
    public boolean isSameContent(File directory, Version version) {
        return true;
    }

    /**
     * Clears all temporary directories with checked-out versions.
     */
    @Override
    public void close() throws IOException {
        
    }
    
}
