package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.model.keys.ProjectGroupKey;

import javax.persistence.*;
import java.util.Objects;

@Entity(name = "ProjectGroup")
@IdClass(ProjectGroupKey.class)
public class ProjectGroup {
    @Id
    private int groupId;

    @Id
    private int projectId;

    public ProjectGroup(int projectId, int groupId) {
        this.groupId = groupId;
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectGroup that = (ProjectGroup) o;
        return groupId == that.groupId && projectId == that.projectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, projectId);
    }

    protected ProjectGroup() {}


    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
}
