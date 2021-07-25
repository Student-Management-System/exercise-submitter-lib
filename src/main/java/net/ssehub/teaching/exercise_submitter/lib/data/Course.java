package net.ssehub.teaching.exercise_submitter.lib.data;



/**
 * The Class Course.
 */
public class Course {

  
   
    private String name;
    
    private String id;
    
    /**
     * Instantiates a new course.
     *
     * @param name the name
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
    
    public String getId() {
        return id;
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
