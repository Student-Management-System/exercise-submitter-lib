package net.ssehub.teaching.exercise_submitter.lib.data;



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
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     *Converts this Object to string.
     *
     * @return The String
     */
    @Override
    public String toString() {
        return name;
    }
    
}
