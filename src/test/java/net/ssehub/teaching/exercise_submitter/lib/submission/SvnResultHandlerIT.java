package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_submitter.lib.submission.Problem.Severity;

public class SvnResultHandlerIT {
    
    private static StuMgmtDocker docker;
    
    private final static File TESTDATA = new File("src/test/resources/SvnResultHandlerTest");
    
    private static net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient;

    private static ApiClient mgmtClient;
    
    @BeforeAll
    public static void setupServers() {
        docker = new StuMgmtDocker();
        docker.createUser("svn", "abcdefgh");
        docker.createUser("adam", "123456");
        docker.createUser("student1", "123456");
        docker.createUser("student2", "123456");
        docker.createUser("student3", "123456");
        docker.createUser("student4", "123456");
        
        String courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam", "svn");
        
        docker.startSvn(courseId, "svn");
        
        docker.enrollStudent(courseId, "student1");
        docker.enrollStudent(courseId, "student2");
        docker.enrollStudent(courseId, "student3");
        docker.enrollStudent(courseId, "student4");
        
        docker.createGroup(courseId, "JP001", "student1", "student3");
        docker.createGroup(courseId, "JP002", "student2", "student4");
        
        String a1 = docker.createAssignment(courseId, "Homework01", AssignmentState.INVISIBLE, Collaboration.GROUP);
        String a2 = docker.createAssignment(courseId, "Homework02", AssignmentState.INVISIBLE, Collaboration.GROUP);
        docker.createAssignment(courseId, "Testat01", AssignmentState.INVISIBLE, Collaboration.SINGLE);
        
        docker.changeAssignmentState(courseId, a1, AssignmentState.SUBMISSION);
        docker.changeAssignmentState(courseId, a1, AssignmentState.IN_REVIEW);
    
        docker.changeAssignmentState(courseId, a2, AssignmentState.SUBMISSION);
        
        assertDoesNotThrow(() -> {
            
            authClient = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
            authClient.setBasePath(docker.getAuthUrl());

            mgmtClient = new ApiClient();
            mgmtClient.setBasePath(docker.getStuMgmtUrl());
            
            CredentialsDto credentials = new CredentialsDto();
            credentials.setUsername("student1");
            credentials.setPassword("123456");
            
            AuthControllerApi api = new AuthControllerApi(authClient);
            
            AuthenticationInfoDto authinfo = api.authenticate(credentials);
            mgmtClient.setAccessToken(authinfo.getToken().getToken());
    
        });
    }

    @AfterAll
    public static void tearDownServers() {
        docker.close();
    }
    
   @Test
    public void convertErrorMessageToStringTest() {
    
       assertDoesNotThrow(()-> {
       Preparator prep = new Preparator(new File(TESTDATA,"notcompiling"));
       File testdir = prep.getResult();
      
       String buildurl = docker.getSvnUrl() + "Homework02/JP001/";
       SVNURL url = SVNURL.parseURIEncoded(buildurl);
      
       SVNClientManager clientManager = SVNClientManager.newInstance(null, 
               BasicAuthenticationManager.newInstance("student1", "123456".toCharArray()));
               
       SVNCommitClient client = clientManager.getCommitClient();
       client.setCommitParameters(new DefaultSVNCommitParameters() {
           @Override
           public Action onMissingFile(File file) {
               return DELETE;
           }

           @Override
           public Action onMissingDirectory(File file) {
               return DELETE;
           }
       });
       
       SVNUpdateClient update = clientManager.getUpdateClient();
       update.doCheckout(url, testdir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
     
     
       SVNWCClient wcClient = clientManager.getWCClient();
       
       clientManager.getStatusClient().doStatus(testdir, SVNRevision.HEAD, SVNDepth.INFINITY,
               false, false, false, false,
           (status) -> {
               SVNStatusType type = status.getNodeStatus();
               File file = status.getFile();

               if (type == SVNStatusType.STATUS_UNVERSIONED) {
                   wcClient.doAdd(file, true, false, false, SVNDepth.EMPTY, false, false);
                   
               } else if (type == SVNStatusType.STATUS_MISSING) {
                   wcClient.doDelete(file, true, false, false);
               }
           }, null);
       
    
       SVNCommitInfo info = null;
       try {
           info = client.doCommit(new File[] {testdir}, false, "Testcommit", null, null, false, false,
                   SVNDepth.INFINITY);
           
        
       } catch (SVNException e) {
           SVNErrorMessage errorMsg = e.getErrorMessage();
           if (errorMsg.hasChildWithErrorCode(SVNErrorCode.REPOS_HOOK_FAILURE)) {
               info = new SVNCommitInfo(-1, "test", new Date(), errorMsg);
           }
       } 
       
       assertEquals(SvnResultHandler.svnErrorMessageToString(info.getErrorMessage()), 
               "\n<submitResults>\n"
               + "    <message file=\"main.java\" line=\"3\" message=\"invalid method declaration;"
               + " return type required\" tool=\"javac\" type=\"error\">\n"
               + "        <example position=\"5\"/>\n"
               + "    </message>\n"
               + "    <message file=\"main.java\" line=\"3\" message=\"';' expected\" tool=\"javac\" type=\"error\">\n"
               + "        <example position=\"23\"/>\n"
               + "    </message>\n"
               + "</submitResults>\n\n");
       
   });
    }
   @Test
   public void convertPostSvnErrorMessageToProblem() {
       assertDoesNotThrow(()-> {
           Preparator prep = new Preparator(new File(TESTDATA,"notcompiling"));
           File testdir = prep.getResult();
           
           String buildurl = docker.getSvnUrl() + "Homework02/JP001/";
           SVNURL url = SVNURL.parseURIEncoded(buildurl);
          
           
           SVNClientManager clientManager = SVNClientManager.newInstance(null, 
                   BasicAuthenticationManager.newInstance("student1", "123456".toCharArray()));
                   
           SVNCommitClient client = clientManager.getCommitClient();
           client.setCommitParameters(new DefaultSVNCommitParameters() {
               @Override
               public Action onMissingFile(File file) {
                   return DELETE;
               }

               @Override
               public Action onMissingDirectory(File file) {
                   return DELETE;
               }
           });
           
           SVNUpdateClient update = clientManager.getUpdateClient();
           update.doCheckout(url, testdir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
         
         
           SVNWCClient wcClient = clientManager.getWCClient();
           
           clientManager.getStatusClient().doStatus(testdir, SVNRevision.HEAD, SVNDepth.INFINITY,
                   false, false, false, false,
               (status) -> {
                   SVNStatusType type = status.getNodeStatus();
                   File file = status.getFile();

                   if (type == SVNStatusType.STATUS_UNVERSIONED) {
                       wcClient.doAdd(file, true, false, false, SVNDepth.EMPTY, false, false);
                       
                   } else if (type == SVNStatusType.STATUS_MISSING) {
                       wcClient.doDelete(file, true, false, false);
                   }
               }, null);
           
        
           SVNCommitInfo info = null;
           try {
               info = client.doCommit(new File[] {testdir}, false, "Testcommit", null, null, false, false,
                       SVNDepth.INFINITY);
               
            
           } catch (SVNException e) {
               SVNErrorMessage errorMsg = e.getErrorMessage();
               if (errorMsg.hasChildWithErrorCode(SVNErrorCode.REPOS_HOOK_FAILURE)) {
                   info = new SVNCommitInfo(-1, "test", new Date(), errorMsg);
               }
           } 
           SvnResultHandler handler = new SvnResultHandler(SvnResultHandler.svnErrorMessageToString(info.getErrorMessage()));
          
           Problem problem1 = new Problem("javac","invalid method declaration; return type required",Severity.ERROR);
                   problem1.setFile(new File("main.java"));
                   problem1.setLine(3);
                   problem1.setColumn(5);
           Problem problem2 = new Problem("javac","';' expected",Severity.ERROR);
                   problem2.setFile(new File("main.java"));
                   problem2.setLine(3);
                   problem2.setColumn(23);
                   
           List<Problem> problems = handler.parseXmlToProblem();   
           
           assertTrue(problems.size() == 2);
           assertEquals(problems.get(0),problem1);
           assertEquals(problems.get(1), problem2);
                   
                               
       });
   }
}