package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByName (String name);
    Event findById (int id);
    List<Event> findByProjectId(int projectId);
}
