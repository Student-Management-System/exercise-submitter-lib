package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * The Class SVNResultHandler converts the SVNCommitInfo from XML to Problem.   
 */
public class SVNResultHandler {
    
   
    private String xmlmessage;
    
    /**
     * Instantiates a new SVN result handler.
     *
     * @param xmlmessage the xmlmessage
     */
    public SVNResultHandler(String xmlmessage) {
        this.xmlmessage = xmlmessage;
    }
    
    /**
     * Parses the XML Message to Problem.
     *
     * @return the list of Problems retrieved out the XML message.
     * @throws SAXException Failed parsing.
     * @throws IOException Problem converting string to stream.
     * @throws ParserConfigurationException Failed parsing.
     */
    public List<Problem> parseXmlToProblem() throws SAXException, IOException, ParserConfigurationException {
        List<Problem> problems = new ArrayList<Problem>();
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xmlmessage.getBytes("utf-8"))));
        doc.getDocumentElement().normalize();
        
        NodeList nList = doc.getElementsByTagName("message");
        
        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            
            
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
              
              
                String tool = eElement.getAttribute("tool");
                String type = eElement.getAttribute("type");
                String message = eElement.getAttribute("message");
                //optional
                String file = eElement.getAttribute("file");
                String line = eElement.getAttribute("line");
                
                
                if (tool != null && type != null && message != null) {
                    if (file != null && line != null) {
                        Problem.Severity sev = type.equals("error") ? Problem.Severity.ERROR : Problem.Severity.WARNING;
                        Problem problem = new Problem(tool, message, sev);
                        problem.setFile(new File(file));
                        problem.setLine(Integer.parseInt(line));
                        //column
                        Element column = (Element) eElement.getElementsByTagName("example").item(0);
                        if (column != null) {
                            problem.setColumn(Integer.parseInt(column.getAttribute("position")));
                        }
                        problems.add(problem);
                       
                    } else {
                        Problem.Severity sev = type.equals("error") ? Problem.Severity.ERROR : Problem.Severity.WARNING;
                        Problem problem = new Problem(tool, message, sev);
                        problems.add(problem);
                    }
                } else {
                    //TODO: exception
                }
                
               
            }
            
            
        
        }
        return problems;
    }
}
