package net.ssehub.teaching.exercise_submitter.lib.replay;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.server.api.ApiClient;
import net.ssehub.teaching.exercise_submitter.server.api.ApiException;
import net.ssehub.teaching.exercise_submitter.server.api.api.SubmissionApi;
import net.ssehub.teaching.exercise_submitter.server.api.model.FileDto;

/**
 * Replays versions from the exercise-submitter-server version history of an exercise submission.
 *
 * @author Adam
 * @author Lukas
 */
public class Replayer implements Closeable {

    private String courseId;
    
    private String assignmentName;
    
    private String groupName;
    
    private SubmissionApi api;
    
    private Map<Version, File> cachedFiles = new HashMap<Version, File>();

    /**
     * Creates a new replayer for the given assignment.
     * 
     * @param baseUrl The URL of the exercise-submitter-server API.
     * @param courseId the ID of the course to submit to.
     * @param assignmentName The name of the assignment to submit to.
     * @param groupName The name of the group to submit to. May be the students name for non-group assignments.
     * @param token The token to authenticate with. This is the same as used for the student management system. 
     *
     */
    public Replayer(String baseUrl, String courseId, String assignmentName, String groupName, String token) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        client.setAccessToken(token);
        this.api = new SubmissionApi(client);
        
        this.courseId = courseId;
        this.assignmentName = assignmentName;
        this.groupName = groupName;
    }
    
    /**
     * Represents a version in the homework submission history.
     */
    public static class Version {

        private String author;

        private Instant timestamp;

        /**
         * Creates a new version.
         *
         * @param author    The author that created this version.
         * @param timestamp The timestamp of this version.
         */
        Version(String author, Instant timestamp) {
            this.author = author;
            this.timestamp = timestamp;
        }

        /**
         * Returns the author that created this version (i.e. who committed this
         * version).
         *
         * @return The username of the author of this version.
         */
        public String getAuthor() {
            return this.author;
        }

        /**
         * Returns the timestamp when this version was created.
         *
         * @return The timestamp of this version.
         */
        public Instant getTimestamp() {
            return this.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(author, timestamp);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Version other = (Version) obj;
            return Objects.equals(author, other.author) && Objects.equals(timestamp, other.timestamp);
        }

        @Override
        public String toString() {
            return "Version [author=" + this.author + ", timestamp=" + this.timestamp + "]";
        }

    }

    /**
     * Returns a list of all versions that were submitted to the assignment. The
     * entries are sorted with the most recent versions first.
     *
     * @return The list of versions.
     * @throws ReplayException
     */
    public List<Version> getVersions() throws ReplayException {
        try {
            return api.listVersions(courseId, assignmentName, groupName).stream()
                    .map(dto -> new Version(dto.getAuthor(), Instant.ofEpochSecond(dto.getTimestamp())))
                    .collect(Collectors.toList());
            
        } catch (ApiException e) {
            throw new ReplayException("Failed to retrieve version list", e);
        }
    }

    /**
     * Replays the given version to a temporary directory. The directory will be
     * cleared when this {@link Replayer} is closed.
     *
     * @param version The version to replay. See {@link #getVersions()}.
     *
     * @return A temporary directory with the submission content.
     * @throws ReplayException
     */
    public File replay(Version version) throws ReplayException {
        File temp = null;
        if (this.checkIfCached(version).isPresent()) {
            temp = this.checkIfCached(version).get();
        } else {
            
            try {
                temp = this.createTempDir();
            } catch (IOException e) {
                throw new ReplayException("Failed to create temporary directory", e);
            }
            Path dir = temp.toPath();
    
            try {
                List<FileDto> files = api.getVersion(
                        courseId, assignmentName, groupName, version.getTimestamp().getEpochSecond());
                
                for (FileDto dto : files) {
                    
                    Path filepath = dir.resolve(dto.getPath());
                    byte[] content = Base64.getDecoder().decode(dto.getContent());
                    Files.write(filepath, content);
                }
                
                cacheVersion(version, temp);
                
            } catch (IOException e) {
                throw new ReplayException("Failed to write file", e);
                
            } catch (ApiException e) {
                throw new ReplayException("Failed to retrieve submission version", e);
            }
        }

        return temp;
    }

    /**
     * Checks if the given directory has the same content as the given submitted
     * version.
     *
     * @param directory A directory that will be checked.
     * @param version   The version which content will be compared to the given
     *                  directory.
     *
     * @return Whether the given directory and the submitted version have the same
     *         content.
     * @throws IOException
     * @throws ReplayException
     */
    public boolean isSameContent(File directory, Version version) throws IOException, ReplayException {

        File result = this.replay(version);

        boolean sameContent = this.compareTwoFiles(directory.toPath(), result.toPath(), directory, true);


        return sameContent;

    }

    /**
     * Clears all temporary directories with checked-out versions.
     */
    @Override
    public void close() throws IOException {
        for (Map.Entry<Version, File> entry : this.cachedFiles.entrySet()) {
            Replayer.deleteDir(entry.getValue(), null);
        }
    }

    /**
     * Creates a temporay dir.
     *
     * @return the temp Dir as File
     * @throws IOException
     */
    private File createTempDir() throws IOException {
        File temp = File.createTempFile("exercise_submission", null);
        temp.delete();
        temp.mkdir();
        return temp;
    }

    /**
     * Compares two directories if they have the same content.
     *
     * @param baselocal , first dir
     * @param basetemp  , second dir
     * @param firstfile , firstdir as File
     * @param result    , always set to true
     * @return true, for same content false for NOT the same content.
     */
    private boolean compareTwoFiles(Path baselocal, Path basetemp, File firstfile, boolean result) {
        if (firstfile.isFile()) {
            try {
                String firstFile = "";
                try (BufferedReader reader = new BufferedReader(new FileReader(firstfile))) {
                    firstFile = reader.lines().collect(Collectors.joining("\n", "", "\n"));
                }
                String secondFile = "";
                Path relativized = baselocal.relativize(firstfile.toPath());
                try (BufferedReader reader = new BufferedReader(
                        new FileReader(basetemp.resolve(relativized).toFile()))) {
                    secondFile = reader.lines().collect(Collectors.joining("\n", "", "\n"));
                }
                if (!firstFile.equals(secondFile)) {
                    result = false;
                }
            } catch (IOException e) {
                result = false;
            }
        } else if (firstfile.list().length != 0) {
            for (int i = 0; i < firstfile.listFiles().length; i++) {
                result = this.compareTwoFiles(baselocal, basetemp, firstfile.listFiles()[i], result);
            }
        } else {
            Path relativized = baselocal.relativize(firstfile.toPath());
            File emptydir = basetemp.resolve(relativized).toFile();
            if (!(emptydir.exists() && emptydir.isDirectory() && emptydir.listFiles().length == 0)) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Deletes the given dir.
     *
     * @param dir         , directory that should be deleted
     * @param currentfile , the same as above or null
     */
    private static void deleteDir(File dir, File currentfile) {
        if (currentfile == null) {
            currentfile = dir;
        }
        if (currentfile.isFile() || currentfile.list().length == 0) {
            currentfile.delete();
        } else {
         
            File[] files  = currentfile.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteDir(dir, files[i]);
                if (currentfile.list().length == 0) {
                    currentfile.delete();
                }
                
            
            }

        }

    }
    /**
     * Caches the version.
     * @param version
     * @param dir
     */
    private void cacheVersion(Version version, File dir) {
        this.cachedFiles.put(version, dir);
    }
    /**
     * Checks if the version is cached. And gives when its cached give the dir back.
     * @param version
     * @return Optional<File>
     */
    private Optional<File> checkIfCached(Version version) {
        return Optional.ofNullable(this.cachedFiles.get(version));
    }

}
