package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.dto.GenericDetailsWithDateDto;
import nz.ac.canterbury.seng302.portfolio.dto.MinimalProjectDetailsDto;
import nz.ac.canterbury.seng302.portfolio.dto.RangeDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.*;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
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
import java.util.*;

@Controller
public class ProjectController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private ProjectService projects;
    @Autowired
    private SprintService sprints;
    @Autowired
    private GroupService groups;

    private final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    /**
     * Controller for project page.
     * @param principal Authentication token
     * @param edit Defined in URL, values can be project, sprint(number) or empty
     * @param model Page model
     * @return HTML to display
     */
    @GetMapping("/project")
    public String project(
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(name="edit", required=false, defaultValue="false") String edit,
            @RequestParam(name="projectID", required = false, defaultValue="-1") String projectIDString,
            Model model
    ) {
        if (principal == null) {
            return "redirect:/login";
        }

        if (Objects.equals(projectIDString, "-1")) {
            return "redirect:/projects";
        }

        int projectID = Integer.parseInt(projectIDString);

        //Get User Details
        int userId = userService.getIdFromAuthState(principal);
        if (!projects.hasProject(projectID)) {
            return "redirect:/projects";
        }

        Project currentProject = projects.getProjectById(projectID);

        //Get Sprints
        List<Sprint> sprintsList = currentProject.getSprints();
        model.addAttribute("sprints", sprintsList);

        //User
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        model.addAttribute("userId", userId);
        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(userId);
        } catch (StatusRuntimeException e){
            model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
            return "register";
        }
        model.addAttribute("username", userReply.getUsername());

        //Project
        model.addAttribute("project", currentProject);

        //Control
        model.addAttribute("projectErrors", ProjectService.getCurrentErrors());
        model.addAttribute("sprintErrors", SprintService.getCurrentErrors());

        return "project/project";
    }

    /**
     * API POST method to update Project details
     * @param principal Authentication token
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("api/project/{id}/Update")
    public void updateProjectDetails(
            @AuthenticationPrincipal AuthState principal,
            @PathVariable("id") int projectId,
            @RequestBody GenericDetailsWithDateDto body
    ) {
        //Prevent student from editing a project
        if (!userService.isAdmin(principal)) {
            logger.error("[updateProjectDetails] - User must be an admin to access");
            throw new ForbiddenException();
        }

        try {
            ProjectService.clearErrors();

            Project currentProject = projects.getProjectById(projectId);

            if (currentProject == null) throw new NotFoundException("project", projectId);

            // Format the dates for the current project
            Date startDate;
            Date endDate;
            startDate = DateUtil.stringToISODate(body.getStartDateString());
            endDate = DateUtil.stringToISODate(body.getEndDateString());

            if (startDate == null || endDate == null) {
                logger.error("[updateProjectDetails] - Could not format the dates.");
                throw new DomainValidationException("Could not format the dates");
            }

            currentProject.setDescription(body.getDescription());
            currentProject.setName(body.getName());
            currentProject.setStartDate(startDate);
            currentProject.setEndDate(endDate);
            projects.save(currentProject);

            if (Boolean.TRUE.equals(ProjectService.hasErrors())) {
                UserError error = ProjectService.getCurrentErrors().get(ProjectService.getCurrentErrors().size() - 1);
                logger.error("[updateProjectDetails] - {}", error.message());
                throw new DomainValidationException(error.errLocation() + error.message());
            }
        } catch (NotFoundException | DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[updateSprintDetails] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get the project minimized details.
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("api/project/{id}/MinDetails")
    public MinimalProjectDetailsDto getMinProjectDetails(@PathVariable("id") int projectId) {
        try {
            Project project = projects.getProjectById(projectId);

            if (project == null) throw new NotFoundException("project", projectId);

            MinimalProjectDetailsDto result = new MinimalProjectDetailsDto();
            result.setDescription(project.getDescription());
            result.setName(project.getName());
            result.setStartDate(project.getFormattedStartDate());
            result.setEndDate(project.getFormattedEndDate());

            return result;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[getMinProjectDetails] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }



    /**
     * Get project start date lower and upper bounds
     * @param projectId - Project to get bounds
     * @param endDateString - Optional parameter specifying the currently selected end date
     * @return - Json representation of a java List\<String\> ["lowerBound", "upperBound"]
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("api/project/getStartDateBounds")
    public List<String> getStartDateBounds(
            @RequestParam(value="projectId") int projectId,
            @RequestParam(value="endDate", defaultValue = "") String endDateString
    ) {
        List<Date> bounds = projects.getProjectStartDateLimits(projects.getProjectById(projectId));

        try {
            return projects.getJSFriendlyDates (endDateString, bounds);
        } catch (Exception e) {
            logger.error("[getStartDateBounds] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get project end date lower and upper bounds
     * @param projectId - Project to get bounds
     * @param startDateString - Optional parameter specifying the currently selected start date
     * @return - Json representation of a java List\<String\> ["lowerBound", "upperBound"]
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("api/project/getEndDateBounds")
    public List<String> getEndDateBounds(@RequestParam(value="projectId") int projectId, @RequestParam(value="startDate", defaultValue = "") String startDateString) {
        List<Date> bounds = projects.getProjectEndDateLimits(projects.getProjectById(projectId));

        SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.ISO_PATTERN, Locale.ENGLISH);
        Date startDate = null;

        if (!Objects.equals(startDateString, "")) {
            try {
                startDate = formatter.parse(startDateString);
                startDate = DateUtil.stripTimeFromDate(startDate);
                startDate = DateUtil.addDaysToDate(startDate, 1); // Must be at least the next day
            } catch (ParseException e) {
                logger.error("Failed to format date string: {}", startDateString);
            }
        }

        // Make the minimum at least the currently selected startDate
        if (startDate != null && startDate.after(bounds.get(0))) {
            bounds.set(0, startDate);
        }

        // Format to javascript array (yyyy-mm-dd)
        List<String> javaScriptFriendly = new ArrayList<>();
        javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(0)));
        javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(1)));

        logger.debug("[getEndDateBounds] - {}", javaScriptFriendly);

        return javaScriptFriendly;
    }

    /**
     * Gets all the date ranges in terms of sprints for a certain project. This function will return all non-zero ranges
     * in the project range, including all sprints as a single range each, and the amount of time between the sprints.
     * The returned list is in order of occurrence
     * @param projectId ID of the project to generate date ranges for
     * @return List of all the ranges in the project
     */
    @GetMapping("api/project/{projectId}/ranges")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<RangeDTO>> getDateRangesInProject (@PathVariable int projectId) {
        logger.info("Getting date ranges for project {}", projectId);

        if (!projects.hasProject(projectId)) {
            logger.info("Tried to get date ranges for project {} but the project couldn't be found!", projectId);
            throw new ProjectNotFoundException(projectId);
        }

        Project project = projects.getProjectById(projectId);
        List<Sprint> allSprints = sprints.getSprintsByProject(project);

        if (project == null || allSprints == null) {
            logger.info("Something went wrong in the database");
            throw new InternalServerErrorException();
        }

        List<RangeDTO> ranges = projects.getProjectRanges(project, allSprints);
        logger.info("Got all ranges in project {} ({})", projectId, ranges.size());
        return new ResponseEntity<>(ranges, HttpStatus.OK);
    }

    /**
     * API put method to link a project and group
     * @param projectId ID of the project to be linked
     * @param groupId ID of the group to be linked
     */
    @PutMapping("api/project/{projectId}/linkGroup/{groupId}")
    public ResponseEntity<String> linkProjectAndGroup(@AuthenticationPrincipal AuthState principal,
                                    @PathVariable int projectId,
                                    @PathVariable int groupId) {
        logger.info("linking project {} and group {}", projectId, groupId);

        if (principal == null) {
            throw new ForbiddenException();
        }

        if (!userService.isAdmin(principal)) {
            throw new ForbiddenException();
        }

        ProjectGroup created = projects.linkProjectAndGroup(projectId, groupId);
        for (UserDTO user : groups.getUserDTOInGroup(groupId)) {
            groups.addUserToGroupProjects(groupId, user.getId());
        }

        if (created == null) {
            return new ResponseEntity<>("Group doesn't exist", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>("Linked project and group", HttpStatus.CREATED);
    }

    /**
     * API delete method to unlink a project and group
     * @param projectId ID of the project to be unlinked
     * @param groupId ID of the group to be unlinked
     */
    @DeleteMapping("api/project/{projectId}/unlinkGroup/{groupId}")
    public ResponseEntity<String> unlinkProjectAndGroup(@AuthenticationPrincipal AuthState principal,
                                      @PathVariable int projectId,
                                      @PathVariable int groupId) {
        logger.info("unlinking project {} and group {}", projectId, groupId);

        if (principal == null) {
            throw new ForbiddenException();
        }

        if (!userService.isAdmin(principal)) {
            throw new ForbiddenException();
        }

        for (UserDTO user : groups.getUserDTOInGroup(groupId)) {
            groups.removeUserFromGroupProjects(groupId, user.getId());
        }
        projects.unlinkProjectAndGroup(projectId, groupId);

        return new ResponseEntity<>("Unlinked project and group", HttpStatus.OK);
    }

    /**
     * @param projectId ID of the project to be linked
     * @param groupId ID of the current group to be linked with the project
     * @param model model to add attributes
     * @return HTML to display
     */

    @GetMapping("api/project/{projectId}/linkedProject/{groupId}")
    public String getLinkedProjects(@PathVariable int projectId,
                                    @PathVariable int groupId,
                                    Model model) {

        Project project = projects.getProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("groupId", groupId);

        return "groupSettings/project";


    }
}
