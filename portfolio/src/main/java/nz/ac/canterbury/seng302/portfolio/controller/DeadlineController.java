package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.advent.DeadlineDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserEditDeadlineDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.DeadlineService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
public class DeadlineController {

    @Autowired
    private DeadlineService deadlines;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projects;

    private final Logger logger = LoggerFactory.getLogger(DeadlineController.class);

    /**
     * HTTP Controller safe method to get deadlines for project. If there are any errors, the correct error code will
     * be thrown from here, which is why this function can't exist in the service class
     * @param projectId ID of the project to get deadlines for
     * @return List of all deadlines in the project
     */
    private List<Deadline> getDeadlinesInProject(@PathVariable int projectId) {
        logger.info("Getting all deadlines for project: {}", projectId);
        List<Deadline> allDeadlines = deadlines.getDeadlinesForProject(projectId);
        if (allDeadlines == null) {
            //allDeadlines will be null if there was a problem in the DeadlineService/DeadlineRepository class
            logger.warn("Tried to get all deadlines for project: {}, but the object couldn't be found. Check DeadlineService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //Returns with HTTP status 404
        }
        logger.info("Got all deadlines for project: {}", projectId);
        Collections.sort(allDeadlines); //Get all deadlines in the correct order by start date
        return allDeadlines;
    }

    /**
     * API endpoint for getting all the deadlines that exist for a project.
     *
     * @param projectId ID of the project to get the deadlines from
     * @return http response containing the status of the request (200 if successful) and a list of all the deadlines in the project
     */
    @GetMapping("api/project/{projectId}/deadlines")
    @ResponseStatus(HttpStatus.OK)
    public String getDeadlines(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, Model model) {
        List<Deadline> allDeadlines = getDeadlinesInProject(projectId);
        //Convert each Deadline to an DeadlineDTO
        List<DeadlineDTO> responseBody = allDeadlines.stream().map(DeadlineDTO::new).toList();
        model.addAttribute("allDeadlines", responseBody);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "deadlines/deadlines"; //HTTP status 200
    }

    /**
     * API endpoint for getting all the deadlines that exist for a project.
     *
     * @param projectId ID of the project to get the deadlines from
     * @return http response containing the status of the request (200 if successful) and a list of all the deadlines in the project
     */
    @GetMapping("api/project/{projectId}/deadlinesJSON")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<DeadlineDTO>> getDeadlinesJSON(@PathVariable int projectId) {
        List<Deadline> allDeadlines = getDeadlinesInProject(projectId);
        //Convert each Deadline to an DeadlineDTO
        return new ResponseEntity<>( allDeadlines.stream().map(DeadlineDTO::new).toList(), HttpStatus.OK);
    }

    /**
     * API GET endpoint for getting the HTML element for editing a Deadline.
     * This should be called when we want to edit a Deadline, and add the HTML that is returned to the page in place
     * of the Deadline Pane that currently exists on the page and wants to be edited.
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to get the edit template for
     * @param deadlineId Deadline to be edited
     * @param model the DOM for thymeleaf to create the new fragment
     * @return HTML fragment of the edit deadline pane
     */
    @GetMapping("api/project/{projectId}/deadlines/{deadlineId}/edit")
    @ResponseStatus(HttpStatus.OK)
    public String getEditTemplate(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int deadlineId, Model model) {
        logger.info("Getting deadline edit template for deadline {} in project {}", deadlineId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to getEditTemplate()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to create a deadline", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Deadline deadline = deadlines.getDeadlineById(deadlineId);
        if (deadline == null) {
            logger.warn("Tried to get deadline {} for project {} but the object couldn't be found", deadlineId, projectId);
            throw new DeadlineNotFoundException(deadlineId);
        }
        Project project = projects.getProjectById(projectId);
        if (project == null) {
            logger.warn("Tried to fetch project {} but the object couldn't be found", projectId);
            throw new ProjectNotFoundException(projectId);
        }
        logger.info("Created new edit template for deadline {}", deadlineId);
        model.addAttribute("deadline", new DeadlineDTO(deadline));
        model.addAttribute("start", project.getStartDateIsoString());
        model.addAttribute("end", project.getEndDateIsoString());
        return "deadlines/deadlineEditPane";
    }


    /**
     * API endpoint for creating a deadline for a project
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to add the deadline to
     * @param deadlineDTO  DTO containing the data to be created with the deadline
     * @return http response containing the status of the request (201 if successful) and the deadline object itself.
     */
    @PostMapping("api/project/{projectId}/deadlines")
    @ResponseStatus(HttpStatus.CREATED)
    public String createDeadline(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @RequestBody DeadlineDTO deadlineDTO, Model model) {
        logger.info("Creating new deadline for project: {}", projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to createDeadline()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to create a deadline", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        // Creates default deadline and then updates the deadline using the deadlineDTO in the request body so that information
        //  that is not provided in the deadlineDTO gets set to default parameters
        Deadline deadline = new Deadline(projectId);
        deadline.updateUsingDTO(deadlineDTO);
        Deadline newDeadline = deadlines.save(deadline);

        if (newDeadline == null) {
            logger.warn("Tried to create a new deadlines for project: {}, but the object couldn't be found. Check DeadlineService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Created new deadline for project: {}, with deadlineId: {}", projectId, newDeadline.getDeadlineId());
        model.addAttribute("deadline", new DeadlineDTO(deadline));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "deadlines/deadlinePane"; //HTTP status 201
    }


    /**
     * API endpoint for updating an existing deadline
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted deadline is a part of
     * @param deadlineId   ID of the deadline to edit
     * @param deadlineDTO  New data to replace old deadline data
     * @return http response containing the status of the request (201 if successful) and the updated deadline object
     */
    @PutMapping("api/project/{projectId}/deadlines/{deadlineId}")
    @ResponseStatus(HttpStatus.CREATED)
    public String updateDeadline(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int deadlineId, @RequestBody DeadlineDTO deadlineDTO, Model model) {
        logger.info("Updating deadline: {} for project: {}", deadlineId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to updateDeadline()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to update a deadline", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Deadline oldDeadline = deadlines.getDeadlineById(deadlineId); // Get old deadline
        if (oldDeadline == null) {
            logger.warn("Tried to get deadline: {} for project: {} but the deadline doesn't exist", deadlineId, projectId);
            throw new DeadlineNotFoundException(deadlineId);
        }
        oldDeadline.updateUsingDTO(deadlineDTO); // Update existing deadline with new data
        Deadline newDeadline = deadlines.save(oldDeadline);

        if (newDeadline == null) {
            logger.warn("Tried to update deadline: {} for project: {}, but the project doesn't exist", deadlineId, projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Updated deadline: {} for project: {}", newDeadline.getDeadlineId(), projectId);
        model.addAttribute("deadline", new DeadlineDTO(newDeadline));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "deadlines/deadlinePane"; //HTTP status 201
    }


    /**
     * API endpoint for deleting an existing deadline
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted deadline is a part of
     * @param deadlineId   ID of the deadline to delete
     * @return http response containing the status of the request (200 if successful) and the ID of the deadline that was deleted
     */
    @DeleteMapping("api/project/{projectId}/deadlines/{deadlineId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Integer> deleteDeadline(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int deadlineId) {
        logger.info("Deleting deadline: {} for project: {}", deadlineId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to deleteDeadline()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to delete a deadline", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Deadline deadline = deadlines.getDeadlineById(deadlineId);
        if (deadline == null) {
            logger.warn("Couldn't find deadline: {} for project: {}", deadlineId, projectId);
            throw new DeadlineNotFoundException(deadlineId);
        }

        //deadlines.delete(deadline) returns true if successful, false if not
        if (deadlines.delete(deadline)) {
            logger.info("Deadline {} successfully deleted from project: {}", deadline.getDeadlineId(), projectId);
        } else {
            logger.warn("Tried to delete a deadline for project: {}, but the object couldn't be found. Check DeadlineService and ProjectService logs for more information", projectId);
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>(deadline.getDeadlineId(), HttpStatus.OK); //HTTP status 200
    }


    /**
     * Sends HTML info passed in to all other users subscribed to the receiving endpoint.
     * This will be called when a deadline is created
     * @param projectId ID of the project to send/receive the message on
     * @param newDeadlineHTML HTML returned from API to add to page
     * @return HTML to add to page
     */
    @MessageMapping("/project/{projectId}/createDeadline")
    @SendTo("/websocketsReceive/project/{projectId}/deadlineCreated")
    public String deadlineCreated (@DestinationVariable int projectId, String newDeadlineHTML) {
        logger.info("Sending WebSocket message of creation of deadline for project {}", projectId);
        return newDeadlineHTML;
    }


    /**
     * Sends information about a deadline being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone starts to edit a deadline
     * @param projectId ID of the project to send/receive the message on
     * @param deadlineEditDTO DTO of the required information for the editing of the deadline
     * @return Information about the deadline being edited
     */
    @MessageMapping("/project/{projectId}/startDeadlineEdit")
    @SendTo("/websocketsReceive/project/{projectId}/deadlineBeingEdited")
    public UserEditDeadlineDTO deadlineStartedEdit (@DestinationVariable int projectId, UserEditDeadlineDTO deadlineEditDTO) {
        logger.info("Sending WebSocket message that user {} has started editing deadline {} in project {}", deadlineEditDTO.getUsername(), deadlineEditDTO.getDeadlineId(), projectId);
        return deadlineEditDTO;
    }


    /**
     * Sends information about an deadline being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone finished editing an deadline
     * @param projectId ID of the project to send/receive the message on
     * @param deadlineEditDTO DTO of the required information for the editing of the deadline
     * @return Information about the deadline being edited
     */
    @MessageMapping("/project/{projectId}/endDeadlineEdit")
    @SendTo("/websocketsReceive/project/{projectId}/deadlineFinishedEdit")
    public UserEditDeadlineDTO deadlineFinishedEdit (@DestinationVariable int projectId, UserEditDeadlineDTO deadlineEditDTO) {
        logger.info("Sending WebSocket message that user {} has finished editing deadline {} in project {}", deadlineEditDTO.getUsername(), deadlineEditDTO.getDeadlineId(), projectId);
        return deadlineEditDTO;
    }


    /**
     * Sends the updated HTML for an deadline that has been updated to all users subscribed to the receiving endpoint
     * This will be called when a user updates an deadlines information
     * @param projectId ID of the project to send/receive the information on
     * @param updateDeadlineHTML new HTML of the updated deadline
     * @return the new HTML of the updated deadline that has been passed in
     */
    @MessageMapping("/project/{projectId}/deadlineUpdate")
    @SendTo("/websocketsReceive/project/{projectId}/deadlineUpdated")
    public String deadlineUpdated (@DestinationVariable int projectId, String updateDeadlineHTML) {
        logger.info("Sending WebSocket message that an deadline has been updated in project {}", projectId);
        return updateDeadlineHTML;
    }


    /**
     * Sends the ID of the deadline to delete if a user deletes an deadline. This will send the message to all users
     * subscribed on the receiving endpoint with the ID of the deadline to remove.
     * @param projectId ID of the project to send/receive the information on
     * @param deadlineId ID of the deadline to remove from the page
     * @return the ID of the deadline to remove from the page
     */
    @MessageMapping("/project/{projectId}/deadlineDelete")
    @SendTo("/websocketsReceive/project/{projectId}/deadlineDeleted")
    public int deadlineDeleted (@DestinationVariable int projectId, int deadlineId) {
        logger.info("Sending WebSocket message that deadline {} has been deleted in project {}", deadlineId, projectId);
        return deadlineId;
    }

}