package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Integer> {
    Evidence findById (int id);
    List<Evidence> findByUserId (int userId);
    List<Evidence> findByCategoriesAndUserId (String category, int userId);
    List<Evidence> findByCategories (String category);
    List<Evidence> findByUserIdAndProjectId (int userId, int projectId);
}
