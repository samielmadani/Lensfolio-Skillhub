package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.evidence.CommitDTO;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.RepositoryDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.InternalServerErrorException;
import nz.ac.canterbury.seng302.portfolio.exceptions.InvalidAuthorizationException;
import nz.ac.canterbury.seng302.portfolio.exceptions.NotFoundException;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Repo;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ModifyGroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GroupSettingsController {
    @Autowired
    private RepositoryService repository;
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private GroupClientGRPCService groupClientGRPCService;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    private static final Logger logger = LoggerFactory.getLogger(GroupSettingsController.class);

    /**
     * Gets the settings page for a group including the repository information
     * @param principal AuthenticationPrincipal of the request origin
     * @param groupIdStr String representation of the ID of the group to get the settings page for
     * @param model HTML DOM
     * @return HTML thymeleaf page for the group settings with this specific group information
     */
    @GetMapping("groupSettings")
    public String getGroupSettingsPage (@AuthenticationPrincipal AuthState principal, @RequestParam(name="groupID") String groupIdStr, Model model) {
        int groupId = Integer.parseInt(groupIdStr);
        int userId = userService.getIdFromAuthState(principal);
        if (!groupService.userInGroup(principal, groupId)) {
            logger.info("User {} tried to access group settings page for group {}, but they weren't authorized", userId, groupId);
            return "groups/groups";
        }
        //Add username for navbar
        model.addAttribute("username", userClientGRPCService.receiveGetUserAccountById(userId).getUsername());
        //Add projects
        model.addAttribute("projects", groupService.getProjectsForGroup(groupId));
        //Add all users to the page
        model.addAttribute("users", groupService.getUserDTOInGroup(groupId));
        model.addAttribute("groupId", groupId);
        model.addAttribute("groupShortName", groupClientGRPCService.getGroup(groupId).getShortName());
        model.addAttribute("groupLongName", groupClientGRPCService.getGroup(groupId).getLongName());
        //Construct/Get repository information for this group
        Repo groupRepository = repository.getRepoForGroup(groupId);
        RepositoryDTO repoDTO = repository.constructRepoDTO(groupRepository);
        if (repoDTO == null) return "groups/groups";
        String repoLinkedMessage = repository.getRepoLinkMessage (repoDTO);
        model.addAttribute("repositoryLinked", repoDTO.getBranches() != null);
        model.addAttribute("linkedMessage", repoLinkedMessage);
        model.addAttribute("repository", repoDTO);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        if (userService.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR) {
            model.addAttribute("allProjects", projectService.getProjectsNotInGroup(groupId));
        } else {
            model.addAttribute("allProjects", new ArrayList<Project>());
        }

        return "groupSettings/groupSettings";
    }

    /**
     * Updates the repository location information. (APIKey and ProjectID)
     * @param principal AuthenticationPrincipal of the request origin
     * @param groupId ID of the group to link the repository information to
     * @param newRepo Repository information to link (only required APIKey and ProjectID)
     * @return Status code to use in fetch request
     */
    @PutMapping("api/group/{groupId}/linkRepository")
    public ResponseEntity<String> updateRepositorySettings (@AuthenticationPrincipal AuthState principal, @PathVariable int groupId, @RequestBody RepositoryDTO newRepo) {
        logger.info("Updating repository information for group {}", groupId);
        int userId = userService.getIdFromAuthState(principal);
        if (!groupService.userInGroup(principal, groupId)) {
            logger.info("User {} tried to access group settings page for group {}, but they weren't authorized", userId, groupId);
            throw new InvalidAuthorizationException();
        }
        Repo groupRepository = repository.getRepoForGroup(groupId);
        groupRepository.setRepoAPIKey(newRepo.getRepoAPIKey());
        groupRepository.setProjectId(newRepo.getProjectId());
        repository.save(groupRepository);

        return new ResponseEntity<>("Updated", HttpStatus.OK);
    }

    /**
     * Updates the repository alias for a group
     * @param principal AuthenticationPrincipal of the request origin
     * @param groupId ID of the group to link the repository information to
     * @param alias new alias for the repository name
     * @return Status code to use in fetch request
     */
    @PutMapping("api/group/{groupId}/updateRepoAlias")
    public ResponseEntity<String> updateRepoAlias (@AuthenticationPrincipal AuthState principal, @PathVariable int groupId, @RequestParam(name="alias") String alias) {
        logger.info("Updating repository alias for group {}", groupId);
        int userId = userService.getIdFromAuthState(principal);
        if (!groupService.userInGroup(principal, groupId)) {
            logger.info("User {} tried to access group settings page for group {}, but they weren't authorized", userId, groupId);
            throw new InvalidAuthorizationException();
        }
        Repo groupRepository = repository.getRepoForGroup(groupId);
        groupRepository.setRepoAlias(alias);
        repository.save(groupRepository);
        for (String user : repository.getUsersInRepository(groupRepository)) {
            System.out.println(user);
        }
        return new ResponseEntity<>("Updated", HttpStatus.OK);
    }

    /**
     * Maps passed in Commit JSON objects into thymeleaf fragments to display on the page
     * @param branchName Name of the branch that the commits belong to
     * @param body simplified JSON representation of the commit information gathered
     * @param model HTML DOM
     * @return HTML fragment for all repository commits passed into the body
     */
    @PostMapping("api/repository/commitsFragment")
    public String getCommitsForBranch (@RequestParam(name="branchName") String branchName, @RequestBody List<CommitDTO> body, Model model) {
        branchName = branchName.replaceAll("[\n\r\t]", "_");
        logger.info("Getting thymeleaf fragments for branch {}, {} commits requested", branchName, body.size());
        model.addAttribute("commits", body);
        model.addAttribute("branchName", branchName);
        return "groupSettings/repositoryCommits";
    }

    /**
     * Updates a groups details (short and long name)
     * @param principal AuthenticationPrincipal of the request origin
     * @param groupId ID of the group to change the details of
     * @param longName new long name for the group
     * @param shortName new short name for the group
     * @return HTTP status code with message (200 if all ok)
     */
    @PutMapping("api/groups/{groupId}")
    public ResponseEntity<String> updateGroupNames (@AuthenticationPrincipal AuthState principal, @PathVariable int groupId, @RequestParam(name="longName") String longName, @RequestParam(name="shortName") String shortName) {
        logger.info("Updating group names for group {}", groupId);
        int userId = userService.getIdFromAuthState(principal);
        if (!groupService.userInGroup(principal, groupId)) {
            logger.info("User {} tried to access group settings page for group {}, but they weren't authorized", userId, groupId);
            throw new InvalidAuthorizationException();
        }
        ModifyGroupDetailsResponse response = groupClientGRPCService.modifyGroupDetails(groupId, longName, shortName);
        if (response.getIsSuccess()) {
            return new ResponseEntity<>("Successfully updated group information", HttpStatus.OK);
        }
        throw new InternalServerErrorException();
    }

    @GetMapping("api/groups/{groupId}/commits")
    public String getCommitsForGroup (@PathVariable int groupId, @RequestParam(name="user", required = false) String user,
                                      @RequestParam(name="startDate", required = false) String startDate,
                                      @RequestParam(name="endDate", required = false) String endDate,
                                      @RequestParam(name="id", required = false) String commitId,
                                      Model model) throws ParseException {
        if (!repository.repoExistsForGroup(groupId)) {
            model.addAttribute("noRepository", true);
            model.addAttribute("commits", new ArrayList<>());
            return "evidence/commitList";
        }
        Repo repo = repository.getRepoForGroup(groupId);
        if (repo == null) {
            logger.info("Tried to get repository for group {}, but there wasn't any found", groupId);
            throw new NotFoundException("Couldn't find repository", groupId);
        }
        if (commitId != null) {
            model.addAttribute("noRepository", false);
            model.addAttribute("commits", repository.getCommitById(repo, commitId));
            return "evidence/commitList";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<CommitDTO> commits;
        if (user != null) {
            if (startDate != null && endDate != null) {
                commits = repository.getCommitsByDateRangeAndUser(repo, dateFormat.parse(startDate), dateFormat.parse(endDate), user);
            } else {
                commits = repository.getCommitsByUser(repo, user);
            }
        } else {
            if (startDate != null && endDate != null) {
                commits = repository.getCommitsByDateRange(repo, dateFormat.parse(startDate), dateFormat.parse(endDate));
            } else {
                commits = repository.getCommits(repo);
            }
        }
        model.addAttribute("noRepository", false);
        model.addAttribute("commits", commits);
        return "evidence/commitList";
    }

    @GetMapping("api/groups/{groupId}/usersInRepo")
    public String getUsersInRepo (@PathVariable int groupId, Model model) {
        if (!repository.repoExistsForGroup(groupId)) {
            model.addAttribute("users", new ArrayList<>());
            return "evidence/userFilterDropdown";
        }
        Repo repo = repository.getRepoForGroup(groupId);
        if (repo == null) {
            logger.info("Tried to get repository for group {}, but there wasn't any found", groupId);
            throw new NotFoundException("Couldn't find repository", groupId);
        }
        model.addAttribute("users", repository.getUsersInRepository(repo));
        return "evidence/userFilterDropdown";
    }
}
