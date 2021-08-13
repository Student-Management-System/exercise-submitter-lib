package net.ssehub.teaching.exercise_submitter.lib.submission;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;



public class SVNResultHandlerTest {
    
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
        SVNResultHandler handler = new SVNResultHandler(erromessage);
      
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
       
    
    
}
