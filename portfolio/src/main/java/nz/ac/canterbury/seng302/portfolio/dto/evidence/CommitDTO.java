package nz.ac.canterbury.seng302.portfolio.dto.evidence;

import nz.ac.canterbury.seng302.portfolio.model.entities.RepositoryCommit;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommitDTO {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
    private String commitId;
    private String commitAuthor;
    private Date commitDate;
    private String commitName;

    public CommitDTO () {}

    public CommitDTO (String commitId, String commitAuthor, Date commitDate, String commitName) {
        this.commitId = commitId;
        this.commitAuthor = commitAuthor;
        this.commitDate = commitDate;
        this.commitName = commitName;
    }

    public CommitDTO(RepositoryCommit commit) {
        this.commitId = commit.getEvidenceCommitId();
        this.commitAuthor = commit.getUsersName();
        this.commitDate = commit.getCommitDate();
        this.commitName = commit.getCommitName();
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public void setCommitAuthor(String commitAuthor) {
        this.commitAuthor = commitAuthor;
    }

    public String getCommitDate() {
        return dateFormat.format(commitDate);
    }

    public void setCommitDate(Date commitDate) {
        this.commitDate = commitDate;
    }

    public String getCommitName() {
        return commitName;
    }

    public void setCommitName(String commitName) {
        this.commitName = commitName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CommitDTO other)) return false;
        return other.getCommitId().equals(this.commitId);
    }
}
