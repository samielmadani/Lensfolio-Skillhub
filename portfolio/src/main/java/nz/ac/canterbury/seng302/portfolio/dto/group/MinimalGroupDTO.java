package nz.ac.canterbury.seng302.portfolio.dto.group;

public class MinimalGroupDTO {
    private String groupName;
    private int groupId;

    public MinimalGroupDTO () {}

    public MinimalGroupDTO (String groupName, int groupId) {
        this.groupId = groupId;
        this.groupName = groupName;
    }


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
}
