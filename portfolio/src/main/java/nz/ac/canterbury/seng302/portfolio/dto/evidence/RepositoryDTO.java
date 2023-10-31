package nz.ac.canterbury.seng302.portfolio.dto.evidence;

import java.util.List;

public class RepositoryDTO {
    private String repoAPIKey;
    private int projectId;
    private String repoAlias;
    private List<BranchDTO> branches;

    public RepositoryDTO () {}

    public RepositoryDTO (String repoAPIKey, int projectId, String repoAlias, List<BranchDTO> branches) {
        this.repoAPIKey = repoAPIKey;
        this.projectId = projectId;
        this.repoAlias = repoAlias;
        this.branches = branches;
    }

    public String getRepoAPIKey() {
        return repoAPIKey;
    }

    public void setRepoAPIKey(String repoAPIKey) {
        this.repoAPIKey = repoAPIKey;
    }

    public int getProjectId() {return projectId;}

    public void setProjectId(int projectId) {this.projectId = projectId;}

    public String getRepoAlias() {
        return repoAlias;
    }

    public void setRepoAlias(String repoAlias) {
        this.repoAlias = repoAlias;
    }

    public List<BranchDTO> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchDTO> branches) {
        this.branches = branches;
    }
}
