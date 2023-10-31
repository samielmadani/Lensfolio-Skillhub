package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.advent.AdventDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AdventService {

    private final Logger logger = LoggerFactory.getLogger(AdventService.class);

    @Autowired
    private ProjectService projects;
    @Autowired
    private EventService events;
    @Autowired
    private DeadlineService deadlines;
    @Autowired
    private MilestoneService milestones;

    /**
     * Gets all the advents in a certain range for a project. The returned list will be sorted based on the time that
     * each AdventDTO has in the date field.
     * @param projectId ID of the project to get the range of advents for
     * @param start Start date of the range
     * @param end End date of the range
     * @return Sorted list of all the advents found in the range for the project
     */
    public List<AdventDTO> getAdventInRange (int projectId, Date start, Date end) {
        logger.info ("Getting advents in range for project {}", projectId);
        if (!projects.hasProject(projectId)) {
            logger.info ("Tried to get advents in range for project {} but the project couldn't be found", projectId);
            return Collections.emptyList();
        }
        //Get Events
        List<Event> eventsInRange = events.getEventsInRange(projectId, start, end);
        List<AdventDTO> eventDTOs = eventsInRange.stream().map(AdventDTO::new).toList();
        logger.info("Got {} events in range", eventDTOs.size());
        //Get Deadlines
        List<Deadline> deadlinesInRange = deadlines.getDeadlinesInRange(projectId, start, end);
        List<AdventDTO> deadlineDTOs = deadlinesInRange.stream().map(AdventDTO::new).toList();
        logger.info("Got {} deadlines in range", deadlineDTOs.size());
        //Get Milestones
        List<Milestone> milestonesInRange = milestones.getMilestonesInRange(projectId, start, end);
        List<AdventDTO> milestoneDTOs = milestonesInRange.stream().map(AdventDTO::new).toList();
        logger.info("Got {} milestones in range", milestoneDTOs.size());

        List<AdventDTO> eventsAndDeadlines = Stream.concat(eventDTOs.stream(), deadlineDTOs.stream()).toList();
        List<AdventDTO> allAdvents = new java.util.ArrayList<>(Stream.concat(eventsAndDeadlines.stream(), milestoneDTOs.stream()).toList());
        Collections.sort(allAdvents);
        logger.info("Got {} advents for project {} in range {} - {}", allAdvents.size(), projectId, start, end);
        return allAdvents;
    }
}
