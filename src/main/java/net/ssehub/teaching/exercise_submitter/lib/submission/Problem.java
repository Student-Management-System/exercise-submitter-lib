package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.File;
import java.util.Objects;
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
     * @param message   the message
     * @param severity  the severity
     */
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
    public void setFile(File file) {
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
    public void setLine(int line) {
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
     * @param column the new column
     */
    public void setColumn(int column) {
        this.column = Optional.of(column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkName, column, file, line, message, severity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Problem)) {
            return false;
        }
        Problem other = (Problem) obj;
        return Objects.equals(checkName, other.checkName) && Objects.equals(column, other.column)
                && Objects.equals(file, other.file) && Objects.equals(line, other.line)
                && Objects.equals(message, other.message) && severity == other.severity;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Problem [checkName=");
        builder.append(checkName);
        builder.append(", message=");
        builder.append(message);
        builder.append(", severity=");
        builder.append(severity);
        builder.append(", file=");
        builder.append(file);
        builder.append(", line=");
        builder.append(line);
        builder.append(", column=");
        builder.append(column);
        builder.append("]");
        return builder.toString();
    }

}
