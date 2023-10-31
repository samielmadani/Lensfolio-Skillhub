package nz.ac.canterbury.seng302.portfolio.dto.evidence;

import java.util.List;

public class BranchDTO {
    private String branchId;
    private String name;
    private List<CommitDTO> branchCommits;

    public BranchDTO () {}

    public BranchDTO (String branchId, String name, List<CommitDTO> branchCommits) {
        this.branchId = branchId;
        this.name = name;
        this.branchCommits = branchCommits;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CommitDTO> getBranchCommits() {
        return branchCommits;
    }

    public void setBranchCommits(List<CommitDTO> branchCommits) {
        this.branchCommits = branchCommits;
    }
}
