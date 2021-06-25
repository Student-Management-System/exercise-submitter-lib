package net.ssehub.teaching.exercise_submitter.lib;


public class Assignment {

    public enum State {
        INVISIBLE,
        SUBMISSION,
        IN_REVIEW,
        REVIEWED;
    }
    
    private String name;
    
    private State state;
    
    private boolean isGroupWork;

    public Assignment(String name, State state, boolean isGroupWork) {
        this.name = name;
        this.state = state;
        this.isGroupWork = isGroupWork;
    }
    
    
    public String getName() {
        return name;
    }
    
    
    public State getState() {
        return state;
    }
    
    public boolean isGroupWork() {
        return isGroupWork;
    }
    
}
