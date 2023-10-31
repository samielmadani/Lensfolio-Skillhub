package nz.ac.canterbury.seng302.portfolio.dto.user;

import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;

import java.util.ArrayList;
import java.util.List;

public class UserDTO {
    private int id;
    private String name;
    private List<String> roles = new ArrayList<>();
    private String username;
    private String nickname;
    private boolean isDefault;
    private boolean inGroup = false;
    private int groupId;

    public UserDTO () {}

    public UserDTO (String name, List<String> roles) {
        this.name = name;
        this.roles = roles;
    }

    public UserDTO(String name, String username, int id, List<UserRole> roles) {
        this.name = name;
        this.username = username;
        this.isDefault = username.equals("admin200") || username.equals("teacher200") || username.equals("student200");
        this.id = id;
        this.addRoles(roles);
    }

    public UserDTO(UserResponse userResponse) {
        this.name = userResponse.getFirstName() +  " " + userResponse.getLastName();
        this.username = userResponse.getUsername();
        this.isDefault = username.equals("admin200") || username.equals("teacher200") || username.equals("student200");
        this.id = userResponse.getId();
        this.addRoles(userResponse.getRolesList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRolesString() {
        return roles;
    }

    public List<UserRole> getRoles() {
        List<UserRole> userRoles = new ArrayList<>();

        for (String role : roles) {
            userRoles.add(UserService.convertStringToRole(role));
        }
        return userRoles;
    }

    public void addRole(UserRole role) {
        this.roles.add(UserService.convertRoleToString(role));
    }

    public void addRoles(List<UserRole> roles) {
        for (UserRole role : roles) {
            this.roles.add(UserService.convertRoleToString(role));
        }
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) {
        this.username = username;
        this.isDefault = username.equals("admin200") || username.equals("teacher200") || username.equals("student200");
    }

    public boolean isInGroup() {
        return inGroup;
    }

    public void setInGroup(boolean inGroup) {
        this.inGroup = inGroup;
    }

    public int getGroupId () {return groupId;}

    public void setGroupId (int groupId) {this.groupId = groupId;}

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof UserDTO other)) return false;
        return other.getId() == this.getId();
    }
}
