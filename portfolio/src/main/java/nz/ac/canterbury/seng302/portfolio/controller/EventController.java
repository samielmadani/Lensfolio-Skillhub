package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.advent.EventDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserEditEventDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
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
public class EventController {

    @Autowired
    private EventService events;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projects;

    private final Logger logger = LoggerFactory.getLogger(EventController.class);


    /**
     * HTTP Controller safe method to get events for project. If there are any errors, the correct error code will
     * be thrown from here, which is why this function can't exist in the service class
     * @param projectId ID of the project to get events for
     * @return List of all events in the project
     */
    private List<Event> getEventsForProject(@PathVariable int projectId) {
        logger.info("Getting all events for project: {}", projectId);
        List<Event> allEvents = events.getEventsForProject(projectId);
        if (allEvents == null) {
            //allEvents will be null if there was a problem in the EventService/EventRepository class
            logger.warn("Tried to get all events for project: {}, but the object couldn't be found. Check EventService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //Returns with HTTP status 404
        }
        logger.info("Got all events for project: {}", projectId);
        Collections.sort(allEvents); //Get all events in the correct order by start date
        return allEvents;
    }

    /**
     * API endpoint for getting all the events that exist for a project.
     *
     * @param projectId ID of the project to get the events from
     * @return http response containing the status of the request (200 if successful) and a list of all the events in the project
     */
    @GetMapping("api/project/{projectId}/events")
    @ResponseStatus(HttpStatus.OK)
    public String getEvents(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, Model model) {
        List<Event> allEvents = getEventsForProject(projectId);
        //Convert each Event to an EventDTO
        List<EventDTO> responseBody = allEvents.stream().map(EventDTO::new).toList();
        model.addAttribute("allEvents", responseBody);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "events/events"; //HTTP status 200
    }

    /**
     * API endpoint for getting all the events in a JSON format that exist for a project.
     *
     * @param projectId ID of the project to get the events from
     * @return http response containing the status of the request (200 if successful) and a list of all the events in the project
     */
    @GetMapping("api/project/{projectId}/eventsJSON")
    public ResponseEntity<List<EventDTO>> getEvents(@PathVariable int projectId) {
        List<Event> allEvents = getEventsForProject(projectId);
        //Convert each Event to an EventDTO
        return new ResponseEntity<>(allEvents.stream().map(EventDTO::new).toList(), HttpStatus.OK);
    }

    /**
     * API GET endpoint for getting the HTML element for editing an Event.
     * This should be called when we want to edit an Event, and add the HTML that is returned to the page in place
     * of the Event Pane that currently exists on the page and wants to be edited.
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to get the edit template for
     * @param eventId Event to be edited
     * @param model the DOM for thymeleaf to create the new fragment
     * @return HTML fragment of the edit event pane
     */
    @GetMapping("api/project/{projectId}/events/{eventId}/edit")
    @ResponseStatus(HttpStatus.OK)
    public String getEditTemplate(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int eventId, Model model) {
        logger.info("Getting event edit template for event {} in project {}", eventId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to createEvent()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to create an event", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Event event = events.getEventById(eventId);
        if (event == null) {
            logger.warn("Tried to get event {} for project {} but the object couldn't be found", eventId, projectId);
            throw new EventNotFoundException(eventId);
        }
        Project project = projects.getProjectById(projectId);
        if (project == null) {
            logger.warn("Tried to fetch project {} but the object couldn't be found", projectId);
            throw new ProjectNotFoundException(projectId);
        }
        logger.info("Created new edit template for event {}", eventId);
        model.addAttribute("event", new EventDTO(event));
        model.addAttribute("start", project.getStartDateIsoString());
        model.addAttribute("end", project.getEndDateIsoString());
        return "events/eventEditPane";
    }


    /**
     * API endpoint for creating an event for a project
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project to add the event to
     * @param eventDTO  DTO containing the data to be created with the event
     * @return http response containing the status of the request (201 if successful) and the event object itself.
     */
    @PostMapping("api/project/{projectId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public String createEvent(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @RequestBody EventDTO eventDTO, Model model) {
        logger.info("Creating new event for project: {}", projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to createEvent()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to create an event", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        // Creates default event and then updates the event using the eventDTO in the request body so that information
        //  that is not provided in the eventDTO gets set to default parameters
        Event event = new Event(projectId);
        event.updateUsingDTO(eventDTO);
        Event newEvent = events.save(event);

        if (newEvent == null) {
            logger.warn("Tried to create a new event for project: {}, but the object couldn't be found. Check EventService and ProjectService logs for more information", projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Created new event for project: {}, with eventId: {}", projectId, newEvent.getEventId());
        model.addAttribute("event", new EventDTO(event));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "events/eventPane"; //HTTP status 201
    }


    /**
     * API endpoint for updating an existing event
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted event is a part of
     * @param eventId   ID of the event to edit
     * @param eventDTO  New data to replace old event data
     * @return http response containing the status of the request (201 if successful) and the updated event object
     */
    @PutMapping("api/project/{projectId}/events/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public String updateEvent(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int eventId, @RequestBody EventDTO eventDTO, Model model) {
        logger.info("Updating event: {} for project: {}", eventId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to updateEvent()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to update an event", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Event oldEvent = events.getEventById(eventId); // Get old event
        if (oldEvent == null) {
            logger.warn("Tried to get event: {} for project: {} but the event doesn't exist", eventId, projectId);
            throw new EventNotFoundException(eventId);
        }
        oldEvent.updateUsingDTO(eventDTO); // Update existing event with new data
        Event newEvent = events.save(oldEvent);

        if (newEvent == null) {
            logger.warn("Tried to update event: {} for project: {}, but the project doesn't exist", eventId, projectId);
            throw new ProjectNotFoundException(projectId); //404
        }
        logger.info("Updated event: {} for project: {}", newEvent.getEventId(), projectId);
        model.addAttribute("event", new EventDTO(newEvent));
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "events/eventPane"; //HTTP status 201
    }


    /**
     * API endpoint for deleting an existing event
     *
     * @param principal Authentication handler, should be automatically sent with an API request
     * @param projectId ID of the project that the targeted event is a part of
     * @param eventId   ID of the event to delete
     * @return http response containing the status of the request (200 if successful) and the ID of the event that was deleted
     */
    @DeleteMapping("api/project/{projectId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Integer> deleteEvent(@AuthenticationPrincipal AuthState principal, @PathVariable int projectId, @PathVariable int eventId) {
        logger.info("Deleting event: {} for project: {}", eventId, projectId);

        //Tests if the user sending the request has the required Authorization from the server to perform the request
        if (principal == null) {
            logger.info("Invalid AuthState principal passed in to deleteEvent()");
            throw new InvalidAuthorizationException(); //HTTP status 400
        }
        if (!userService.isAdmin(principal)) {
            logger.info("User {} has not been authorized to delete an event", userService.getIdFromAuthState(principal));
            throw new UserNotAuthorizedException(); //Returns with status 401
        }

        Event event = events.getEventById(eventId);
        if (event == null) {
            logger.warn("Couldn't find event: {} for project: {}", eventId, projectId);
            throw new EventNotFoundException(eventId);
        }

        //events.delete(event) returns true if successful, false if not
        if (events.delete(event)) {
            logger.info("Event {} successfully deleted from project: {}", event.getEventId(), projectId);
        } else {
            logger.warn("Tried to delete an event for project: {}, but the object couldn't be found. Check EventService and ProjectService logs for more information", event.getEventId());
            throw new InternalServerErrorException();
        }

        return new ResponseEntity<>(event.getEventId(), HttpStatus.OK); //HTTP status 200
    }


    /**
     * Sends HTML info passed in to all other users subscribed to the receiving endpoint.
     * This will be called when an event is created
     * @param projectId ID of the project to send/receive the message on
     * @param newEventHTML HTML returned from API to add to page
     * @return HTML to add to page
     */
    @MessageMapping("/project/{projectId}/createEvent")
    @SendTo("/websocketsReceive/project/{projectId}/eventCreated")
    public String eventCreated (@DestinationVariable int projectId, String newEventHTML) {
        logger.info("Sending WebSocket message of creation of event for project {}", projectId);
        return newEventHTML;
    }


    /**
     * Sends information about an event being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone starts to edit an event
     * @param projectId ID of the project to send/receive the message on
     * @param eventEditDTO DTO of the required information for the editing of the event
     * @return Information about the event being edited
     */
    @MessageMapping("/project/{projectId}/startEventEdit")
    @SendTo("/websocketsReceive/project/{projectId}/eventBeingEdited")
    public UserEditEventDTO eventStartedEdit (@DestinationVariable int projectId, UserEditEventDTO eventEditDTO) {
        logger.info("Sending WebSocket message that user {} has started editing event {} in project {}", eventEditDTO.getUsername(), eventEditDTO.getEventId(), projectId);
        return eventEditDTO;
    }


    /**
     * Sends information about an event being edited to all users subscribed to the receiving endpoint.
     * This will be called when someone finished editing an event
     * @param projectId ID of the project to send/receive the message on
     * @param eventEditDTO DTO of the required information for the editing of the event
     * @return Information about the event being edited
     */
    @MessageMapping("/project/{projectId}/endEventEdit")
    @SendTo("/websocketsReceive/project/{projectId}/eventFinishedEdit")
    public UserEditEventDTO eventFinishedEdit (@DestinationVariable int projectId, UserEditEventDTO eventEditDTO) {
        logger.info("Sending WebSocket message that user {} has finished editing event {} in project {}", eventEditDTO.getUsername(), eventEditDTO.getEventId(), projectId);
        return eventEditDTO;
    }


    /**
     * Sends the updated HTML for an event that has been updated to all users subscribed to the receiving endpoint
     * This will be called when a user updates an events information
     * @param projectId ID of the project to send/receive the information on
     * @param updateEventHTML new HTML of the updated event
     * @return the new HTML of the updated event that has been passed in
     */
    @MessageMapping("/project/{projectId}/eventUpdate")
    @SendTo("/websocketsReceive/project/{projectId}/eventUpdated")
    public String eventUpdated (@DestinationVariable int projectId, String updateEventHTML) {
        logger.info("Sending WebSocket message that an event has been updated in project {}", projectId);
        return updateEventHTML;
    }


    /**
     * Sends the ID of the event to delete if a user deletes an event. This will send the message to all users
     * subscribed on the receiving endpoint with the ID of the event to remove.
     * @param projectId ID of the project to send/receive the information on
     * @param eventId ID of the event to remove from the page
     * @return the ID of the event to remove from the page
     */
    @MessageMapping("/project/{projectId}/eventDelete")
    @SendTo("/websocketsReceive/project/{projectId}/eventDeleted")
    public int eventDeleted (@DestinationVariable int projectId, int eventId) {
        logger.info("Sending WebSocket message that event {} has been deleted in project {}", eventId, projectId);
        return eventId;
    }

}