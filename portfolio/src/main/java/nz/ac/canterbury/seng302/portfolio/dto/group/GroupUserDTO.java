package nz.ac.canterbury.seng302.portfolio.dto.group;

public class GroupUserDTO {
    public GroupUserDTO(int userId, int groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "GroupUserDTO{" +
                "userId=" + userId +
                ", groupId=" + groupId +
                '}';
    }

    private int userId;

    private int groupId;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
