package net.ssehub.teaching.exercise_submitter.lib.data;

import java.util.Objects;




/**
 * Assignments for Students.
 * 
 */
public class Assignment {

    
    /**
     * The state of an Assignment.
     */
    public enum State {
        
       
        /** The invisible. */
        INVISIBLE,
        
      
        /** The submission. */
        SUBMISSION,
        
      
        /** The in review. */
        IN_REVIEW,
        
       
        /** The reviewed. */
        REVIEWED;
    }
    
   
    
    private String name;
    
  
   
    private State state;
    
    
   
    private boolean isGroupWork;

    /**
     * Instantiates a new assignment.
     *
     * @param name The name of the assignment
     * @param state The State of the Assignment
     * @param isGroupWork the is group work
     */
    public Assignment(String name, State state, boolean isGroupWork) {
        this.name = name;
        this.state = state;
        this.isGroupWork = isGroupWork;
    }
    
    
    /**
     * Gets the human-readable name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    
    /**
     * Gets the state.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }
    
    /**
     * Returns if it is GroupWork.
     *
     * @return true, if it is group work
     */
    public boolean isGroupWork() {
        return isGroupWork;
    }

   
    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return Objects.hash(isGroupWork, name, state);
    }

   
    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Assignment)) {
            return false;
        }
        Assignment other = (Assignment) obj;
        return isGroupWork == other.isGroupWork && Objects.equals(name, other.name) && state == other.state;
    }

    /**
     * Converts this Object to string.
     *
     * @return The string
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Assignment [name=");
        builder.append(name);
        builder.append(", state=");
        builder.append(state);
        builder.append(", isGroupWork=");
        builder.append(isGroupWork);
        builder.append("]");
        return builder.toString();
    }
    
}
