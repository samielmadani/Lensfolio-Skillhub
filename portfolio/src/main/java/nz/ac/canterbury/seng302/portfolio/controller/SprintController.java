package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.GenericDetailsWithDateDto;
import nz.ac.canterbury.seng302.portfolio.exceptions.DomainValidationException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ForbiddenException;
import nz.ac.canterbury.seng302.portfolio.exceptions.NotFoundException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ServerException;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Controller
public class SprintController {
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectService projects;
    @Autowired
    private SprintService sprints;

    private final Logger logger = LoggerFactory.getLogger(SprintController.class);

    /**
     * Creates a new blank sprint.
     * @param principal Authentication token
     * @return SprintId
     */
    @ResponseBody
    @PostMapping("/api/sprint/{projectId}")
    @ResponseStatus(HttpStatus.CREATED)
    public int createSprint(@AuthenticationPrincipal AuthState principal, @PathVariable("projectId") int projectId) {
        //Prevent student from adding a sprint
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to access");
            throw new ForbiddenException();
        }

        SprintService.clearErrors();

        Project currentProject = projects.getProjectById(projectId);

        if (currentProject == null) throw new NotFoundException("project", projectId);

        Sprint newSprint = new Sprint(currentProject);

        if (Boolean.TRUE.equals(SprintService.hasErrors())) {
            throw new DomainValidationException(
                    SprintService.getCurrentErrors().get(SprintService.getCurrentErrors().size()-1)
                            .message());
        }

        newSprint = sprints.save(newSprint);

        if (Boolean.TRUE.equals(SprintService.hasErrors())) {
            throw new DomainValidationException(
                    SprintService.getCurrentErrors().get(SprintService.getCurrentErrors().size()-1)
                            .message());
        }

        return newSprint.getId();
    }



    /**
     * API POST method to update Sprint details
     * @param principal Authentication token
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/api/sprint/{id}/Update")
    public void updateSprintDetails(
            @AuthenticationPrincipal AuthState principal,
            @PathVariable("id") int sprintId,
            @RequestBody GenericDetailsWithDateDto body
    ) {
        //Prevent student from editing a sprint
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to access");
            throw new ForbiddenException();
        }

        try {
            SprintService.clearErrors();

            Sprint sprint = sprints.getSprintById(sprintId);

            if (sprint == null) throw new NotFoundException("sprint", sprintId);

            // Format dates
            Date startDate;
            Date endDate;
            startDate = DateUtil.stringToISODate(body.getStartDateString());
            endDate = DateUtil.stringToISODate(body.getEndDateString());

            if (startDate == null || endDate == null) {
                SprintService.addNewError(new UserError("updateSprintDetails", "Error formatting the dates"));
                logger.error("[updateSprintDetails] - Failed to format the sprint dates.");
                throw new DomainValidationException("Failed to format the sprint dates.");
            }

            // Update database
            sprint.setStartDate(startDate);
            sprint.setEndDate(endDate);
            sprint.setDescription(body.getDescription());
            sprint.setName(body.getName());
            sprints.save(sprint);

            if (Boolean.TRUE.equals(SprintService.hasErrors())) {
                String message = SprintService.getCurrentErrors()
                        .get(SprintService.getCurrentErrors().size() - 1).message();
                logger.error("[updateSprintDetails] - {}", message);
                throw new DomainValidationException(message);
            }
        } catch (NotFoundException | DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[updateSprintDetails] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Fetch a sprint label by id
     */
    @ResponseBody
    @GetMapping("/api/sprint/{id}/Label")
    @ResponseStatus(HttpStatus.OK)
    public String getSprintLabel(@PathVariable("id") int sprintId) {
        try {
            Sprint sprint = sprints.getSprintById(sprintId);
            if (sprint == null) {
                throw new NotFoundException("sprint", sprintId);
            }

            return sprint.getLabel();
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[getSprintDetails] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * API DELETE method to delete a sprint
     * @param principal Authentication token
     */
    @DeleteMapping("/api/sprint/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSprint(
            @AuthenticationPrincipal AuthState principal,
            @PathVariable("id") int sprintId
    ) {
        //Prevent student from deleting a sprint
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to access");
            throw new ForbiddenException();
        }

        try {
            Sprint sprint = sprints.getSprintById(sprintId);
            if (sprint == null) {
                throw new NotFoundException("sprint", sprintId);
            }

            sprints.delete(sprint);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[deleteSprint] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get a sprint HTML component
     * @param principal Authentication token
     * @return project/sprintPartial HTML component
     */
    @GetMapping("/sprint/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String fetchSprintPartial (
            @AuthenticationPrincipal AuthState principal, @PathVariable("id") int id, Model model
    ) {
        Sprint sprint;
        try {
            sprint = sprints.getSprintById(id);

            if (sprint == null) throw new NotFoundException("sprint", id);

            model.addAttribute("sprint", sprint);
            model.addAttribute("isAdmin", userService.isAdmin(principal));
        } catch (Exception e) {
            logger.error("[fetchSprint] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }

        return "project/sprintPartial";
    }

    /**
     * Get sprint start date lower and upper bounds
     * @param sprintId - Sprint to get bounds
     * @return - Json representation of a java List\<String\> ["lowerBound", "upperBound"]
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/sprint/getStartDateBounds")
    public List<String> getStartDateBounds(
            @RequestParam(value="sprintId") int sprintId,
            @RequestParam(value="endDate", defaultValue = "") String endDateString) {
        try {
            List<Date> bounds = sprints.getSprintStartDateBounds(sprints.getSprintById(sprintId));

            return projects.getJSFriendlyDates (endDateString, bounds);
        } catch (Exception e) {
            logger.error("[getStartDateBounds] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get sprint end date lower and upper bounds
     * @param sprintId - Sprint to get bounds
     * @param startDateString - Optional parameter specifying the currently selected start date
     * @return - Json representation of a java List\<String\> ["lowerBound", "upperBound"]
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/sprint/getEndDateBounds")
    public List<String> getEndDateBounds(@RequestParam(value="sprintId") int sprintId, @RequestParam(value="startDate", defaultValue = "") String startDateString) {
        try {
            List<Date> bounds = sprints.getSprintEndDateBounds(sprints.getSprintById(sprintId));

            Date startDate = null;
            if (!Objects.equals(startDateString, "")) {
                startDate = DateUtil.stringToISODate(startDateString);
                startDateString = startDateString.replaceAll("[\n\r\t]", "_");
                if (startDate == null) logger.error("Failed to format date string: {}", startDateString);

                startDate = DateUtil.stripTimeFromDate(startDate);
                startDate = DateUtil.addDaysToDate(startDate, 1); // Must be at least the next day
            }

            // Make the minimum at least the currently selected startDate
            if (startDate != null && startDate.after(bounds.get(0))) {
                bounds.set(0, startDate);
            }

            // Set the end date of the sprint same as the end date of the project date
            bounds.set(1, DateUtil.addDaysToDate(bounds.get(1), 0));

            // Format to javascript array (yyyy-mm-dd)
            List<String> javaScriptFriendly = new ArrayList<>();
            javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(0)));
            javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(1)));

            logger.debug("[getEndDateBounds] - {}", javaScriptFriendly);

            return javaScriptFriendly;
        } catch (Exception e) {
            logger.error("[getEndDateBounds] - {}", e.getMessage());
            throw new ServerException("Cannot create sprint out of project range date!");
        }
    }

    /**
     * Get a list of all sprint IDs within a given project.
     * @return JSON array of sprint ID integers
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/api/sprint/getSprintIds/{projectId}")
    public List<Integer> getSprintIds(@PathVariable("projectId") int projectId) {
        List<Integer> result = new ArrayList<>();

        try {
            if (!projects.hasProject(projectId))
                throw new NotFoundException("project", projectId);

            List<Sprint> sprintList = sprints.getSprintsByProject(projects.getProjectById(projectId));

            for (Sprint sprint : sprintList) {
                result.add(sprint.getId());
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[getSprintIds] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }

        return result;
    }
}
