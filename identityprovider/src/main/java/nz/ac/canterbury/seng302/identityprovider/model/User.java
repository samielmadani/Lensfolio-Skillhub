package nz.ac.canterbury.seng302.identityprovider.model;

import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;

import javax.persistence.*;
import java.util.*;

@Entity(name = "User")
@Table(name = "User")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int userId; //unique ID for each user

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "username", nullable = false, unique = true)
    private String username; //username for each user

    @Column(name = "role", nullable = false)
    private String role; //role that the user is, e.g Student, Teacher etc.

    @ManyToMany(mappedBy = "groupMembers", fetch = FetchType.EAGER)
    private Set<Group> groups;
    private String firstName; //name of the user
    private String middleName;
    private String lastName;
    private String nickname;

    private String bio;
    private String pronouns;
    @Lob
    private byte[] image;

    /**
     * Minimum definition of a user
     */
    public User(String email, String password, UserRole role) {
        this.email = email;
        this.username = email;
        this.password = password;
        this.role = String.valueOf(role.getNumber());
        this.groups = new HashSet<>();
    }

    /**
     * Full definition of a user
     */
    public User(String email, String password,
                UserRole role, String userName,
                String firstName, String middleName,
                String lastName, String nickname,
                String bio, String pronouns) {
        this.username = userName;
        this.role = String.valueOf(role.getNumber());
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.bio = bio;
        this.pronouns = pronouns;
        this.groups = new HashSet<>();
    }

    // For @Entity
    protected User() {}

    public Boolean isTeacher () {return role.contains("1");}

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", nickname='" + nickname + '\'' +
                ", bio='" + bio + '\'' +
                ", pronouns='" + pronouns + '\'' +
                ", groups='" + groups + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof User) {
            User user = (User) o;
            if (user.getUserId() == this.getUserId() && Objects.equals(user.getEmail(), this.getEmail())) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, username);
    }

    // Getters + Setters

    public int getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public String getRoles() {
        return this.role;
    }

    public void addRole(UserRole role) {
        this.role += String.valueOf(role.getNumber());
        //Sort by highest role
        char[] arr = this.role.toCharArray();
        Arrays.sort(arr);
        this.role = new StringBuilder(String.valueOf(arr)).reverse().toString();
    }

    public void removeRole(UserRole role) {this.role = this.role.replace(String.valueOf(role.getNumber()), "");}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public void addGroup(Group group) {
        this.groups.add(group);
    }

    public void removeGroup(Group group) {
        this.groups.remove(group);
    }

    public Set<Group> getGroups() { return Set.copyOf(this.groups); }

    public byte[] getImage () {return this.image;}

    public void setImage (byte[] image) {this.image = image;}
}
