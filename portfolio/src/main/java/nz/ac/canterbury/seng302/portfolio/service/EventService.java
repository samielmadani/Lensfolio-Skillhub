package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EventRepository;
import nz.ac.canterbury.seng302.portfolio.service.validators.EventValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;

@Service
public class EventService {

    @Autowired
    private EventRepository events;

    private final Logger logger = LoggerFactory.getLogger(EventService.class);

    /* Collection for errors to be represented to the user */
    private static final ArrayList<UserError> errs = new ArrayList<>();

    /**
     * Gets a single event from the database with the ID passed in
     * @param id ID of the event to fetch from the database
     * @return Event with ID from the database if found
     */
    public Event getEventById (int id) {
        logger.info(format("Finding event %s in the database", id));
        Event event = events.findById(id);
        if (event == null) {
            logger.error(format("Couldn't find event %s in the database", id));
        } else {
            logger.info(format("Got event %s from the database", id));
        }
        return event;
    }

    /**
     * Gets all the events that exist for a given project
     * @param projectId ID of the project to get all the events for
     * @return All the events in the project
     */
    public ArrayList<Event> getEventsForProject (int projectId) {
        logger.info(format("Getting all events for project %s", projectId));
        ArrayList<Event> projectEvents = new ArrayList<>();
        try {
            projectEvents = (ArrayList<Event>) events.findByProjectId(projectId);
            logger.info(format("Got all events for project %s (%s events)", projectId, projectEvents.size()));
        } catch (Exception e) {
            logger.error(format("Unable to find project with id: %s in the database!", projectId));
            logger.error(e.getMessage());
        }
        Collections.sort(projectEvents); // We need the events in order of start date
        return projectEvents;
    }

    /**
     * Saves an Event in the database
     * @param event Event to save in the database
     * @return Event that has been saved in the database (if successful)
     */
    public Event save (Event event) {
        logger.info(format("Saving event for project %s", event.getProjectId()));
        EventValidator.validateEvent(event); // Ensures fields are expected values

        if (hasErrors()){
            logger.info("Event cannot be saved as there was an error in one or more of the event fields");
            return null; //There has been an error in one or more of the Event fields
        }
        Event savedEvent = events.save(event);
        logger.info(format("Event %s saved for project %s", savedEvent.getEventId(), savedEvent.getProjectId()));
        return savedEvent;
    }

    /**
     * Deletes an existing Event from the database
     * @param event Event to delete (This Event should already exist in the database)
     * @return True if successful, false otherwise
     */
    public boolean delete (Event event) {
        logger.info(format("Deleting event %s for project %s", event.getEventId(), event.getProjectId()));
        try {
            events.delete(event);
            logger.info(format("Successfully deleted event %s for project %s", event.getEventId(), event.getProjectId()));
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting event!");
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Gets all the events that exist in a project between two dates
     * @param projectId ID of the project to get the events for
     * @param startDate Start date of the range you want to search for Events in
     * @param endDate End date of the range you want to search for Events in
     * @return List of all events in the project that are in the date range
     */
    public List<Event> getEventsInRange(int projectId, Date startDate, Date endDate) {
        ArrayList<Event> allEvents = getEventsForProject(projectId);
        ArrayList<Event> eventsInRange = new ArrayList<>();
        for (Event e : allEvents) {
            // THIS CODE WASN'T MY IDEA, BLAME OLIVER GARRETT (49 MAYS ROAD)
            //Prune the event dates to clamp dates within region that will always give a range that is testable and
            //predictable for the range test
            Date prunedStartDate = new Date(Math.max(e.getStartDate().getTime(), Math.min(e.getEndDate().getTime(), startDate.getTime())));
            Date prunedEndDate = new Date (Math.min(e.getEndDate().getTime(), Math.max(e.getStartDate().getTime(), endDate.getTime())));

            if (!prunedStartDate.before(startDate) && !prunedStartDate.after(endDate)) {
                eventsInRange.add(e);
            } else if (!prunedEndDate.before(startDate) && !prunedEndDate.after(endDate)) {
                eventsInRange.add(e);
            }
        }
        return eventsInRange;
    }

    public static void clearErrors () {errs.clear();}

    public static boolean hasErrors () {return !errs.isEmpty();}

    public static void addNewError (UserError error) {
        errs.add(error);}

    public static ArrayList<UserError> getCurrentErrors () {return errs;}
}
