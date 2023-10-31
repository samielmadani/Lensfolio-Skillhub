package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// the entity type and ID this works with are specified in the signature
@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    Project findById(int id);
    List<Project> findByIsDefaultProject(boolean isDefault);
}
