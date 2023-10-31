package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.authentication.CookieUtil;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.CommitDTO;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.EvidenceDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.BadRequest;
import nz.ac.canterbury.seng302.portfolio.exceptions.ForbiddenException;
import nz.ac.canterbury.seng302.portfolio.exceptions.InternalServerErrorException;
import nz.ac.canterbury.seng302.portfolio.exceptions.NotFoundException;
import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.entities.EvidenceSkill;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.RepositoryCommit;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.portfolio.util.EvidenceSorter;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class EvidenceController {
    public static final String REGEX = "[\n\r\t]";
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService users;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private ProjectUserService projectUserService;
    @Autowired
    private EvidenceService evidenceService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private SprintService sprintService;

    private final Logger logger = LoggerFactory.getLogger(EvidenceController.class);
    private static final String EVIDENCE_ATTRIB = "evidence"; //Thymeleaf attribute for evidence

    /**
     * Controller for evidence page.
     * @param principal Authentication token
     * @param model Page model
     * @return HTML to display
     */
    @GetMapping("/evidence")
    public String getEvidence (@AuthenticationPrincipal AuthState principal, @RequestParam(name="userId", required=false, defaultValue="-1") String userIdStr,
                               HttpServletRequest request, @RequestParam(name="projectId", required=false) String projectIdParam,
                               Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        int userId = Integer.parseInt(userIdStr);
        boolean myEvidence = false;
        if (userId == -1) {
            userId = users.getIdFromAuthState(principal);
            myEvidence = true;
        }
        try {
            userClientGRPCService.receiveGetUserAccountById(users.getIdFromAuthState(principal));
        } catch (StatusRuntimeException e){
            if (!myEvidence) return "redirect:user_details";
            model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
            return "redirect:/login";
        }

        model.addAttribute("userId", users.getIdFromAuthState(principal));
        model.addAttribute("username", userClientGRPCService.receiveGetUserAccountById(users.getIdFromAuthState(principal)).getUsername());
        model.addAttribute("projects", projectUserService.getProjectsForUser(userId));
        String projectId;
        if (projectIdParam == null && myEvidence) {
            projectId = CookieUtil.getValue(request, "current-project");
        } else {
            projectId = projectIdParam;
        }
        if (projectId == null || projectService.getProjectById(Integer.parseInt(projectId)) == null) {
            //User has not selected project for viewing evidence
            model.addAttribute("projectSelected", false);
            model.addAttribute("project", null);
            model.addAttribute("myEvidence", myEvidence);
            model.addAttribute("viewingUserId", userId);
            return "evidence/evidence";
        }
        model.addAttribute("projectSelected", true);
        model.addAttribute("currentProjectId", projectId);

        //Add to model
        Project project = projectService.getProjectById(Integer.parseInt(projectId));
        model.addAttribute("isAdmin", users.isAdmin(principal));
        model.addAttribute("viewingUserId", userId);
        model.addAttribute("project", project);
        model.addAttribute("myEvidence", myEvidence);
        model.addAttribute("userSkills", evidenceService.getSkillsForUser(userId));
        model.addAttribute("groups", groupService.getGroupsForUser(principal));
        model.addAttribute("sprints", sprintService.getSprintsByProject(project));

        //Get all evidence
        List<Evidence> allEvidence = evidenceService.getEvidenceByUserAndProject(userId, Integer.parseInt(projectId));
        allEvidence.sort(new EvidenceSorter());
        Collections.reverse(allEvidence);
        model.addAttribute(EVIDENCE_ATTRIB, allEvidence);
        model.addAttribute("evidenceList", allEvidence.isEmpty());
        return "evidence/evidence";
    }

    /**
     * API endpoint for creating a piece of evidence
     * @param principal Authentication token
     * @param evidenceDTO Evidence data
     * @return New evidence ID
     */
    @ResponseBody
    @PostMapping("api/evidence/create")
    @ResponseStatus(HttpStatus.CREATED)
    public int createEvidence(@AuthenticationPrincipal AuthState principal, @RequestParam(name="projectId", required=false) Integer projectId,
                              @RequestBody EvidenceDTO evidenceDTO) {
        int userId = users.getIdFromAuthState(principal);
        evidenceDTO.setName(evidenceDTO.getName().replaceAll(REGEX, "_"));
        logger.info("Creating new evidence for user {} with name {}", userId, evidenceDTO.getName());
        Evidence newEvidence = evidenceService.createEvidence(evidenceDTO, userId, projectId, users.getUserDTO(userId).getName());
        evidenceService.addCategories (newEvidence, evidenceDTO.getCategories());
        if (newEvidence == null) {
            logger.info("Try to create a new evidence but there's a problem");
            throw new BadRequest();
        }
        return newEvidence.getId();
    }

    /**
     * API endpoint for getting the HTML for a piece of Evidence
     * @param evidenceId The Evidence ID that is being requested
     * @param model Page model
     * @return HTML fragment of the requested evidence
     */
    @GetMapping("api/evidence/{evidenceID}")
    @ResponseStatus(HttpStatus.OK)
    public String getEvidenceById(@AuthenticationPrincipal AuthState principal, @PathVariable("evidenceID") int evidenceId,
                                  @RequestParam int projectId,
                                  Model model) {
        Evidence evidence1;
        evidence1 = evidenceService.getEvidenceById(evidenceId);

        if (evidence1 == null) throw new NotFoundException(EVIDENCE_ATTRIB, evidenceId);

        model.addAttribute(EVIDENCE_ATTRIB, evidence1);
        model.addAttribute("viewingUserId", users.getIdFromAuthState(principal));
        model.addAttribute("project", projectService.getProjectById(projectId));

        logger.info("Returning evidence pane");
        return "evidence/evidencePane";
    }

    /**
     * Get the evidence details in DTO form
     * @return EvidenceDTO with uptodate details
     */
    @ResponseBody
    @GetMapping("api/evidence/{evidenceID}/details")
    @ResponseStatus(HttpStatus.OK)
    public EvidenceDTO getEvidenceDetailsById(@PathVariable("evidenceID") int evidenceId) {
        Evidence evidence = evidenceService.getEvidenceById(evidenceId);
        if (evidence == null) throw new NotFoundException(EVIDENCE_ATTRIB, evidenceId);

        EvidenceDTO result = new EvidenceDTO();
        result.setName(evidence.getName());
        result.setDescription(evidence.getDescription());
        result.setDate(DateUtil.dateToISOString(evidence.getDate()));
        result.setWebLinks(evidence.getWebLinks());
        result.setCategories(evidence.getCategories());

        Set<String> skills = new HashSet<>();
        for (EvidenceSkill skill : evidence.getSkills()) {
            skills.add(skill.getSkillName());
        }

        Set<CommitDTO> commits = new HashSet<>();
        for (RepositoryCommit commit : evidence.getCommits()) {
            commits.add(new CommitDTO(commit));
        }

        result.setSkills(skills);
        result.setCommits(commits.stream().toList());
        return result;
    }

    /**
     * Adds a weblink to a piece of evidence
     * @param principal Authentication token
     * @param id ID of the evidence to add weblink to
     * @param webLink String representation of the weblink to add
     * @return HTTP response, 200 if added, 403 if forbidden, 500 otherwise
     */
    @PutMapping("api/evidence/{evidenceId}/webLink")
    public ResponseEntity<String> addWebLink (@AuthenticationPrincipal AuthState principal, @PathVariable("evidenceId") int id, @RequestParam(name="weblink") String webLink) {
        int userId = users.getIdFromAuthState(principal);
        if (!evidenceService.userCanModifyEvidence(id, userId)) {
            logger.warn("User {} is not authorised to modify evidence {}", id, userId);
            throw new ForbiddenException();
        }

        // Checks for invalid sanitised web link and throws 400 if found.
        if (!webLink.matches("(https?:\\/\\/www\\.[a-zA-Z\\d@:%._\\+\\-~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)|https?:\\/\\/[^w.]([a-zA-Z\\d@:%._\\+\\-~#=]){2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*))")) {
            logger.error("Weblink {} is invalid and cannot be added to this evidence {}", webLink, id);
            throw new BadRequest();
        }

        String sanitisedWebLink = webLink.replaceAll(REGEX, "_");
        logger.info("Adding new weblink '{}' to evidence {}", sanitisedWebLink, id);

        if (!evidenceService.addWebLink(id, sanitisedWebLink)) {
            logger.warn("Something went wrong adding a new weblink to evidence {}", id);
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>("Added new weblink to evidence", HttpStatus.OK);
    }

    /**
     * Removes an existing weblink from a piece of evidence
     * @param principal Authentication token
     * @param id ID of the evidence to remove weblink from
     * @param webLink String representation of the weblink to remove
     * @return HTTP response, 200 if removed, 403 if forbidden, 500 otherwise
     */
    @DeleteMapping("api/evidence/{evidenceId}/webLink")
    public ResponseEntity<String> removeWebLink (@AuthenticationPrincipal AuthState principal, @PathVariable("evidenceId") int id, @RequestParam(name="weblink") String webLink) {
        int userId = users.getIdFromAuthState(principal);
        if (!evidenceService.userCanModifyEvidence(id, userId)) {
            logger.warn("User {} is not authorised to modify evidence {}", id, userId);
            throw new ForbiddenException();
        }

        String sanitisedWebLink = webLink.replaceAll(REGEX, "_");
        logger.info("Removing weblink '{}' for evidence {}", sanitisedWebLink, id);

        if (!evidenceService.removeWebLink(id, sanitisedWebLink)) {
            logger.warn("Something went wrong removing weblink '{}' from evidence {}", sanitisedWebLink, id);
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>("Removed weblink from evidence", HttpStatus.OK);
    }

    /**
     * Adds a skill to a piece of evidence
     * @param principal Authentication token
     * @param id ID of the evidence to add weblink to
     * @param skill String representation of the skill to add
     * @return HTTP response, 200 if added, 403 if forbidden, 500 otherwise
     */
    @PutMapping("api/evidence/{evidenceId}/skill")
    public ResponseEntity<String> addSkill (@AuthenticationPrincipal AuthState principal, @PathVariable("evidenceId") int id, @RequestParam(name="skill") String skill) {
        int userId = users.getIdFromAuthState(principal);
        if (!evidenceService.userCanModifyEvidence(id, userId)) {
            logger.warn("User {} is not authorised to modify evidence {}", id, userId);
            throw new ForbiddenException();
        }

        if (!evidenceService.addEvidenceSkill(id, skill)) {
            logger.warn("Something went wrong adding a new skill to evidence {}", id);
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>("Added new skill to evidence", HttpStatus.OK);
    }

    /**
     * Gets a Thymeleaf fragment for a query to get matching skill text
     * @param query Query to search in skill DB
     * @param model Thymeleaf DOM
     * @return Thymeleaf fragment for autocomplete skills
     */
    @GetMapping("api/evidence/skills")
    public String getAutocompleteSkillResults (@RequestParam(name="query") String query, Model model) {
        //Sanitize query
        String sanitizedQuery = query.replaceAll(REGEX, " ");
        logger.info("Getting skill tags like query \"{}\"", sanitizedQuery);
        List<String> matches = evidenceService.findSkillQueryMatch(sanitizedQuery);
        logger.info("Found {} similar matches", matches.size());
        model.addAttribute("autocompleteSkills", matches);
        return "evidence/autocomplete";
    }

    /**
     * Gets the evidence skills page displaying all evidence that has a specific skill on it
     * @param principal Authentication token
     * @param skill Skill to display evidence for
     * @param model HTML DOM
     * @return Full Thymeleaf page for entire evidence skill page
     */
    @GetMapping("evidenceSkill")
    public String evidenceSkillPage (@AuthenticationPrincipal AuthState principal, @RequestParam(name="skill") String skill, @RequestParam int projectId, Model model) {
        String sanitizedSkill = skill.replaceAll(REGEX, " ");
        int userId = users.getIdFromAuthState(principal);
        model.addAttribute("username", userClientGRPCService.receiveGetUserAccountById(userId).getUsername());
        List<Evidence> allEvidence = evidenceService.findEvidenceBySkill(sanitizedSkill);
        //Sort by most recent first
        allEvidence.sort(new EvidenceSorter());
        Collections.reverse(allEvidence);
        model.addAttribute("allEvidence", allEvidence);
        model.addAttribute("skill", sanitizedSkill);
        model.addAttribute("viewingUserId", userId);
        model.addAttribute("currentProjectId", projectId);

        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("groups", groupService.getGroupsForUser(principal));
        model.addAttribute("sprints", sprintService.getSprintsByProject(project));
        return "evidence/evidenceSkillPage";
    }

    /**
     * Gets the evidence category page displaying all evidence that has a specific category on it
     * @param principal Authentication token
     * @param category Category to display evidence for
     * @param model HTML DOM
     * @return Full Thymeleaf page for entire evidence skill page
     */
    @GetMapping("evidenceCategory")
    public String evidenceCategoryPage (@AuthenticationPrincipal AuthState principal, @RequestParam(name="category") String category, @RequestParam int projectId, Model model) {
        int userId = users.getIdFromAuthState(principal);
        model.addAttribute("username", userClientGRPCService.receiveGetUserAccountById(userId).getUsername());
        List<Evidence> allEvidence = evidenceService.getEvidenceByCategory(category);
        //Sort by most recent first
        allEvidence.sort(new EvidenceSorter());
        Collections.reverse(allEvidence);
        logger.info("Get all evidence by category and user id");

        // Pass all the information to the HTML page
        model.addAttribute("allEvidence", allEvidence);
        model.addAttribute("category", category);
        model.addAttribute("viewingUserId", userId);
        model.addAttribute("currentProjectId", projectId);

        Project project = projectService.getProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("groups", groupService.getGroupsForUser(principal));
        model.addAttribute("sprints", sprintService.getSprintsByProject(project));
        return "evidence/evidenceCategoryPage";
    }

    /**
     * Deletes the piece of evidence specified by the supplied evidence ID
     * @param principal Authentication token
     * @param evidenceId ID of the evidence to be deleted
     * @param model HTML DOM
     * @return Response Entity stating the operation that has been completed
     */
    @DeleteMapping("api/evidence/{evidenceId}")
    public ResponseEntity<String> deleteEvidence (@AuthenticationPrincipal AuthState principal, @PathVariable(name="evidenceId") int evidenceId, Model model) {
        int userId = users.getIdFromAuthState(principal);
        if (!evidenceService.userCanModifyEvidence(evidenceId, userId)) {
            logger.warn("User {} is not authorised to modify evidence {}", evidenceId, userId);
            throw new ForbiddenException();
        }

        if (!evidenceService.deleteEvidenceById(evidenceId)) {
            logger.warn("Something went wrong deleting evidence {}", evidenceId);
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>("Deleted specified evidence", HttpStatus.OK);
    }

    /**
     * API endpoint to get all the existing skills for the user
     * @param principal Authentication token
     * @return List of all the currently existing skills for the viewing users evidence
     */
    @ResponseBody
    @GetMapping("api/evidence/allSkills")
    public List<String> getAllEvidenceSkills (@AuthenticationPrincipal AuthState principal) {
        return evidenceService.getSkillsForUser(users.getIdFromAuthState(principal));
    }


    /**
     * Gets a list of users in the database from a search query to be added to the evidence
     * @param principal Authentication token
     * @param query Search query to match users
     * @param model HTML DOM
     * @return Thymeleaf fragment of list of users to add to evidence
     */
    @GetMapping ("evidence/getUsers")
    public String getUsersListForEvidence (@AuthenticationPrincipal AuthState principal, @RequestParam(name="query") String query, Model model) {
        query = query.replaceAll(REGEX, " ");
        logger.info("Finding all users that match query {}", query);
        List<UserDTO> matchingUsers = null;
        if (!query.isBlank() && !query.isEmpty()) {
            matchingUsers = users.getFilteredPaginatedUsers(query, 0);
            matchingUsers.remove(users.getCompleteUserDTO(userClientGRPCService.receiveGetUserAccountById(users.getIdFromAuthState(principal))));
        }
        if (matchingUsers == null) matchingUsers = new ArrayList<>();
        model.addAttribute("users", matchingUsers);
        return "evidence/userDropdown";
    }
}
