package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.DefaultSVNCommitParameters;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.xml.sax.SAXException;

import net.ssehub.teaching.exercise_submitter.lib.ExerciseSubmitterManager;
import net.ssehub.teaching.exercise_submitter.lib.student_management_system.AuthenticationException;

/**
 * Submits solutions to a given SVN exercise submission location.
 *
 * @author Adam
 */
public class Submitter {

    private SVNClientManager clientmanager;
    private SVNCommitClient commitclient;

    private String url;
    private ExerciseSubmitterManager.Credentials cred;

    /**
     * Creates a new submitter for the given SVN location.
     *
     * @param url  The URL of the homework folder that should be submitted to.
     * @param cred the credentials
     */
    public Submitter(String url, ExerciseSubmitterManager.Credentials cred) {
        this.url = url;
        this.cred = cred;

    }

    /**
     * Submits the given directory.
     *
     * @param directory The directory that contains the solution to be submitted.
     *
     * @return The result of the submission.
     *
     * @throws SubmissionException      If the submission fails.
     * @throws IllegalArgumentException If the given directory is not a directory.
     * @throws AuthenticationException
     */
    public SubmissionResult submit(File directory)
            throws SubmissionException, IllegalArgumentException, AuthenticationException {

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        SubmissionResult submissionresult = null;

        try (Preparator preparator = new Preparator(directory)) {

            File result = preparator.getResult();

            this.clientmanager = SVNClientManager.newInstance(null,
                    BasicAuthenticationManager.newInstance(this.cred.getUsername(), this.cred.getPassword()));

            this.commitclient = this.clientmanager.getCommitClient();

            this.setCommitParameters();

            SVNURL svnurl = SVNURL.parseURIEncoded(this.url);

            this.doCheckout(result, svnurl);

            SVNCommitInfo info = null;

            try {
                info = this.commitclient.doCommit(new File[] {result}, false, "Testcommit", null, null, false, false,
                        SVNDepth.INFINITY);

            } catch (SVNAuthenticationException e) {
                throw new AuthenticationException("SVN cant authenticate succesfully");

            } catch (SVNException e) {
                SVNErrorMessage errorMsg = e.getErrorMessage();
                if (errorMsg.hasChildWithErrorCode(SVNErrorCode.REPOS_HOOK_FAILURE)) {
                    SvnResultHandler handler = new SvnResultHandler(SvnResultHandler.svnErrorMessageToString(errorMsg));
                    submissionresult = new SubmissionResult(false, handler.parseXmlToProblem());
                } else {
                    throw new SubmissionException("Response parse Error");
                }
            }

            if (info.getErrorMessage() != null) {
                SvnResultHandler handler = new SvnResultHandler(
                        SvnResultHandler.svnErrorMessageToString(info.getErrorMessage()));
                submissionresult = new SubmissionResult(
                        info.getErrorMessage().getErrorCode().equals(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED),
                        handler.parseXmlToProblem());
            } else {
                List<Problem> emptylist = new ArrayList<Problem>();
//                if (info.getNewRevision() == -1) {
                submissionresult = new SubmissionResult(true, emptylist);
//                } else {
//                    submissionresult = new SubmissionResult(true, emptylist);
//                }
            }

        } catch (IOException e) {
            throw new SubmissionException("cant create temp dir", e);
        } catch (SVNException | ParserConfigurationException | SAXException e) {
            throw new SubmissionException("cant do checkout", e);
        }
        return submissionresult;

    }

    /**
     * Sets the commit parameters which are needed for the checkout.
     */
    private void setCommitParameters() {
//        this.commitclient.setCommitParameters(new DefaultSVNCommitParameters() {
//            @Override
//            public Action onMissingFile(File file) {
//                return DELETE;
//            }
//
//            @Override
//            public Action onMissingDirectory(File file) {
//                return DELETE;
//            }
//        });
    }

    /**
     * Does a SVN checkout for the project files in the directory from the
     * repository behind the URL.
     *
     * @param dir    dir for the checkout
     * @param svnurl the svnurl where the repository is located
     * @throws SVNException the SVN exception
     */
    private void doCheckout(File dir, SVNURL svnurl) throws SVNException {
        SVNUpdateClient update = this.clientmanager.getUpdateClient();
        update.doCheckout(svnurl, dir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);

        SVNWCClient wcClient = this.clientmanager.getWCClient();

        this.clientmanager.getStatusClient().doStatus(dir, SVNRevision.HEAD, SVNDepth.INFINITY, false, false, false,
                false, status -> {
                SVNStatusType type = status.getNodeStatus();
                File file = status.getFile();

                if (type == SVNStatusType.STATUS_UNVERSIONED) {
                    wcClient.doAdd(file, true, false, false, SVNDepth.EMPTY, false, false);

//                } else if (type == SVNStatusType.STATUS_MISSING) {
//                    wcClient.doDelete(file, true, false, false);
                }
            }, null);
    }
}
