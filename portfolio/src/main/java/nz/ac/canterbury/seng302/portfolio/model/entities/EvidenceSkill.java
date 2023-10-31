package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class EvidenceSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String skillName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;


    public EvidenceSkill(Evidence evidence, String skillName) {
        this.evidence = evidence;
        this.skillName = skillName;
    }

    public EvidenceSkill() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvidenceSkill that = (EvidenceSkill) o;
        return Objects.equals(skillName.toUpperCase(), that.skillName.toUpperCase()) && Objects.equals(evidence, that.evidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, skillName, evidence);
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String tagName) {
        this.skillName = tagName;
    }
}
