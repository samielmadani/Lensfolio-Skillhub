package nz.ac.canterbury.seng302.identityprovider.model;

import org.hibernate.annotations.Cascade;

import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;

import javax.persistence.*;
import java.util.*;

@Entity(name = "UserGroup")
public class Group {
    @Id
    @Column(name = "group_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int groupId;

    @Column(name = "long_name", nullable = false)
    private String longName;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @ManyToMany(fetch = FetchType.EAGER)
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    @JoinTable(name = "grouped_users", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> groupMembers;

    public Group(String s, String l) {
        this.longName = l;
        this.shortName = s;
        this.groupMembers = new HashSet<>();
    }

    public Group() {}

    @Override
    public boolean equals(Object o) {
        if (o instanceof Group) {
            Group group = (Group) o;
            if (group.groupId == this.groupId && Objects.equals(group.shortName, this.shortName) && Objects.equals(group.longName, this.longName)) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return groupId + " | " + longName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, longName, shortName);
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Set<User> getGroupMembers() {
        return this.groupMembers;
    }

    public void removeGroupMember(int userID) {
        User user = null;
        for (User u : groupMembers) {
            if (u.getUserId() == userID) {
                user = u;
            }
        }
        if (user != null) {
            // if (this.getShortName() == "TS") {
            //     user.addRole(UserRole.STUDENT);
            //     user.removeRole(UserRole.TEACHER);
            //     user.removeRole(UserRole.COURSE_ADMINISTRATOR);
            // }
            this.groupMembers.remove(user);
            user.removeGroup(this);
        }
        // System.out.println(user.getRoles() + "aime");
    }

    public void addGroupMember(User user) {
        this.groupMembers.add(user);
        user.addGroup(this);
    }

    public int getGroupId() { return this.groupId; }
}