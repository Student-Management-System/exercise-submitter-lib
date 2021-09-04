package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * The Class SVNResultHandler converts the SVNCommitInfo from XML to Problem.
 * 
 * @author Lukas
 * @author Adam
 */
public class SvnResultHandler {
    
    private static final String BLOCKED_BY_PRE_COMMIT_PREFIX
        = "Commit blocked by pre-commit hook (exit code 1) with output:\n";
    
    private static final DocumentBuilderFactory XML_BUILDER_FACTORY;
    
    private String xmlmessage;
    
    static {
        SchemaFactory schemaFactory = SchemaFactory.newDefaultInstance();
        try {
            Schema schema = schemaFactory.newSchema(SvnResultHandler.class.getResource("messageSchema.xsd"));
            XML_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
            XML_BUILDER_FACTORY.setSchema(schema);
            
        } catch (SAXException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Instantiates a new SVN result handler.
     *
     * @param xmlmessage the xmlmessage
     */
    public SvnResultHandler(String xmlmessage) {
        this.xmlmessage = xmlmessage;
    }
    /**
     * 
     * Collects the errors thrown by the XML parser.
     *
     */
    private static class ErrorCollector implements ErrorHandler {

        private List<SAXParseException> exceptions = new LinkedList<>();
        
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            // ignore warnings
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }
        
    }
    
    /**
     * Parses the XML Message to Problem.
     *
     * @return the list of Problems retrieved out the XML message.
     * @throws SAXException Failed parsing.
     * @throws IOException Problem converting string to stream.
     * @throws ParserConfigurationException If creating the XML parser fails.
     */
    public List<Problem> parseXmlToProblem() throws SAXException, IOException, ParserConfigurationException {
        List<Problem> problems = new ArrayList<Problem>();
        
        DocumentBuilder dBuilder = XML_BUILDER_FACTORY.newDocumentBuilder();
        ErrorCollector errorCollector = new ErrorCollector();
        dBuilder.setErrorHandler(errorCollector);
        Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xmlmessage.getBytes("utf-8"))));
        
        if (!errorCollector.exceptions.isEmpty()) {
            throw errorCollector.exceptions.get(0);
        }
        
        doc.getDocumentElement().normalize();
        
        
        NodeList nList = doc.getElementsByTagName("message");
        
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            
            Element eElement = (Element) nNode;
          
            String tool = eElement.getAttribute("tool");
            String type = eElement.getAttribute("type");
            String message = eElement.getAttribute("message");
            String file = eElement.getAttribute("file");
            String line = eElement.getAttribute("line");
            
            Problem.Severity sev = type.equals("error") ? Problem.Severity.ERROR : Problem.Severity.WARNING;
            Problem problem = new Problem(tool, message, sev);
            
            if (!file.isEmpty()) {
                problem.setFile(new File(file));
                
                if (!line.isEmpty()) {
                    problem.setLine(Integer.parseInt(line));
                    
                    Optional<Element> column = Optional.ofNullable(
                            (Element) eElement.getElementsByTagName("example").item(0));
                    
                    column
                            .map(e -> e.getAttribute("position"))
                            .map(Integer::parseInt)
                            .ifPresent(problem::setColumn);
                }
            }
            
            problems.add(problem);
        
        }
        return problems;
    }
    
    /**
     * Convert the SvnErrormessage to a String.
     *
     * @param message errormessage
     * @return String which is the converted errormessage
     */
    public static String svnErrorMessageToString(SVNErrorMessage message) {
        String result = "";
        if (message.getErrorCode().equals(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED)) {
            String messageString = message.getMessageTemplate();
            int pos = messageString.indexOf('\n');
            if (pos > 0) {
                messageString = messageString.substring(pos);
            }
            result = messageString;
            
        } else {
            do {
                String messageString = message.getMessageTemplate();
                if (messageString != null && messageString.startsWith(BLOCKED_BY_PRE_COMMIT_PREFIX)) {
                    messageString = messageString.substring(BLOCKED_BY_PRE_COMMIT_PREFIX.length());
                    result = messageString;
                    break;
                }
                message = message.getChildErrorMessage();
            } while (message != null);
        }
        return result;
       
    }
}
