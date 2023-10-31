package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.evidence.CommitDTO;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.EvidenceDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.entities.EvidenceSkill;
import nz.ac.canterbury.seng302.portfolio.model.entities.EvidenceUser;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.RepositoryCommit;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EvidenceRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EvidenceSkillRepository;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EvidenceService {
    @Autowired
    private EvidenceRepository evidence;

    @Autowired
    private EvidenceSkillRepository evidenceSkillRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    private static final Logger logger = LoggerFactory.getLogger(EvidenceService.class);

    /**
     * Gets a piece of evidence stored in the database from the id
     * @param id ID of the evidence to find
     * @return Evidence object found in the database
     */
    public Evidence getEvidenceById (int id) {
        logger.info("Getting evidence {}", id);
        if (!evidence.existsById(id)) {
            logger.info("Tried to get evidence {} but it couldn't be found", id);
            return null;
        }
        logger.info("Got evidence {}", id);
        return evidence.findById(id);
    }

    /**
     * Gets all the evidence that exists in the database for a user
     * @param userId User ID to get the evidence for
     * @return List of all the evidence that the user has
     */
    public List<Evidence> getEvidenceByUser (int userId) {
        logger.info("Getting all evidence for user {}", userId);
        List<Evidence> allEvidence = evidence.findByUserId(userId);
        if (allEvidence == null) {
            logger.info("Tried to get all evidence for user {} but the user couldn't be found", userId);
            return null;
        }
        logger.info("Got {} pieces of evidence for user {}", allEvidence.size(), userId);
        return allEvidence;
    }

    public List<Evidence> getEvidenceByUserAndProject (int userId, int projectId) {
        return evidence.findByUserIdAndProjectId(userId, projectId);
    }

    /**
     * Saves valid Evidence object in the database
     * @param e Evidence object to save in the database
     * @return ID of the evidence stored in the database, -1 if it couldn't be completed
     */
    public int save (Evidence e) {
        logger.info("Saving evidence for user {}", e.getUserId());
        if (e.getName() == null || e.getDate() == null || e.getDescription() == null) {
            logger.info("Tried to save evidence for user {} but there was an issue with one or " +
                    "more of the fields: {}", e.getUserId(), e);
            return -1;
        }

        Evidence saved = evidence.save(e);
        logger.info("Saved evidence with id {} for user {}", saved.getId(), saved.getUserId());
        return saved.getId();
    }

    /**
     * Provides an interface to check the existence of a piece of evidence in the database given the evidence id
     * @param id ID of the evidence to check
     * @return true if the evidence exists, false otherwise
     */
    public boolean existsById (int id) {
        logger.info("Seeing if Evidence with id {} exists in the database", id);
        if (evidence.existsById(id)) {
            logger.info("Evidence found with id {}", id);
            return true;
        } else {
            logger.info("Evidence with id {} couldn't be found in the database", id);
            return false;
        }
    }


    /**
     * Safely creates a new piece of evidence and validates values. Will return null if there is an error
     * @param evidenceDTO Evidence information to create the Evidence object based off
     * @param userId ID of the user that the evidence belongs to
     * @param projectId ID of the current project that the evidence belongs to
     * @return new Evidence object created from the DTO
     */
    public Evidence createEvidence(EvidenceDTO evidenceDTO, int userId, int projectId, String usersName) {
        logger.info("Creating new piece of evidence for user " + userId);
        //We only want to accept valid name strings that are non-null and contain content
        if (evidenceDTO.getName() == null || evidenceDTO.getName().strip().isEmpty()) {
            logger.info("Tried to create new piece of evidence for user {}, but the name field wasn't valid", userId);
            return null;
        }
        //We only want to accept valid description strings that are non-null and contain content
        if (evidenceDTO.getDescription() == null || evidenceDTO.getDescription().strip().isEmpty()) {
            logger.info("Tried to create new piece of evidence for user {}, but the description field wasn't valid", userId);
            return null;
        }
        //Testing validity of passed in date
        if (evidenceDTO.getDate() == null) {
            logger.info("Tried to create new piece of evidence for user {}, but the date value was null", userId);
            return null;
        }
        Date evidenceDate = DateUtil.stringToISODate(evidenceDTO.getDate());
        if (evidenceDate == null) {
            logger.info("Tried to create a new piece of evidence for user {}, but there was an error creating the Date object", userId);
            return null;
        }
        if (!dateValidation(evidenceDTO, projectId)) {
            logger.info("Tried to create a new piece of evidence for user {}, but the evidence was created out of project date", userId);
            return null;
        }
        logger.info("Created new piece of evidence for user {}", userId);
        Evidence newEvidence;
        //If there are no weblinks provided in the DTO, create the evidence with no weblinks
        if (evidenceDTO.getWebLinks() == null) {
            newEvidence = new Evidence(evidenceDTO.getName(), evidenceDate, evidenceDTO.getDescription(), userId, usersName, projectId);
        } else {
            newEvidence = new Evidence(evidenceDTO.getName(), evidenceDate, evidenceDTO.getDescription(), userId, evidenceDTO.getWebLinks(), usersName);
            newEvidence.setProjectId(projectId);
        }

        newEvidence.setCategories(evidenceDTO.getCategories());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        for (CommitDTO dto : evidenceDTO.getCommits()) {
            try {
                newEvidence.addCommit(new RepositoryCommit(dto.getCommitId(), dto.getCommitName(), dto.getCommitAuthor(), dateFormat.parse(dto.getCommitDate())));
            } catch (ParseException e) {
                logger.error("Couldn't parse date");
            }
        }

        evidenceDTO.getLinkedUsers().add(userId);
        newEvidence.setLinkedUsers(getEvidenceUsers(evidenceDTO.getLinkedUsers()));
        newEvidence = evidence.save(newEvidence);
        // If there are no skills to be added return Evidence as is.
        if (evidenceDTO.getSkills() != null) {
            // If there are skills to be added, then add them to evidence and then return.
            for (String s : evidenceDTO.getSkills()) {
                addEvidenceSkill(newEvidence.getId(), s);
            }
        }

        Evidence savedEvidence = getEvidenceById(newEvidence.getId());
        createEvidenceForLinkedUsers(savedEvidence, evidenceDTO.getLinkedUsers(), userId);

        return savedEvidence;
    }

    /**
     * Creates evidence for linked users
     * @param original Original piece of evidence created
     * @param linkedUsers users to link to evidence
     * @param originalUser the original user that created the evidence
     */
    public void createEvidenceForLinkedUsers (Evidence original, List<Integer> linkedUsers, Integer originalUser) {
        for (Integer focus : linkedUsers) {
            if (!Objects.equals(focus, originalUser)) {
                Evidence newEvidence = copy(original);
                newEvidence.setUserId(focus);
                //Remove the current user and add the original user
                List<EvidenceUser> ogLinkedUsers = new ArrayList<>(newEvidence.getLinkedUsers());
                ogLinkedUsers.remove(new EvidenceUser(focus, userService.getUserDTO(focus).getName()));
                newEvidence.setLinkedUsers(ogLinkedUsers);
                save(newEvidence);
            }
        }
    }

    /**
     * Copies a piece of evidence into a clone
     * @param original Original piece of evidence to clone
     * @return a new distinct evidence object with the same values
     */
    public Evidence copy (Evidence original) {
        Evidence copied = new Evidence(original.getName(), original.getDate(), original.getDescription(), original.getUserId(), new ArrayList<>(original.getWebLinks()), original.getUsersName());
        copied.setProjectId(original.getProjectId());
        copied.setLinkedUsers(new ArrayList<>(original.getLinkedUsers()));
        copied.setCategories(new ArrayList<>(original.getCategories()));
        copied.setCommits(new ArrayList<>(original.getCommits()));
        // If there are skills to be added, then add them to evidence and then return.
        copied = evidence.save(copied);
        for (EvidenceSkill s : original.getSkills()) {
            addEvidenceSkill(copied.getId(), s.getSkillName());
        }
        return evidence.save(copied);
    }
    /**
     * Gets a list of evidence users from a list of user ids
     * @param userIds List of userIds to get EvidenceUser objects for
     * @return a list of all the evidence user objects from the passed in user ids
     */
    public List<EvidenceUser> getEvidenceUsers (List<Integer> userIds) {
        ArrayList<EvidenceUser> evidenceUsers = new ArrayList<>();
        for (Integer userId : userIds) {
            UserDTO user = userService.getUserDTO(userId);
            EvidenceUser evidenceUser = new EvidenceUser(user.getId(), user.getName());
            evidenceUsers.add(evidenceUser);
        }
        return evidenceUsers;
    }

    public boolean dateValidation(EvidenceDTO evidenceDTO, int projectId) {
        Project project = projectService.getProjectById(projectId);
        Date evidenceDate = DateUtil.stringToISODate(evidenceDTO.getDate());
        Date projectStartDate = project.getStartDate();
        Date projectEndDate = project.getEndDate();
        assert evidenceDate != null;
        return !evidenceDate.before(projectStartDate) && !evidenceDate.after(projectEndDate);
    }

    /**
     * Tests if a user is authorised to modify a piece of evidence
     * @param evidenceId ID of the evidence to check
     * @param userId ID of the user to check
     * @return True if the user can modify the evidence, false otherwise
     */
    public boolean userCanModifyEvidence (int evidenceId, int userId) {
        Evidence evidence = getEvidenceById(evidenceId);
        if (evidence == null) return false;
        return evidence.getUserId() == userId;
    }

    /**
     * Adds a weblink to a piece of evidence
     * @param evidenceId ID of the evidence to add weblink to
     * @param webLink String representation of the weblink to add
     * @return True if successfully added the weblink, false otherwise
     */
    public boolean addWebLink (int evidenceId, String webLink) {
        logger.info("Adding new weblink  {}' to evidence {}", webLink, evidenceId);
        Evidence evidence = getEvidenceById (evidenceId);

        if (evidence == null) {
            logger.info("Tried to get evidence with id {} but it couldn't be found", evidenceId);
            return false;
        }

        int numWebLinks = evidence.getWebLinks().size();

        if (numWebLinks >= 10) {
            logger.info("Tried to add new weblink to evidence {} but there were already {} weblinks associated with that evidence! Max 10 links per evidence", evidenceId, numWebLinks);
            return false;
        }

        evidence.getWebLinks().add(webLink);
        return evidenceId == save(evidence);
    }

    /**
     * Removes an already existing weblink from a piece of evidence
     * @param evidenceId ID of the evidence to remove weblink from
     * @param webLink String representation of the weblink to remove
     * @return True if successful, false otherwise
     */
    public boolean removeWebLink (int evidenceId, String webLink) {
        logger.info("Removing weblink  {}' from evidence {}", webLink, evidenceId);
        Evidence evidence = getEvidenceById (evidenceId);

        if (evidence == null) {
            logger.info("Tried to remove weblink from evidence {} but the evidence couldn't be found", evidenceId);
            return false;
        }

        if (!evidence.getWebLinks().contains(webLink)) {
            logger.info("Weblink  {}' doesn't exist for evidence {}", webLink, evidenceId);
            return false;
        }

        evidence.getWebLinks().remove(webLink);
        return evidenceId == save(evidence);
    }

    /**
     * Adds a new EvidenceSkill to a piece of Evidence
     * @param evidenceId ID of the evidence to add skill too
     * @param evidenceSkill String representation of the skill to add
     * @return True if successful, false otherwise
     */
    public boolean addEvidenceSkill(int evidenceId, String evidenceSkill) {
        evidenceSkill = evidenceSkill.replaceAll("[\n\r\t]", "_");
        logger.info("Adding skill {} to evidence {}", evidenceSkill, evidenceId);
        Evidence evidence = getEvidenceById(evidenceId);

        if (evidence == null) {
            logger.info("Tried to add skill to evidence {} but the evidence couldn't be found", evidenceId);
            return false;
        }

        if (evidenceSkill.equals("")) {
            logger.error("Cannot add an empty string as an evidence skill");
            return false;
        }

        if (evidenceSkill.equalsIgnoreCase("NO_SKILLS")) {
            logger.error("Cannot use  skill '{}'", evidenceSkill);
            return false;
        }
        evidenceSkill = getOriginalSkill(evidenceSkill);
        EvidenceSkill newEvidenceSkill = new EvidenceSkill(evidence, evidenceSkill);
        evidenceSkillRepository.save(newEvidenceSkill);

        if (evidence.getSkills().contains(newEvidenceSkill)) {
            logger.error("Cannot add skill {} to evidence {} twice", newEvidenceSkill.getSkillName(), evidenceId);
            return false;
        }

        evidence.addSkill(newEvidenceSkill);
        return evidenceId == save(evidence);
    }

    /**
     * Removes an EvidenceSkill from a piece of Evidence
     * @param evidenceId ID of the evidence to remove skill from
     * @param evidenceSkill String representation of the skill to remove
     * @return True if successful, false otherwise
     */
    public boolean removeEvidenceSkill(int evidenceId, String evidenceSkill) {
        logger.info("Removing skill {} to evidence {}", evidenceSkill, evidenceId);
        Evidence evidence = getEvidenceById(evidenceId);

        if (evidence == null) {
            logger.info("Tried to remove skill from evidence {} but the evidence couldn't be found", evidenceId);
            return false;
        }

        if (evidenceSkill.equals("")) {
            logger.error("Cannot remove an empty string from evidence {} skills", evidenceId);
            return false;
        }
        evidenceSkill = getOriginalSkill(evidenceSkill);
        EvidenceSkill skillToRemove = new EvidenceSkill(evidence, evidenceSkill);

        if (!evidence.getSkills().contains(skillToRemove)) {
            logger.error("Cannot remove skill {} that evidence {} doesn't have", evidenceSkill, evidenceId);
            return false;
        }

        evidence.getSkills().remove(skillToRemove);
        return evidenceId == save(evidence);
    }

    /**
     * Gets the name of an existing skill that already exists in the database. For example, if TAG_Communication
     * already exists, then the string "tag_communication" will be replaced by the original
     * @param evidenceSkill String representation of the skill
     * @return the original value of the skill, if one exists, or the original skill string otherwise
     */
    public String getOriginalSkill (String evidenceSkill) {
        List<String> existingSkills = findSkillQueryMatch(evidenceSkill);
        for (String skill : existingSkills) {
            if (skill.equalsIgnoreCase(evidenceSkill)) return skill;
        }
        return evidenceSkill;
    }

    /**
     * Finds all skills that exist in the database that match the input query
     * @param query Query to match skill
     * @return list of all matched skills
     */
    public List<String> findSkillQueryMatch (String query) {
        List<String> skillStrings = new ArrayList<>();
        //Sanitize query
        String sanitizedQuery = query.replaceAll("[\n\r\t]", " ");
        if (sanitizedQuery.isBlank()) return skillStrings;
        //Search in db
        List<EvidenceSkill> skills = evidenceSkillRepository.search(sanitizedQuery.toLowerCase());
        //filter out similar tags
        for (EvidenceSkill skill : skills) {
            if (!skillStrings.contains(skill.getSkillName())) {
                skillStrings.add(skill.getSkillName());
            }
        }
        return skillStrings;
    }

    /**
     * Searches the database to get all evidence in the application that has a specific tag on it
     * @param skill Skill to search for
     * @return List of all evidence that has that skill
     */
    public List<Evidence> findEvidenceBySkill (String skill) {
        if (skill.equals("No Skills")) return getEvidenceWithoutSkills();
        //get name of skill as it is represented in the database
        String actual = getOriginalSkill(skill);
        List<EvidenceSkill> evidenceSkills = evidenceSkillRepository.findBySkillName(actual);
        return evidenceSkills.stream().map(EvidenceSkill::getEvidence).collect(Collectors.toList());
    }

    /**
     * Finds all the skills that the user has added to any of their evidence
     * @param userId ID of the user to get skills for
     * @return list of all the string representations of the skills the user has added to evidence
     */
    public List<String> getSkillsForUser (int userId) {
        List<Evidence> allEvidence = getEvidenceByUser(userId);
        List<String> allSkills = new ArrayList<>();
        if (!getEvidenceWithoutSkillsForUser(userId).isEmpty()) allSkills.add("No Skills");
        for (Evidence evidence : allEvidence) {
            List<EvidenceSkill> skillsForEvidence = evidenceSkillRepository.findByEvidence(evidence);
            for (EvidenceSkill skill : skillsForEvidence) {
                if (!allSkills.contains(skill.getSkillName())) {
                    allSkills.add(skill.getSkillName());
                }
            }
        }
        return allSkills;
    }

    /**
     * Finds all the evidence that doesn't have an associated skill for a particular user
     * @param userId ID of the user to get evidence without skills for
     * @return List of all the evidence that don't have skills for a user
     */
    public List<Evidence> getEvidenceWithoutSkillsForUser (int userId) {
        List<Evidence> evidenceWithoutSkills = new ArrayList<>(evidence.findByUserId(userId));
        for (Evidence e : new ArrayList<>(evidenceWithoutSkills)) {
            List<EvidenceSkill> skillsForEvidence = evidenceSkillRepository.findByEvidence(e);
            for (EvidenceSkill skill : skillsForEvidence) {
                evidenceWithoutSkills.remove(skill.getEvidence());
            }
        }
        return evidenceWithoutSkills;
    }

    /**
     * Gets all the evidence in the application that have no skills
     * an example would be all the members in team 400
     * @return List of all the evidence with no skills
     */
    public List<Evidence> getEvidenceWithoutSkills () {
        List<Evidence> allEvidence = evidence.findAll();
        for (EvidenceSkill evidenceSkill : evidenceSkillRepository.findAll()) {
            allEvidence.remove(evidenceSkill.getEvidence());
        }
        return allEvidence;
    }

    /**
     * Adds categories to a piece of evidence
     * @param e Evidence to add categories to
     * @param categories all the categories to add to the evidence
     */
    public void addCategories (Evidence e, List<String> categories) {
        List<String> categoriesToAdd = new ArrayList<>();
        for (String category : categories) {
            if (category != null) {
                categoriesToAdd.add(category);
            }
        }
        e.setCategories(categoriesToAdd);
    }

    public List<Evidence> getEvidenceByCategoryAndUserId (String category, int userId) {
        return evidence.findByCategoriesAndUserId(category, userId);
    }

    public List<Evidence> getEvidenceByCategory (String category) {
        return evidence.findByCategories (category);
    }

    /**
     * Deletes the evidence with the supplied ID
     * @param evidenceId The ID of the evidence to be deleted
     * @return True if the evidence is deleted otherwise false
     */
    public boolean deleteEvidenceById (int evidenceId) {
        logger.info("Deleting evidence {}", evidenceId);
        Evidence e = evidence.findById(evidenceId);
        if (e == null) {
            logger.error("Evidence {} could not be found", evidenceId);
            return false;
        }
        try {
            // Deletes all skills related to evidence
            logger.info("Deleting all skills {}", e.getSkills());
            evidenceSkillRepository.deleteAll(e.getSkills());
            evidence.delete(e);
        } catch (IllegalArgumentException exception) {
            logger.error("Something went wrong deleting evidence {}.", e.getId());
            return false;
        }
        return true;
    }
}
