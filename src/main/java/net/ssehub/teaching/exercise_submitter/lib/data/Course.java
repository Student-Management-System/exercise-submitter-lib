package net.ssehub.teaching.exercise_submitter.lib.data;



/**
 * The Class Course.
 */
public class Course {

  
   
    private String name;
    
    /**
     * Instantiates a new course.
     *
     * @param name the name
     */
    public Course(String name) {
        this.name = name;
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
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return name;
    }
    
}
