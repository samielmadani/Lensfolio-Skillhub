package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.entities.EvidenceSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceSkillRepository extends JpaRepository<EvidenceSkill, Integer> {

    List<EvidenceSkill> findByEvidence(Evidence evidence);

    EvidenceSkill findById(int id);

    List<EvidenceSkill> findBySkillName(String skillName);
    @Query("SELECT skill FROM EvidenceSkill skill WHERE LOWER(skill.skillName) LIKE %?1%")
    List<EvidenceSkill> search (String query);
}
