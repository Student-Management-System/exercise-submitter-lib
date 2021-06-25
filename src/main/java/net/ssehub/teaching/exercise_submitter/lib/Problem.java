package net.ssehub.teaching.exercise_submitter.lib;

import java.io.File;
import java.util.Optional;

public class Problem {
    
    private String checkName;
    
    private String message;
    
    private Severity severity;

    private Optional<File> file = Optional.empty();
    
    private Optional<Integer> line = Optional.empty();
    
    private Optional<Integer> column = Optional.empty();
    
    public Problem(String checkName, String message, Severity severity) {
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
    
    
    public String getMessage() {
        return message;
    }
    
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
    
    
    public void setFile(File file) {
        this.file = Optional.of(file);
    }
    
    public Optional<Integer> getLine() {
        return line;
    }
    
    
    public void setLine(int line) {
        this.line = Optional.of(line);
    }
    
    public Optional<Integer> getColumn() {
        return column;
    }
    
    
    public void setColumn(int column) {
        this.column = Optional.of(column);
    }
    
}
