package nz.ac.canterbury.seng302.portfolio.dto.group;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;

import java.util.List;

public class GroupDTO {
    private GroupDetailsResponse group;
    private boolean canEdit;
    private boolean isDefault;
    private boolean isAdmin;
    private int pages;
    private boolean isUserCourseAdmin;
    private List<UserDTO> users;

    public GroupDTO (GroupDetailsResponse group, boolean canEdit, boolean isDefault, boolean isAdmin, int pages,
                     boolean userCourseAdmin, List<UserDTO> users) {
        this.group = group;
        this.canEdit = canEdit;
        this.isDefault = isDefault;
        this.isAdmin = isAdmin;
        this.pages = pages;
        this.isUserCourseAdmin = userCourseAdmin;
        this.users = users;
    }

    public GroupDTO () {}

    public GroupDetailsResponse getGroup() {
        return group;
    }

    public void setGroup(GroupDetailsResponse group) {
        this.group = group;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public void setUserCourseAdmin (boolean userCourseAdmin) {
        this.isUserCourseAdmin = userCourseAdmin;
    }

    public boolean getUserIsCourseAdmin () {
        return isUserCourseAdmin;
    }

    public List<UserDTO> getUsers () {return this.users;}

    public void setUsers (List<UserDTO> users) {this.users = users;}
}
