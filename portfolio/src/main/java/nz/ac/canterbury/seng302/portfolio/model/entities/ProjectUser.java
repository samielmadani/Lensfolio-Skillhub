package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.model.keys.ProjectUserKey;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * Entity representing the Project_User table. This table links projects to userIds.
 */
@Entity(name="ProjectUser")
@IdClass(ProjectUserKey.class)
public class ProjectUser {
    // The ID is a composite key of the ProjectId, and UserId. This combination should be unique.
    @Id
    private int projectId;
    @Id
    private int userId;

    public ProjectUser() {}

    public ProjectUser(int projectId, int userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    public int getProjectId () {return this.projectId;}

    public int getUserId () {return this.userId;}
}
