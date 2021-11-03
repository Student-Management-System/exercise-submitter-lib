package net.ssehub.teaching.exercise_reviewer.lib.data;

import java.util.List;

/**
 * This class represents a group of user which submitted to the server in
 * a collaboration project.
 * 
 * @author lukas
 *
 */
public class Group {
    private String groupId;
    private String name;
    private List<User> members;

    /**
     * Instantiates a new group.
     *
     * @param groupId the group id
     * @param name the name
     * @param members the members
     */
    public Group(String groupId, String name, List<User> members) {
        this.groupId = groupId;
        this.name = name;
        this.members = members;
    }

    /**
     * Gets the group id.
     *
     * @return the group id
     */
    public String getGroupId() {
        return groupId;
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
     * Gets the members.
     *
     * @return the members
     */
    public List<User> getMembers() {
        return members;
    }
}
