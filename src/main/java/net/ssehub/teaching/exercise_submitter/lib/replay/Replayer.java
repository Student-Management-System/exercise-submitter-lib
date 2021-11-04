package net.ssehub.teaching.exercise_submitter.lib.replay;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    
    private Map<Version, Path> cachedFiles = new HashMap<>();

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
        Path resultCheckout = cachedFiles.get(version);
        
        if (resultCheckout == null) {
            try {
                resultCheckout = Files.createTempDirectory("submission_replay");
            } catch (IOException e) {
                throw new ReplayException("Failed to create temporary directory", e);
            }
    
            try {
                List<FileDto> files = api.getVersion(
                        courseId, assignmentName, groupName, version.getTimestamp().getEpochSecond());
                
                for (FileDto dto : files) {
                    
                    Path filepath = resultCheckout.resolve(dto.getPath());
                    Files.createDirectories(filepath.getParent());
                    
                    byte[] content = Base64.getDecoder().decode(dto.getContent());
                    Files.write(filepath, content);
                }
                
                
            } catch (IOException e) {
                try {
                    deleteDirectory(resultCheckout);
                } catch (IOException e1) {
                    // ignore
                }
                throw new ReplayException("Failed to write file", e);
                
            } catch (ApiException e) {
                try {
                    deleteDirectory(resultCheckout);
                } catch (IOException e1) {
                    // ignore
                }
                throw new ReplayException("Failed to retrieve submission version", e);
            }
            
            cachedFiles.put(version, resultCheckout);
        }

        return resultCheckout.toFile();
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
     *         
     * @throws IOException If comparing the file content fails.
     * @throws ReplayException If retrieving the given version fails.
     */
    public boolean isSameContent(File directory, Version version) throws IOException, ReplayException {
        File result = replay(version);
        // TODO: ignore eclipse project files, as the submitter does not upload them
        return pathContentEqual(directory.toPath(), result.toPath());
    }

    /**
     * Clears all temporary directories with checked-out versions.
     */
    @Override
    public void close() throws IOException {
        IOException exception = null;
        
        for (Path directory : cachedFiles.values()) {
            try {
                deleteDirectory(directory);
            } catch (IOException e) {
                exception = e;
            }
        }
        
        cachedFiles.clear();
        
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Checks if the two paths have equal content. If paths are files, their content is compared. If paths are
     * directories, they must have the same files with equal content (recursive {@link #pathContentEqual(Path, Path)}).
     * <p>
     * Package visibility for test cases.
     * 
     * @param path1 The first path to compare.
     * @param path2 The second path to compare.
     * 
     * @return Whether the two paths are equal.
     * 
     * @throws IOException If reading files or directories fails.
     */
    static boolean pathContentEqual(Path path1, Path path2) throws IOException {
        boolean result = false;
        
        if (Files.isRegularFile(path1) && Files.isRegularFile(path2)) {
            result = Arrays.equals(Files.readAllBytes(path1), Files.readAllBytes(path2));
            
        } else if (Files.isDirectory(path1) && Files.isDirectory(path2)) {
            Set<Path> content1 = Files.list(path1)
                    .map(p -> path1.relativize(p))
                    .collect(Collectors.toSet());
            Set<Path> content2 = Files.list(path2)
                    .map(p -> path2.relativize(p))
                    .collect(Collectors.toSet());
            
            if (content1.equals(content2)) {
                try {
                    result = content1.stream()
                            .map(nested -> {
                                try {
                                    return pathContentEqual(path1.resolve(nested), path2.resolve(nested));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            })
                            .reduce(Boolean::logicalAnd)
                            .orElse(true);
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
            }
        }
        
        return result;
    }
    
    /**
     * Deletes a directory with all content of it.
     * 
     * @param directory The folder to delete.
     * 
     * @throws IOException If deleting the directory fails.
     */
    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
