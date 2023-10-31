package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class EvidenceUser {

    @Column(name="EVIDENCE_USER_ID")
    private int evidenceUserId;
    private String usersName;

    public EvidenceUser () {}

    public EvidenceUser (int evidenceUserId, String usersName) {
        this.evidenceUserId = evidenceUserId;
        this.usersName = usersName;
    }

    public void setEvidenceUserId (int userId) { this.evidenceUserId = userId; }

    public int getEvidenceUserId () {return evidenceUserId;}

    public void setUsersName (String usersName ) { this.usersName = usersName; }

    public String getUsersName () { return usersName; }
}
