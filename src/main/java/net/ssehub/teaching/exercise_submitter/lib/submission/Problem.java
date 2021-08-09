package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.File;
import java.util.Optional;

/**
 * This class poses a problem that comes back from the stumgmt when uploading an assignment.
 */
public class Problem {
    
    /**
     * Indicates the severity of a problem.
     */
    public enum Severity {
        
      
        WARNING,
        
        
        ERROR;
    }
    
 
    private String checkName;
    

    private String message;
    
  
    private Severity severity;


    private Optional<File> file = Optional.empty();
    

    private Optional<Integer> line = Optional.empty();
    

    private Optional<Integer> column = Optional.empty();
    
    /**
     * Instantiates a new problem.
     *
     * @param checkName the check name
     * @param message the message
     * @param severity the severity
     */
    Problem(String checkName, String message, Severity severity) {
        this.checkName = checkName;
        this.message = message;
        this.severity = severity;
    }

    /**
     * The name of the check that detected this problem.
     * 
     * @return The name of the check.
     */
    public String getCheckName() {
        return checkName;
    }
    
    
    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the severity.
     *
     * @return the severity
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * The affected file in the submission. Relative to the submission directory.
     * 
     * @return The affected file.
     */
    public Optional<File> getFile() {
        return file;
    }
    
    
    /**
     * Sets the file.
     *
     * @param file the new file
     */
    void setFile(File file) {
        this.file = Optional.of(file);
    }
    
    /**
     * Gets the line.
     *
     * @return the line
     */
    public Optional<Integer> getLine() {
        return line;
    }
    
    
    /**
     * Sets the line.
     *
     * @param line the new line
     */
    void setLine(int line) {
        this.line = Optional.of(line);
    }
    
    /**
     * Gets the column.
     *
     * @return the column
     */
    public Optional<Integer> getColumn() {
        return column;
    }
    
    
    /**
     * Sets the column.
     *
     * @param column the new column
     */
    void setColumn(int column) {
        this.column = Optional.of(column);
    }
    
}
