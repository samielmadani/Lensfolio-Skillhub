package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.evidence.EvidenceDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.entities.EvidenceSkill;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EvidenceRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EvidenceSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {
    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceSkillRepository evidenceSkillRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;
    @Spy
    @InjectMocks
    private EvidenceService evidenceService;

    @Test
    void getEvidenceById_validId () {
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        Mockito.when(evidenceRepository.existsById(1)).thenReturn(true);
        Mockito.when(evidenceRepository.findById(1)).thenReturn(e);
        assertNotNull(evidenceService.getEvidenceById(1));
    }

    @Test
    void getEvidenceById_invalidId () {
        Mockito.when(evidenceRepository.existsById(1)).thenReturn(false);
        assertNull(evidenceService.getEvidenceById(1));
        verify(evidenceRepository, never()).findById(Mockito.any(Integer.class));
    }

    @Test
    void getEvidenceByUser_validUser_evidenceExists () {
        ArrayList<Evidence> expected = new ArrayList<>();
        expected.add(new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username"));
        Mockito.when(evidenceRepository.findByUserId(1)).thenReturn(expected);
        assertEquals(1, evidenceService.getEvidenceByUser(1).size());
    }

    @Test
    void getEvidenceByUser_validUser_evidenceDoesntExists () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Mockito.when(evidenceRepository.findByUserId(1)).thenReturn(expected);
        assertEquals(0, evidenceService.getEvidenceByUser(1).size());
    }

    @Test
    void getEvidenceByUser_invalidUser () {
        Mockito.when(evidenceRepository.findByUserId(1)).thenReturn(null);
        assertNull(evidenceService.getEvidenceByUser(1));
    }

    @Test
    void saveEvidence_validData () {
        Mockito.when(evidenceRepository.save(Mockito.any(Evidence.class)))
                .thenReturn(new Evidence("My Evidence", new Date(), "Evidence Description", 1, "username"));
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        assertNotEquals(-1, evidenceService.save(e));
        verify(evidenceRepository, times(1)).save(e);
    }

    @Test
    void saveEvidence_invalidData () {
        Evidence invalid = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        invalid.setName(null);
        assertEquals(-1, evidenceService.save(invalid));
        verify(evidenceRepository, never()).save(Mockito.any(Evidence.class));
    }

    @Test
    void existsById_evidenceExists () {
        Mockito.when(evidenceRepository.existsById(1)).thenReturn(true);
        assertTrue(evidenceService.existsById(1));
    }

    @Test
    void existsById_evidenceDoesntExists () {
        Mockito.when(evidenceRepository.existsById(1)).thenReturn(false);
        assertFalse(evidenceService.existsById(1));
    }

    @Test
    void createEvidence_validData() {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Mockito.when(projectService.getProjectById(anyInt())).thenReturn(newProject);
        Mockito.when(evidenceRepository.save(any())).then(returnsFirstArg());
        Mockito.when(evidenceService.existsById(anyInt())).thenReturn(true);
        Mockito.when(userService.getUserDTO(anyInt())).thenReturn(new UserDTO());
        Mockito.when(evidenceService.getEvidenceById(anyInt())).thenReturn(new Evidence("My Evidence", new Date(), "Evidence Description", 1, "username"));
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022-05-05", "Evidence Description");
        evidence.setCommits(new ArrayList<>());
        evidence.setLinkedUsers(new ArrayList<>());
        Evidence valid = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNotNull(valid);
    }

    @Test
    void createEvidence_InvalidDate_BeforeProjectStarts() {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Mockito.when(projectService.getProjectById(anyInt())).thenReturn(newProject);
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2021-12-05", "Evidence Description");
        Evidence invalid = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNull(invalid);
    }

    @Test
    void createEvidence_InvalidDate_AfterProjectEnds() {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Mockito.when(projectService.getProjectById(anyInt())).thenReturn(newProject);
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022-08-05", "Evidence Description");
        Evidence invalid = evidenceService.createEvidence(evidence, 1,1, "username");
        assertNull(invalid);
    }

    @Test
    void createEvidence_validDate_onProjectStartsDate() {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Mockito.when(projectService.getProjectById(anyInt())).thenReturn(newProject);
        Mockito.when(evidenceRepository.save(any())).then(returnsFirstArg());
        Mockito.when(evidenceService.existsById(anyInt())).thenReturn(true);
        Mockito.when(userService.getUserDTO(anyInt())).thenReturn(new UserDTO());
        Mockito.when(evidenceService.getEvidenceById(anyInt())).thenReturn(new Evidence("My Evidence", new Date(), "Evidence Description", 1, "username"));
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022-01-22", "Evidence Description");
        evidence.setLinkedUsers(new ArrayList<>());
        evidence.setCommits(new ArrayList<>());
        Evidence valid = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNotNull(valid);
    }

    @Test
    void createEvidence_validDate_onProjectEndsDate() {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Mockito.when(projectService.getProjectById(anyInt())).thenReturn(newProject);
        Mockito.when(evidenceRepository.save(any())).then(returnsFirstArg());
        Mockito.when(evidenceService.existsById(anyInt())).thenReturn(true);
        Mockito.when(userService.getUserDTO(anyInt())).thenReturn(new UserDTO());
        Mockito.when(evidenceService.getEvidenceById(anyInt())).thenReturn(new Evidence("My Evidence", new Date(), "Evidence Description", 1, "username"));
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022-07-29", "Evidence Description");
        evidence.setCommits(new ArrayList<>());
        evidence.setLinkedUsers(new ArrayList<>());
        Evidence valid = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNotNull(valid);
    }

    @Test
    void createEvidence_invalidName() {
        EvidenceDTO evidence = new EvidenceDTO("", "2022-08-05", "Evidence Description");
        Evidence invalid = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNull(invalid);
    }

    @Test
    void createEvidence_invalidDate() {
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022/08/05", "Evidence Description");
        Evidence invalidDate = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNull(invalidDate);
    }

    @Test
    void createEvidence_invalidDescription() {
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", "2022-08-05", "");
        Evidence invalidDescription = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNull(invalidDescription);
    }

    @Test
    void createEvidence_nullDate() {
        EvidenceDTO evidence = new EvidenceDTO("Evidence Name", null, "Evidence Description");
        Evidence invalidDateNull = evidenceService.createEvidence(evidence, 1, 1, "username");
        assertNull(invalidDateNull);
    }

    @Test
    void userCanModifyEvidence_evidenceExists_userCanModify () {
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(new Evidence("Test Evidence", new Date(), "Test Description", 1, "username"));
        assertTrue(evidenceService.userCanModifyEvidence(1, 1));
    }

    @Test
    void userCanModifyEvidence_evidenceExists_userCantModify () {
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(new Evidence("Test Evidence", new Date(), "Test Description", 1, "username"));
        assertFalse(evidenceService.userCanModifyEvidence(1, 2));
    }

    @Test
    void userCanModifyEvidence_evidenceDoesntExist () {
        when(evidenceRepository.existsById(1)).thenReturn(false);
        assertFalse(evidenceService.userCanModifyEvidence(1, 2));
    }

    @Test
    void addWebLink_evidenceExists () {
        Evidence testEvidence = new Evidence("Test Evidence", new Date(), "Test Description", 1, "username");
        testEvidence.setId(1);
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(testEvidence);
        when(evidenceRepository.save(any())).thenReturn(testEvidence);
        assertTrue(evidenceService.addWebLink(1, "https://www.google.com"));
    }

    @Test
    void addWebLink_evidenceDoesntExist () {
        when(evidenceRepository.existsById(1)).thenReturn(false);
        assertFalse(evidenceService.addWebLink(1, "https://www.google.com"));
    }

    @Test
    void addWebLink_evidenceExists_maxNumLinksReached () {
        Evidence testEvidence = new Evidence("Test Evidence", new Date(), "Test Description", 1, "username");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.setId(1);
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(testEvidence);
        assertFalse(evidenceService.addWebLink(1, "https://www.google.com"));
    }

    @Test
    void addWebLink_evidenceExists_containingValidNumWebLinks () {
        Evidence testEvidence = new Evidence("Test Evidence", new Date(), "Test Description", 1, "username");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.setId(1);
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(testEvidence);
        when(evidenceRepository.save(any())).thenReturn(testEvidence);
        assertTrue(evidenceService.addWebLink(1, "https://www.google.com"));
    }

    @Test
    void removeWebLink_evidenceExists_weblinkExists () {
        Evidence testEvidence = new Evidence("Test Evidence", new Date(), "Test Description", 1, "username");
        testEvidence.getWebLinks().add("testlink");
        testEvidence.setId(1);
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(testEvidence);
        when(evidenceRepository.save(any())).thenReturn(testEvidence);
        assertTrue(evidenceService.removeWebLink(1, "testlink"));
    }

    @Test
    void removeWebLink_evidenceExists_weblinkDoesntExist () {
        Evidence testEvidence = new Evidence("Test Evidence", new Date(), "Test Description", 1, "username");
        testEvidence.setId(1);
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(testEvidence);
        assertFalse(evidenceService.removeWebLink(1, "testlink"));
    }

    @Test
    void removeWebLink_evidenceDoesntExist () {
        when(evidenceRepository.existsById(1)).thenReturn(false);
        assertFalse(evidenceService.removeWebLink(1, "https://www.google.com"));
    }

    @Test
    void addEvidenceSkill_evidenceNull () {
        assertFalse(evidenceService.addEvidenceSkill(-1, "New skill"));
    }

    @Test
    void addEvidenceSkill_evidenceSkillNull () {
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(new Evidence("New Evidence", new Date(), "A description", 1, "username"));
        assertFalse(evidenceService.addEvidenceSkill(1, ""));
    }

    @Test
    void addEvidenceSkill_evidenceSkillValid () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        when(evidenceRepository.existsById(e.getId())).thenReturn(true);
        when(evidenceRepository.findById(e.getId())).thenReturn(e);
        when(evidenceRepository.save(any())).thenReturn(e);
        when(evidenceSkillRepository.save(any())).thenReturn(new EvidenceSkill(e, "New skill"));
        assertTrue(evidenceService.addEvidenceSkill(e.getId(), "New skill"));
    }

    @Test
    void addEvidenceSkill_evidenceSkillExists () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        EvidenceSkill es = new EvidenceSkill(e, "New skill");
        e.addSkill(es);
        when(evidenceRepository.existsById(e.getId())).thenReturn(true);
        when(evidenceRepository.findById(e.getId())).thenReturn(e);
        when(evidenceSkillRepository.save(any())).thenReturn(es);
        assertFalse(evidenceService.addEvidenceSkill(e.getId(), "New skill"));
    }

    @Test
    void removeEvidenceSkill_evidenceNull () {
        assertFalse(evidenceService.removeEvidenceSkill(-1, "New skill"));
    }

    @Test
    void removeEvidenceSkill_evidenceSkillNull () {
        when(evidenceRepository.existsById(1)).thenReturn(true);
        when(evidenceRepository.findById(1)).thenReturn(new Evidence("New Evidence", new Date(), "A description", 1, "username"));
        assertFalse(evidenceService.removeEvidenceSkill(1, ""));
    }

    @Test
    void removeEvidenceSkill_evidenceSkillExists () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        EvidenceSkill es = new EvidenceSkill(e, "New skill");
        e.addSkill(es);
        when(evidenceRepository.existsById(e.getId())).thenReturn(true);
        when(evidenceRepository.findById(e.getId())).thenReturn(e);
        when(evidenceRepository.save(any())).thenReturn(e);
        assertTrue(evidenceService.removeEvidenceSkill(e.getId(), "New skill"));
    }

    @Test
    void removeEvidenceSkill_evidenceSkillDoesntExist () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        when(evidenceRepository.existsById(e.getId())).thenReturn(true);
        when(evidenceRepository.findById(e.getId())).thenReturn(e);
        assertFalse(evidenceService.removeEvidenceSkill(e.getId(), "New skill"));
    }

    @Test
    void getOriginalSkill_queryMatches_differentCase () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals("test", evidenceService.getOriginalSkill("TEST"));
    }

    @Test
    void getOriginalSkill_queryMatches_sameCase () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals("test", evidenceService.getOriginalSkill("test"));
    }

    @Test
    void getOriginalSkill_queryDoesntMatch () {
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals("test", evidenceService.getOriginalSkill("test"));
    }

    @Test
    void findSkillQueryMatch_queryMatches_foundOne () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals(1, evidenceService.findSkillQueryMatch("test").size());
    }

    @Test
    void findSkillQueryMatch_queryMatches_foundNone () {
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals(0, evidenceService.findSkillQueryMatch("test").size());
    }

    @Test
    void findSkillQueryMatch_queryMatches_foundDuplicates () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        response.add(new EvidenceSkill(e, "test"));
        when (evidenceSkillRepository.search("test")).thenReturn(response);
        assertEquals(1, evidenceService.findSkillQueryMatch("test").size());
    }

    @Test
    void findSkillQueryMatch_blankQuery () {
        assertEquals(0, evidenceService.findSkillQueryMatch("").size());
    }

    @Test
    void findEvidenceBySkill_foundNone () {
        when(evidenceSkillRepository.search("test")).thenReturn(new ArrayList<>());
        when(evidenceSkillRepository.findBySkillName("test")).thenReturn(new ArrayList<>());
        assertEquals(0, evidenceService.findEvidenceBySkill("test").size());
    }

    @Test
    void findEvidenceBySkill_foundOne () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        when(evidenceSkillRepository.search("test")).thenReturn(response);
        when(evidenceSkillRepository.findBySkillName("test")).thenReturn(response);
        assertEquals(1, evidenceService.findEvidenceBySkill("test").size());
    }

    @Test
    void findEvidenceBySkill_foundMultiple () {
        Evidence e = new Evidence("New Evidence", new Date(), "A description", 1, "username");
        ArrayList<EvidenceSkill> response = new ArrayList<>();
        response.add(new EvidenceSkill(e, "test"));
        response.add(new EvidenceSkill(e, "test"));
        response.add(new EvidenceSkill(e, "test"));
        when(evidenceSkillRepository.search("test")).thenReturn(response);
        when(evidenceSkillRepository.findBySkillName("test")).thenReturn(response);
        assertEquals(3, evidenceService.findEvidenceBySkill("test").size());
    }

    @Test
    void getSkillsForUser_noSkills () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        expected.add(e);
        when(evidenceRepository.findByUserId(1)).thenReturn(expected);
        when(evidenceSkillRepository.findByEvidence(e)).thenReturn(new ArrayList<>());
        assertEquals(1, evidenceService.getSkillsForUser(1).size());
    }

    @Test
    void getSkillsForUser_containingSkills () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        expected.add(e);
        List<EvidenceSkill> evidenceSkills = new ArrayList<>();
        evidenceSkills.add(new EvidenceSkill(e, "test"));
        when(evidenceRepository.findByUserId(1)).thenReturn(expected);
        when(evidenceSkillRepository.findByEvidence(e)).thenReturn(evidenceSkills);
        assertEquals(1, evidenceService.getSkillsForUser(1).size());
    }

    @Test
    void getSkillsForUser_containingSkillsDuplicates () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        Evidence e1 = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        expected.add(e);
        expected.add(e1);
        List<EvidenceSkill> evidenceSkills = new ArrayList<>();
        evidenceSkills.add(new EvidenceSkill(e, "test"));
        evidenceSkills.add(new EvidenceSkill(e1, "test"));
        when(evidenceRepository.findByUserId(1)).thenReturn(expected);
        when(evidenceSkillRepository.findByEvidence(e)).thenReturn(evidenceSkills);
        assertEquals(1, evidenceService.getSkillsForUser(1).size());
    }

    @Test
    void getEvidenceWithoutSkills_noEvidence () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        expected.add(e);
        List<EvidenceSkill> evidenceSkills = new ArrayList<>();
        evidenceSkills.add(new EvidenceSkill(e, "test"));
        when(evidenceRepository.findAll()).thenReturn(expected);
        when(evidenceSkillRepository.findAll()).thenReturn(evidenceSkills);
        assertEquals(0, evidenceService.getEvidenceWithoutSkills().size());
    }

    @Test
    void getEvidenceWithoutSkills_containingEvidence () {
        ArrayList<Evidence> expected = new ArrayList<>();
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        expected.add(e);
        when(evidenceRepository.findAll()).thenReturn(expected);
        when(evidenceSkillRepository.findAll()).thenReturn(new ArrayList<>());
        assertEquals(1, evidenceService.getEvidenceWithoutSkills().size());
    }

    @Test
    void addCategories_adding3categories () {
        List<String> categories = Arrays.asList("Service", "Quantitative Skill", "Qualitative Skill");
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        evidenceService.addCategories(e, categories);
        assertEquals(3, e.getCategories().size());
        //Check no duplicates
        assertEquals(3, (int) e.getCategories().stream().distinct().count());
    }

    @Test
    void addCategories_allNullCategories () {
        List<String> categories = Arrays.asList(null, null, null);
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        evidenceService.addCategories(e, categories);
        assertEquals(0, e.getCategories().size());
    }

    @Test
    void addCategories_adding2categories () {
        List<String> categories = Arrays.asList("Service", "Quantitative Skill", null);
        Evidence e = new Evidence ("My Evidence", new Date(), "Evidence Description", 1, "username");
        evidenceService.addCategories(e, categories);
        assertEquals(2, e.getCategories().size());
    }

    @Test
    void deleteEvidence_invalidId () {
        Mockito.when(evidenceRepository.findById(100)).thenReturn(null);
        assertFalse(evidenceService.deleteEvidenceById(100));
    }

    @Test
    void deleteEvidence_valid () {
        Evidence e = new Evidence("My Evidence", new Date(), "", 1, "name");
        Mockito.when(evidenceRepository.findById(1)).thenReturn(e);
        assertTrue(evidenceService.deleteEvidenceById(1));
    }

    @Test
    void deleteEvidence_validWithSkills () {
        Evidence e = new Evidence("My Evidence", new Date(), "", 1, "name");
        e.addSkill(new EvidenceSkill(e, "skill"));
        Mockito.when(evidenceRepository.findById(1)).thenReturn(e);
        assertTrue(evidenceService.deleteEvidenceById(1));
    }

    @Test
    void createEvidence_skillStringNoSkills () {
        Evidence e = new Evidence("My Evidence", new Date(), "", 1, "name");

        Mockito.when(evidenceRepository.existsById(1)).thenReturn(true);
        Mockito.when(evidenceRepository.findById(1)).thenReturn(e);

        assertFalse(evidenceService.addEvidenceSkill(1, "No_Skills"));
    }

    @Test
    void createEvidence_skillStringNoSkillsLower () {
        Evidence e = new Evidence("My Evidence", new Date(), "", 1, "name");

        Mockito.when(evidenceRepository.existsById(1)).thenReturn(true);
        Mockito.when(evidenceRepository.findById(1)).thenReturn(e);

        assertFalse(evidenceService.addEvidenceSkill(1, "no_skills"));
    }
}
