package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.Embeddable;
import java.text.SimpleDateFormat;
import java.util.Date;

@Embeddable
public class RepositoryCommit {
    private String evidenceCommitId;
    private String commitName;
    private String usersName;

    private Date commitDate;

    public RepositoryCommit () {}

    public RepositoryCommit (String evidenceCommitId, String commitName, String user, Date commitDate) {
        this.evidenceCommitId = evidenceCommitId;
        this.commitName = commitName;
        this.usersName = user;
        this.commitDate = commitDate;
    }

    public void setEvidenceCommitId (String evidenceCommitId) { this.evidenceCommitId = evidenceCommitId; }

    public String getEvidenceCommitId () {return this.evidenceCommitId;}

    public void setCommitName (String commitName) { this.commitName = commitName;}

    public String getCommitName () { return this.commitName; }

    public void setUsersName (String user) {this.usersName = user;}

    public String getUsersName () {return this.usersName;}

    public void setCommitDate (Date commitDate) {this.commitDate = commitDate;}

    public Date getCommitDate () {return this.commitDate;}

    public String getFormattedDate () {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        return dateFormat.format(commitDate);
    }

    public void setFormattedDate (String date) {}
}
