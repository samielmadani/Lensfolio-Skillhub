package nz.ac.canterbury.seng302.identityprovider.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    List<Group> findByLongName(String name);
    List<Group> findByShortName(String name);
    Group findById(int id);
}