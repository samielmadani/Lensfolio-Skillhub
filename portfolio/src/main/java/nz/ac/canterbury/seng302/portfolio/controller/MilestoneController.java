package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.advent.MilestoneDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserEditMilestoneDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.MilestoneService;
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
public class MilestoneController {

    @Autowired
    private MilestoneService milestones;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projects;

    private final Logger logger = LoggerFactory.getLogger(MilestoneController.class);

    /**
     * HTTP Controller safe method to get milestones for project. If there are any errors, the correct error code will
     * be thrown from here, which is why this function can't exist in the service class
     * @param projectId ID of the project to get milestones for
     * @return List of all milestones in the project
     */
    private List<Milestone> getMilestonesForProject(@PathVariable int projectId) {
        logger.info("Getting all milestones for project: {}", projectId);
        List<Milestone> allMilestones = milestones.getMilestonesForProject(projectId);
        if (allMilestones == null) {
            //allMilestones will be null if there was a problem in the MilestoneService/MilestoneRepository class
            logger.warn("Tried to get all milestones for project: {}, but the object couldn't be found. Check MilestoneService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //Returns with HTTP status 404
        }
        logger.info("Got all milestones for project: {}", projectId);
        Collections.sort(allMilestones); //Get all milestones in the correct order by start date
        return allMilestones;
    }

    /**
     * API endpoint for getting all the milestones that exist for a project.
     *
     * @param projectId ID of the project to get the milestones from
     * @return http response containing the status of the request (200 if successful) and a list of all the milestones in the project
     */
    @GetMapping("api/project/{projectId}/milestonesJSON")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<MilestoneDTO>> getMilestonesJSON(@PathVariable int projectId) {
        List<Milestone> allMilestones = getMilestonesForProject(projectId);
        //Convert each Milestone to an MilestoneDTO
        return new ResponseEntity<>(allMilestones.stream().map(MilestoneDTO::new).toList(), HttpStatus.OK);
    }

    /**
     * API endpoint for getting all the milestones that exist for a project.
     *
     * @param projectId ID of the project to get the milestones from
     * @return http response containing the status of the request (200 if successful) and a list of all the milestones in the project
     */
    @GetMapping("api/project/{projectId}/milestones")
    @ResponseStatus(HttpStatus.OK)
    public String getMilestones(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, Model model) {
        List<Milestone> allMilestones = getMilestonesForProject(projectId);
        //Convert each Milestone to an MilestoneDTO
        List<MilestoneDTO> responseBody = allMilestones.stream().map(MilestoneDTO::new).toList();
        model.addAttribute("allMilestones", responseBody);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "milestones/milestones"; //HTTP status 200
    }

    /**
     * API GET endpoint for getting the HTML element for editing a Milestone.
     * This should be called when we want to edit a Milestone, and add the HTML that is returned to the page in place
     * of the Milestone Pane that currently exists on the page and wants to be edited.
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to get the edit template for
     * @param milestoneId Milestone to be edited
     * @param model the DOM for thymeleaf to create the new fragment
     * @return HTML fragment of the edit milestone pane
     */
    @GetMapping("api/project/{projectId}/milestones/{milestoneId}/edit")
    @ResponseStatus(HttpStatus.OK)
    public String getEditTemplate(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int milestoneId, Model model) {
        logger.info("Getting milestone edit template for milestone {} in project {}", milestoneId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to getEditTemplate()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to edit a milestone", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Milestone milestone = milestones.getMilestoneById(milestoneId);
        if (milestone == null) {
            logger.warn("Tried to get milestone {} for project {} but the object couldn't be found", milestoneId, projectId);
            throw new MilestoneNotFoundException(milestoneId);
        }
        Project project = projects.getProjectById(projectId);
        if (project == null) {
            logger.warn("Tried to fetch project {} but the object couldn't be found", projectId);
            throw new ProjectNotFoundException(projectId);
        }
        logger.info("Created new edit template for milestone {}", milestoneId);
        model.addAttribute("milestone", new MilestoneDTO(milestone));
        model.addAttribute("start", project.getStartDateIsoString());
        model.addAttribute("end", project.getEndDateIsoString());
        return "milestones/milestoneEditPane";
    }


    /**
     * API endpoint for creating an milestone for a project
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to add the milestone to
     * @param milestoneDTO  DTO containing the data to be created with the milestone
     * @return http response containing the status of the request (201 if successful) and the milestone object itself.
     */
    @PostMapping("api/project/{projectId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    public String createMilestone(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @RequestBody MilestoneDTO milestoneDTO, Model model) {
        logger.info("Creating new milestone for project: {}", projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to createMilestone()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to create a milestone", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        // Creates default milestone and then updates the milestone using the milestoneDTO in the request body so that information
        //  that is not provided in the milestoneDTO gets set to default parameters
        Milestone milestone = new Milestone(projectId);
        milestone.updateUsingDTO(milestoneDTO);
        Milestone newMilestone = milestones.save(milestone);

        if (newMilestone == null) {
            logger.warn("Tried to create a new milestone for project: {}, but the object couldn't be found. Check MilestoneService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Created new milestone for project: {}, with milestoneId: {}", projectId, newMilestone.getMilestoneId());
        model.addAttribute("milestone", new MilestoneDTO(milestone));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "milestones/milestonePane"; //HTTP status 201
    }


    /**
     * API endpoint for updating an existing milestone
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted milestone is a part of
     * @param milestoneId   ID of the milestone to edit
     * @param milestoneDTO  New data to replace old milestone data
     * @return http response containing the status of the request (201 if successful) and the updated milestone object
     */
    @PutMapping("api/project/{projectId}/milestones/{milestoneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public String updateMilestone(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int milestoneId, @RequestBody MilestoneDTO milestoneDTO, Model model) {
        logger.info("Updating milestone: {} for project: {}", milestoneId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to updateMilestone()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to update a milestone", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Milestone oldMilestone = milestones.getMilestoneById(milestoneId); // Get old milestone
        if (oldMilestone == null) {
            logger.warn("Tried to get milestone: {} for project: {} but the milestone doesn't exist", milestoneId, projectId);
            throw new MilestoneNotFoundException(milestoneId);
        }
        oldMilestone.updateUsingDTO(milestoneDTO); // Update existing milestone with new data
        Milestone newMilestone = milestones.save(oldMilestone);

        if (newMilestone == null) {
            logger.warn("Tried to update milestone: {} for project: {}, but the project doesn't exist", milestoneId, projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Updated milestone: {} for project: {}", newMilestone.getMilestoneId(), projectId);
        model.addAttribute("milestone", new MilestoneDTO(newMilestone));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "milestones/milestonePane"; //HTTP status 201
    }


    /**
     * API endpoint for deleting an existing milestone
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted milestone is a part of
     * @param milestoneId   ID of the milestone to delete
     * @return http response containing the status of the request (200 if successful) and the ID of the milestone that was deleted
     */
    @DeleteMapping("api/project/{projectId}/milestones/{milestoneId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Integer> deleteMilestone(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int milestoneId) {
        logger.info("Deleting milestone: {} for project: {}", milestoneId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to deleteMilestone()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to delete a milestone", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Milestone milestone = milestones.getMilestoneById(milestoneId);
        if (milestone == null) {
            logger.warn("Couldn't find milestone: {} for project: {}", milestoneId, projectId);
            throw new MilestoneNotFoundException(milestoneId);
        }

        //milestones.delete(milestone) returns true if successful, false if not
        if (milestones.delete(milestone)) {
            logger.info("Milestone {} successfully deleted from project: {}", milestone.getMilestoneId(), projectId);
        } else {
            logger.warn("Tried to delete milestone: {}, but the object couldn't be found. Check MilestoneService and ProjectService logs for more information", milestone.getMilestoneId());
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>(milestone.getMilestoneId(), HttpStatus.OK); //HTTP status 200
    }


    /**
     * Sends HTML info passed in to all other users subscribed to the receiving endpoint.
     * This will be called when an milestone is created
     * @param projectId ID of the project to send/receive the message on
     * @param newMilestoneHTML HTML returned from API to add to page
     * @return HTML to add to page
     */
    @MessageMapping("/project/{projectId}/createMilestone")
    @SendTo("/websocketsReceive/project/{projectId}/milestoneCreated")
    public String milestoneCreated (@DestinationVariable int projectId, String newMilestoneHTML) {
        logger.info("Sending WebSocket message of creation of milestone for project {}", projectId);
        return newMilestoneHTML;
    }


    /**
     * Sends information about an milestone being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone starts to edit an milestone
     * @param projectId ID of the project to send/receive the message on
     * @param milestoneEditDTO DTO of the required information for the editing of the milestone
     * @return Information about the milestone being edited
     */
    @MessageMapping("/project/{projectId}/startMilestoneEdit")
    @SendTo("/websocketsReceive/project/{projectId}/milestoneBeingEdited")
    public UserEditMilestoneDTO milestoneStartedEdit (@DestinationVariable int projectId, UserEditMilestoneDTO milestoneEditDTO) {
        logger.info("Sending WebSocket message that user {} has started editing milestone {} in project {}", milestoneEditDTO.getUsername(), milestoneEditDTO.getMilestoneId(), projectId);
        return milestoneEditDTO;
    }


    /**
     * Sends information about an milestone being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone finished editing an milestone
     * @param projectId ID of the project to send/receive the message on
     * @param milestoneEditDTO DTO of the required information for the editing of the milestone
     * @return Information about the milestone being edited
     */
    @MessageMapping("/project/{projectId}/endMilestoneEdit")
    @SendTo("/websocketsReceive/project/{projectId}/milestoneFinishedEdit")
    public UserEditMilestoneDTO milestoneFinishedEdit (@DestinationVariable int projectId, UserEditMilestoneDTO milestoneEditDTO) {
        logger.info("Sending WebSocket message that user {} has finished editing milestone {} in project {}", milestoneEditDTO.getUsername(), milestoneEditDTO.getMilestoneId(), projectId);
        return milestoneEditDTO;
    }


    /**
     * Sends the updated HTML for an milestone that has been updated to all users subscribed to the receiving endpoint
     * This will be called when a user updates an milestones information
     * @param projectId ID of the project to send/receive the information on
     * @param updateMilestoneHTML new HTML of the updated milestone
     * @return the new HTML of the updated milestone that has been passed in
     */
    @MessageMapping("/project/{projectId}/milestoneUpdate")
    @SendTo("/websocketsReceive/project/{projectId}/milestoneUpdated")
    public String milestoneUpdated (@DestinationVariable int projectId, String updateMilestoneHTML) {
        logger.info("Sending WebSocket message that an milestone has been updated in project {}", projectId);
        return updateMilestoneHTML;
    }


    /**
     * Sends the ID of the milestone to delete if a user deletes an milestone. This will send the message to all users
     * subscribed on the receiving endpoint with the ID of the milestone to remove.
     * @param projectId ID of the project to send/receive the information on
     * @param milestoneId ID of the milestone to remove from the page
     * @return the ID of the milestone to remove from the page
     */
    @MessageMapping("/project/{projectId}/milestoneDelete")
    @SendTo("/websocketsReceive/project/{projectId}/milestoneDeleted")
    public int milestoneDeleted (@DestinationVariable int projectId, int milestoneId) {
        logger.info("Sending WebSocket message that milestone {} has been deleted in project {}", milestoneId, projectId);
        return milestoneId;
    }

}