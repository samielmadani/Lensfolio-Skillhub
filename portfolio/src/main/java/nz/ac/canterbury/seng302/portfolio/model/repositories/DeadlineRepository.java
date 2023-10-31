package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, Integer> {
    Deadline findById (int id);
    List<Deadline> findByProjectId (int projectId);
}
