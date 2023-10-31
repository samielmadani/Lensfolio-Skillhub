package nz.ac.canterbury.seng302.portfolio.model.keys;

import java.io.Serializable;
import java.util.Objects;

public class ProjectGroupKey implements Serializable {
    private int groupId;

    private int projectId;

    public ProjectGroupKey() {}

    public ProjectGroupKey(int groupId, int projectId) {
        this.groupId = groupId;
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectGroupKey that = (ProjectGroupKey) o;
        return groupId == that.groupId && projectId == that.projectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, projectId);
    }
}
