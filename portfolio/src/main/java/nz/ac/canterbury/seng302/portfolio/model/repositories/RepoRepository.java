package nz.ac.canterbury.seng302.portfolio.model.repositories;

import nz.ac.canterbury.seng302.portfolio.model.entities.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepoRepository extends JpaRepository<Repo, Integer> {
    Repo findById (int id);
}
