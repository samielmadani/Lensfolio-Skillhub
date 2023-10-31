package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.UserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStateRepository extends JpaRepository<UserState, Long> {
    UserState findByUserId(int id);
    UserState findByStateId(long id);
}
