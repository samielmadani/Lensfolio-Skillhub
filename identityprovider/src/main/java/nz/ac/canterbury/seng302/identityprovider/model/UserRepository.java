package nz.ac.canterbury.seng302.identityprovider.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findById(int id);
    User findByEmail(String email);
    User findByUsername(String username);
    @Query("SELECT u FROM User u WHERE CONCAT(LOWER(u.username), ' ', LOWER(u.firstName), ' ', LOWER(u.lastName)) LIKE %?1%")
    Page<User> search (String query, Pageable pageable);
}
