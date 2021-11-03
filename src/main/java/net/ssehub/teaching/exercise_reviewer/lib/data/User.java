package net.ssehub.teaching.exercise_reviewer.lib.data;

import java.util.Optional;

/**
 * The Class User.
 * 
 * @author lukas
 */
public class User {
    
    /** The user id. */
    private String userId;
    private String username;
    private String displayname;
    private String role;
    
    /** The group. */
    private Optional<Group> group;
    
    /**
     * Instantiates a new user.
     *
     * @param userId the user id
     * @param username the username
     * @param displayname the displayname
     * @param role the role
     */
    public User(String userId, String username, String displayname, String role) {
        this.userId = userId;
        this.username = username;
        this.displayname = displayname;
        this.role = role;
    }
    
    /**
     * With group.
     *
     * @param group the group
     * @return the user
     */
    public User withGroup(Group group) {
        this.group = Optional.ofNullable(group);
        return this;
    }

    /**
     * Gets the user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the displayname.
     *
     * @return the displayname
     */
    public String getDisplayname() {
        return displayname;
    }

    /**
     * Gets the role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * Gets the group.
     *
     * @return the group
     */
    public Optional<Group> getGroup() {
        return group;
    }
    
    
    

}
