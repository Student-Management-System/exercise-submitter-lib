package net.ssehub.teaching.exercise_submitter.lib.submission;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;



public class SvnResultHandlerTest {
    
    @Test
    public void parseErrorMessageToProblem() {
        final String erromessage = "<?xml version = \"1.0\"?>\n"
                                 +  "<submitResults>\n" 
                                 + "    <message tool=\"javac\" type=\"error\" message=\"';' expected\" file=\"Main.java\" line=\"20\">\n"
                                 + "    </message>\n"
                                 + "    <message tool=\"javac\" type=\"error\" message=\"not a statement\" file=\"Main.java\" line=\"20\">\n"
                                 + "        <example position=\"8\"/>\n"
                                 + "    </message>\n"
                                 + "</submitResults>\n";
        SvnResultHandler handler = new SvnResultHandler(erromessage);
      
        assertDoesNotThrow(() -> {
            List<Problem> problems;
            problems = handler.parseXmlToProblem();
            
            assertTrue(problems.size() == 2);
            Problem problem1 = new Problem("javac","';' expected",Problem.Severity.ERROR);
            problem1.setFile(new File("Main.java"));
            problem1.setLine(20);
            assertTrue(problem1.equals(problems.get(0)));
            
            Problem problem2 = new Problem("javac","not a statement",Problem.Severity.ERROR);
            problem2.setFile(new File("Main.java"));
            problem2.setLine(20);
            problem2.setColumn(8);
            assertTrue(problem2.equals(problems.get(1)));
            
            
        });
        
        }
   
    @Test
    public void svnPostErrorMessageToStringTest() {
        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED,
                "Warning: post-commit hook failed (exit code 1) with output:\n"
                + "<submitResults>\n"
                + "    <message tool=\"javac\" type=\"error\" message=\"example\" />\n"
                + "</submitResults>\n"
        );
        assertEquals(SvnResultHandler.svnErrorMessageToString(error),
                "\n<submitResults>\n"
                + "    <message tool=\"javac\" type=\"error\" message=\"example\" />\n"
                + "</submitResults>\n");
                
        
    }
    @Test
    public void svnPreErrorMessageToStringTest() {
        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE, "Commit failed (details follow):");
        
        SVNErrorMessage child = SVNErrorMessage.create(SVNErrorCode.REPOS_HOOK_FAILURE,
                "Commit blocked by pre-commit hook (exit code 1) with output:\n"
                + "<submitResults>\n"
                + "    <message tool=\"encoding\" type=\"error\" message=\"invalid encoding\"/>\n"
                + "</submitResults>\n"
        );
        error.setChildErrorMessage(child);
        
        SVNErrorMessage child2 = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED,
                "{0} of ''{1}'': 500 Internal Server Error ({2})", "MERGE", "/some/path/", "http://some.example/");
        child.setChildErrorMessage(child2);
          
        
        assertEquals(SvnResultHandler.svnErrorMessageToString(error),
                "<submitResults>\n"
                + "    <message tool=\"encoding\" type=\"error\" message=\"invalid encoding\"/>\n"
                + "</submitResults>\n");
        
        
      }
       
    
    
}
