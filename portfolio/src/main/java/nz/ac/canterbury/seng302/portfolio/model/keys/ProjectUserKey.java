package nz.ac.canterbury.seng302.portfolio.model.keys;

import java.io.Serializable;
import java.util.Objects;

/**
 * ProjectUser composite key. This is used to allow JPA to create a primary composite key using the project and user ids.
 */
public class ProjectUserKey implements Serializable {
    private int projectId;
    private int userId;

    public ProjectUserKey() {}

    public ProjectUserKey(int projectId, int userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectUserKey that = (ProjectUserKey) o;
        return projectId == that.projectId && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}
