package net.ssehub.teaching.exercise_submitter.lib.replay;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;

/**
 * Replays versions from the SVN version history of an exercise submission.
 *
 * @author Lukas
 * @author Adam
 */
public class Replayer implements Closeable {

    private SVNURL url;
    private ExerciseSubmitterManager.Credentials credentials;
    private SVNClientManager clientmanager;

    /**
     * Creates a new replayer for the given SVN URL.
     *
     * @param url         The URL to the homework submission folder.
     * @param credentials The username and password
     * @throws IOException
     */
    public Replayer(String url, ExerciseSubmitterManager.Credentials credentials) throws IOException {
        this.credentials = credentials;
        try {
            this.createSVNURL(url);
        } catch (SVNException e) {
            throw new IOException("Urltype is not supported");
        }
        this.createClientmanager();

    }

    /**
     * Represents a version in the homework submission history.
     */
    public static class Version {


        private String author;

        private LocalDateTime timestamp;

        private long revision;

        /**
         * Creates a new version.
         *
         * @param author    The author that created this version.
         * @param timestamp The timestamp of this version.
         * @param revision  The SVNrevisionid of this version
         */
        Version(String author, LocalDateTime timestamp, long revision) {
            this.author = author;
            this.timestamp = timestamp;
            this.revision = revision;
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
        public LocalDateTime getTimestamp() {
            return this.timestamp;
        }

        /**
         * Return the revisionid from this version.
         *
         * @return the revisionid from this version
         */
        public long getRevision() {
            return this.revision;
        }
       
        @Override
        public int hashCode() {
            return Objects.hash(author, revision, timestamp);
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
            return Objects.equals(author, other.author) && revision == other.revision
                    && Objects.equals(timestamp, other.timestamp);
        }

        @Override
        public String toString() {
            return "Version [author=" + this.author + ", timestamp=" + this.timestamp + ", revision=" + this.revision
                    + "]";
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
            SVNRepository repository = this.clientmanager.createRepository(this.url, false);
            SVNNodeKind nodeKind = repository.checkPath("", -1);
            if (nodeKind == SVNNodeKind.NONE) {
                throw new ReplayException("Not a valid repository url");
            } else if (nodeKind == SVNNodeKind.FILE) {
                throw new ReplayException("Url points to a file not a directory");
            }
            List<Version> list = convertSVNRevisionListEntriesToVersion(repository, "", new ArrayList<Version>())
                    .stream().distinct().collect(Collectors.toList());
            
            Comparator<Version> titleComparator = new Comparator<Version>() {
                /**
                 * Compares the to version for the earlier time.
                 */
                public int compare(Version version1, Version version2) {
                    return version1.getTimestamp().compareTo(version2.getTimestamp());
                }
            };
            Collections.sort(list, titleComparator);
            return list;
        } catch (SVNException e) {
            throw new ReplayException(e);
        }
    }

    /**
     * Replays the given version to a temporary directory. The directory will be
     * cleared when this {@link Replayer} is closed.
     *
     * @param version The version to replay. See {@link #getVersions()}.
     *
     * @return A temporary directory with the submission content.
     * @throws IOException
     * @throws ReplayException
     */
    public File replay(Version version) throws IOException, ReplayException {
        File temp = this.createTempDir();

        SVNUpdateClient client = this.clientmanager.getUpdateClient();
        try {
            client.doCheckout(this.url, temp, SVNRevision.HEAD, SVNRevision.create(version.revision), SVNDepth.INFINITY,
                    true);
        } catch (SVNException e) {
            throw new ReplayException("Cant make checkout", e);
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

        Replayer.deleteDir(result, result);

        return sameContent;

    }

    /**
     * Clears all temporary directories with checked-out versions.
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * Create the clientmanager for the SVN connection.
     */
    private void createClientmanager() {
        this.clientmanager = SVNClientManager.newInstance(null,
                BasicAuthenticationManager.newInstance(this.credentials.getUsername(), this.credentials.getPassword()));
    }

    /**
     * Parses a string url into a SVNURL.
     *
     * @param url The url that should be parsed
     * @throws SVNException
     */
    private void createSVNURL(String url) throws SVNException {
        this.url = SVNURL.parseURIEncoded(url);
    }

    /**
     * Downloads and converts the SVNRevisionList into Version.
     *
     * @param repository  The current repository.
     * @param path        , default: ""
     * @param versionlist , empty list
     * @return List<Version> the versions from the repository which are on the
     *         server
     * @throws SVNException
     */
    @SuppressWarnings("rawtypes")
    private static List<Version> convertSVNRevisionListEntriesToVersion(SVNRepository repository, String path,
            List<Version> versionlist) throws SVNException {
        Collection entries = repository.getDir(path, -1, null, (Collection) null);
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            versionlist.add(new Version(entry.getAuthor(),
                    entry.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), entry.getRevision()));
            if (entry.getKind() == SVNNodeKind.DIR) {
                versionlist = convertSVNRevisionListEntriesToVersion(repository,
                        path.equals("") ? entry.getName() : path + "/" + entry.getName(), versionlist);
            }
        }
        return versionlist;

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

}
