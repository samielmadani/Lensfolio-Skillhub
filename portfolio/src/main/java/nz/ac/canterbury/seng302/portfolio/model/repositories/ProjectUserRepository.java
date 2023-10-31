package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectUser;
import nz.ac.canterbury.seng302.portfolio.model.keys.ProjectUserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserKey> {
    List<ProjectUser> findByUserId (int userId);
    List<ProjectUser> findByProjectId (int projectId);
    List<ProjectUser> findByProjectIdAndUserId(int projectId, int userId);
    void deleteByProjectIdAndUserId(int projectId, int userId);
}
