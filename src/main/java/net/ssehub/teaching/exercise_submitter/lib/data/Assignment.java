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
        
        INVISIBLE,
        
        SUBMISSION,
      
        IN_REVIEW,
       
        REVIEWED,
        
        CLOSED;
    }
    
    private String managementId;
    
    private String name;
   
    private State state;
    
    private MaxPoints maxPoints;
   
    private boolean isGroupWork;

    /**
     * Instantiates a new assignment.
     *
     * @param managementId The ID of this assignment in the management system.
     * @param name The name of the assignment
     * @param state The State of the Assignment
     * @param isGroupWork the is group work
     */
    public Assignment(String managementId, String name, State state, boolean isGroupWork) {
        this.managementId = managementId;
        this.name = name;
        this.state = state;
        this.isGroupWork = isGroupWork;
        this.maxPoints = new MaxPoints(0, 0);
    }
    
    /**
     * Instantiates a new assignment.
     * 
     * @param managementId
     * @param name
     * @param state
     * @param isGroupWork
     * @param maxPoints
     */
    public Assignment(String managementId, String name, State state, boolean isGroupWork, 
            MaxPoints maxPoints) {
        this.managementId = managementId;
        this.name = name;
        this.state = state;
        this.isGroupWork = isGroupWork;
        this.maxPoints = maxPoints;
       
    }
    
    
    /**
     * Nested class for handling the max and bonuspoints for an assignment.
     * 
     * @author lukas
     *
     */
    public static class MaxPoints {
        private double maxPoints;
        private double bonusPoints;
        
        /**
         * Instantiates a new class from maxpoints.
         * 
         * @param maxPoints
         * @param bonusPoints
         */
        public MaxPoints(double maxPoints, double bonusPoints) {
            this.maxPoints = maxPoints;
            this.bonusPoints = bonusPoints;
        }
        /**
         * Gets the maximal points for an assignment.
         * 
         * @return double , maxpoints
         */
        public double getMaxPoints() {
            return maxPoints;
        }
        
        /**
         * Gets the maximal bonuspoints for an assignment.
         * 
         * @return double , bonusmaxpoints
         */
        public double getBonusPoints() {
            return bonusPoints;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(bonusPoints, maxPoints);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            
            MaxPoints other = (MaxPoints) obj;
            return Double.doubleToLongBits(bonusPoints) == Double.doubleToLongBits(other.bonusPoints)
                    && Double.doubleToLongBits(maxPoints) == Double.doubleToLongBits(other.maxPoints);
        }
        
        @Override
        public String toString() {
            return "MaxPoints [maxPoints=" + maxPoints + ", bonusPoints=" + bonusPoints + "]";
        }
       
    }
    
    
    /**
     * The ID of this assignment in the student-management system.
     * 
     * @return The ID of this assignment.
     */
    public String getManagementId() {
        return managementId;
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
     * Gets the max points allowed from the assignment.
     * 
     * @return double , maxpoints
     */
    public double getMaxPoints() {
        return this.maxPoints.getMaxPoints();
    }
    
    /**
     * Gets the bonuspoints allowed from the assignment.
     * 
     * @return double , bonuspoints
     */
    public double getBonusPoints() {
        return this.maxPoints.getBonusPoints();
    }
    
    /**
     * Returns if it is GroupWork.
     *
     * @return true, if it is group work
     */
    public boolean isGroupWork() {
        return isGroupWork;
    }
   
    @Override
    public int hashCode() {
        return Objects.hash(isGroupWork, managementId, maxPoints, name, state);
    }
   
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Assignment other = (Assignment) obj;
        return isGroupWork == other.isGroupWork && Objects.equals(managementId, other.managementId)
                && Objects.equals(maxPoints, other.maxPoints) && Objects.equals(name, other.name)
                && state == other.state;
    }

    @Override
    public String toString() {
        return "Assignment [managementId=" + managementId 
                + ", name=" + name 
                + ", state=" + state 
                + ", maxPoints=" + maxPoints 
                + ", isGroupWork=" + isGroupWork + "]";
    }
    
}
