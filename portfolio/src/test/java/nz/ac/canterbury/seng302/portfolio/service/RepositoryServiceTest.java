package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.evidence.RepositoryDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Repo;
import nz.ac.canterbury.seng302.portfolio.model.repositories.RepoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {
    @InjectMocks
    private RepositoryService repositoryService;
    @Mock
    private RepoRepository repoRepository;

    @Test
    void testAddBranchesToRepository_validData () {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsKMP4UwyH", 12294, "My Repository", null);
        assertNotNull(repositoryService.addBranchesToRepository(repoDTO).getBranches());
    }

    @Test
    void testAddBranchesToRepository_invalidAPIKey () {
        RepositoryDTO repoDTO = new RepositoryDTO("", 12294, "My Repository", null);
        assertNull(repositoryService.addBranchesToRepository(repoDTO).getBranches());
    }

    @Test
    void testAddBranchesToRepository_validAPIKey_invalidProjectID () {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsKMP4UwyH", -1, "My Repository", null);
        assertNull(repositoryService.addBranchesToRepository(repoDTO).getBranches());
    }

    @Test
    void testAddBranchesToRepository_invalidAPIKey_invalidProjectID () {
        RepositoryDTO repoDTO = new RepositoryDTO("", -1, "My Repository", null);
        assertNull(repositoryService.addBranchesToRepository(repoDTO).getBranches());
    }

    @Test
    void repoExistsForGroup_repoExists () {
        when(repoRepository.existsById(1)).thenReturn(true);
        assertTrue(repositoryService.repoExistsForGroup(1));
    }

    @Test
    void repoExistsForGroup_repoDoesntExists () {
        when(repoRepository.existsById(1)).thenReturn(false);
        assertFalse(repositoryService.repoExistsForGroup(1));
    }

    @Test
    void repoExistsForGroup_invalidData () {
        when(repoRepository.existsById(-1)).thenReturn(false);
        assertFalse(repositoryService.repoExistsForGroup(-1));
    }

    @Test
    void getRepoForGroup_repoDoesntExist_validGroupId () {
        when(repoRepository.existsById(1)).thenReturn(false);
        when(repoRepository.save(any(Repo.class))).thenReturn(new Repo(1));
        assertEquals(1, repositoryService.getRepoForGroup(1).getId());
    }

    @Test
    void getRepoForGroup_repoDoesntExist_invalidGroupId () {
        when(repoRepository.existsById(-1)).thenReturn(false);
        when(repoRepository.save(any(Repo.class))).thenReturn(null);
        assertNull(repositoryService.getRepoForGroup(-1));
    }

    @Test
    void getRepoForGroup_repoExists () {
        when(repoRepository.existsById(1)).thenReturn(true);
        when(repoRepository.findById(1)).thenReturn(new Repo(1));
        assertEquals(1, repositoryService.getRepoForGroup(1).getId());
        verify(repoRepository, never()).save(any());
    }

    @Test
    void constructRepoDTO_validData () {
        Repo repo = new Repo(1);
        repo.setRepoAPIKey("_8FKAAm4tWVsKMP4UwyH");
        repo.setProjectId(12294);
        assertNotNull(repositoryService.constructRepoDTO(repo).getBranches());
    }

    @Test
    void constructRepoDTO_noAPIKey () {
        Repo repo = new Repo(1);
        repo.setProjectId(12294);
        assertNull(repositoryService.constructRepoDTO(repo).getBranches());
    }

    @Test
    void constructRepoDTO_noProjectID () {
        Repo repo = new Repo(1);
        repo.setRepoAPIKey("_8FKAAm4tWVsKMP4UwyH");
        assertNull(repositoryService.constructRepoDTO(repo).getBranches());
    }

    @Test
    void constructRepoDTO_validData_repoInformationPersists () {
        Repo repo = new Repo(1);
        repo.setRepoAlias("Test Repo");
        repo.setRepoAPIKey("_8FKAAm4tWVsKMP4UwyH");
        repo.setProjectId(12294);
        assertEquals("Test Repo",  repositoryService.constructRepoDTO(repo).getRepoAlias());
    }

    @Test
    void getRepoLinkMessage_validInformation() {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsKMP4UwyH", 12294, "My Repository", new ArrayList<>());
        assertEquals("Linked, no errors", repositoryService.getRepoLinkMessage(repoDTO));
    }

    @Test
    void getRepoLinkMessage_noAPIKey () {
        RepositoryDTO repoDTO = new RepositoryDTO(null, 12294, "My Repository", null);
        assertEquals("Not Linked, no API key", repositoryService.getRepoLinkMessage(repoDTO));
    }

    @Test
    void getRepoLinkMessage_noProjectID() {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsKMP4UwyH", -1, "My Repository", null);
        assertEquals("Not Linked, no Project ID", repositoryService.getRepoLinkMessage(repoDTO));
    }

    @Test
    void getRepoLinkMessage_invalidAPIKey() {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsK", 12294, "My Repository", null);
        assertEquals("Not Linked, invalid API Key", repositoryService.getRepoLinkMessage(repoDTO));
    }

    @Test
    void getRepoLinkMessage_invalidProjectID() {
        RepositoryDTO repoDTO = new RepositoryDTO("_8FKAAm4tWVsKMP4UwyH", 1, "My Repository", null);
        assertEquals("Not Linked, invalid project ID", repositoryService.getRepoLinkMessage(repoDTO));
    }

}
