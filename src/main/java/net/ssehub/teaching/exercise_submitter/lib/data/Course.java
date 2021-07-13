package net.ssehub.teaching.exercise_submitter.lib.data;


public class Course {

    private String name;
    
    public Course(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
