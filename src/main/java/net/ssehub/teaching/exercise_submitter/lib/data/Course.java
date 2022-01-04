package net.ssehub.teaching.exercise_submitter.lib.data;

import java.util.Objects;

/**
 * Represents a course.
 */
public class Course {
   
    private String name;
    
    private String id;
    
    /**
     * Instantiates a new course.
     *
     * @param name The human-readable name of the course.
     * @param id The student-management ID of the course. Typically in the format <code>shortname-semester</code>,
     *      e.g. <code>java-wise2021</code>.
     */
    public Course(String name, String id) {
        this.name = name;
        this.id = id;
    }
    
    /**
     * Gets the human-readable name of the course.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the student-management ID of this course. Typically in the format <code>shortname-semester</code>,
     * e.g. <code>java-wise2021</code>.
     * 
     * @return The ID of this course.
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Course)) {
            return false;
        }
        Course other = (Course) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }
    
}
