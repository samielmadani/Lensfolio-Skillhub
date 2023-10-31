package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "Repo")
public class Repo {
    @Id
    @Column(name="id")
    private int id;
    private String repoAPIKey;
    private int projectId;
    private String repoAlias;

    public Repo (int groupId) {
        this.id = groupId;
        this.repoAlias = "My Repository";
        this.projectId = -1;
    }
    protected Repo () {}

    public int getId() {
        return id;
    }

    public void setId(int groupId) {
        this.id = groupId;
    }

    public String getRepoAPIKey() {
        return repoAPIKey;
    }

    public void setRepoAPIKey(String repoAPIKey) {
        this.repoAPIKey = repoAPIKey;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getRepoAlias() {
        return repoAlias;
    }

    public void setRepoAlias(String repoAlias) {
        this.repoAlias = repoAlias;
    }
}
