package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.keys.ProjectGroupKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, ProjectGroupKey> {

    List<ProjectGroup> findProjectGroupsByProjectId(int projectId);

    List<ProjectGroup> findProjectGroupsByGroupId(int groupId);

    List<ProjectGroup> findProjectGroupByProjectIdAndGroupId(int projectId, int groupId);

}
