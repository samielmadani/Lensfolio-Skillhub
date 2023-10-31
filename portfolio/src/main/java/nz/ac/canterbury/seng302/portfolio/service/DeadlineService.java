package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.repositories.DeadlineRepository;
import nz.ac.canterbury.seng302.portfolio.service.validators.DeadlineValidator;
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
public class DeadlineService {

    @Autowired
    private DeadlineRepository deadlines;

    private final Logger logger = LoggerFactory.getLogger(DeadlineService.class);

    /* Collection for errors to be represented to the user */
    private static final ArrayList<UserError> errs = new ArrayList<>();

    /**
     * Gets a single deadline from the database with the ID passed in
     * @param id ID of the deadline to fetch from the database
     * @return Deadline with ID from the database if found
     */
    public Deadline getDeadlineById (int id) {
        logger.info(format("Finding deadline %s in the database", id));
        Deadline deadline = deadlines.findById(id);
        if (deadline == null) {
            logger.error(format("Couldn't find deadline %s in the database", id));
        } else {
            logger.info(format("Got deadline %s from the database", id));
        }
        return deadline;
    }

    /**
     * Gets all the deadlines that exist for a given project
     * @param projectId ID of the project to get all the deadlines for
     * @return All the deadlines in the project
     */
    public ArrayList<Deadline> getDeadlinesForProject (int projectId) {
        logger.info(format("Getting all deadlines for project %s", projectId));
        ArrayList<Deadline> projectDeadlines = new ArrayList<>();
        try {
            projectDeadlines = (ArrayList<Deadline>) deadlines.findByProjectId(projectId);
            logger.info(format("Got all deadlines for project %s (%s deadlines)", projectId, projectDeadlines.size()));
        } catch (Exception e) {
            logger.error(format("Unable to find project with id: %s in the database!", projectId));
            logger.error(e.getMessage());
        }
        Collections.sort(projectDeadlines); // We need the deadlines in order of start date
        return projectDeadlines;
    }

    /**
     * Saves a deadline in the database
     * @param deadline Deadline to save in the database
     * @return Deadline that has been saved in the database (if successful)
     */
    public Deadline save (Deadline deadline) {
        logger.info(format("Saving Deadline for project %s", deadline.getProjectId()));
        DeadlineValidator.validateDeadline(deadline); // Ensures fields are expected values

        if (hasErrors()){
            logger.info("Deadline cannot be saved as there was an error in one or more of the deadline fields");
            return null; //There has been an error in one or more of the Deadline fields
        }
        Deadline savedDeadline = deadlines.save(deadline);
        logger.info(format("Deadline %s saved for project %s", savedDeadline.getDeadlineId(), savedDeadline.getProjectId()));
        return savedDeadline;
    }

    /**
     * Deletes an existing deadline from the database
     * @param deadline Deadline to delete (This deadline should already exist in the database)
     * @return True if successful, false otherwise
     */
    public boolean delete (Deadline deadline) {
        logger.info(format("Deleting deadline %s for project %s", deadline.getDeadlineId(), deadline.getProjectId()));
        try {
            deadlines.delete(deadline);
            logger.info(format("Successfully deleted deadline %s for project %s", deadline.getDeadlineId(), deadline.getProjectId()));
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting deadline!");
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Gets all the deadlines that exist in a project between two dates
     * @param projectId ID of the project to get the deadlines for
     * @param startDate Start date of the range you want to search for deadlines in
     * @param endDate End date of the range you want to search for deadlines in
     * @return List of all deadlines in the project that are in the date range
     */
    public List<Deadline> getDeadlinesInRange(int projectId, Date startDate, Date endDate) {
        ArrayList<Deadline> allDeadlines = getDeadlinesForProject(projectId);
        ArrayList<Deadline> deadlinesInRange = new ArrayList<>();
        for (Deadline d : allDeadlines) {
            if (!d.getStartDate().before(startDate) && !d.getStartDate().after(endDate)) deadlinesInRange.add(d);
        }
        return deadlinesInRange;
    }

    public static void clearErrors () {errs.clear();}

    public static boolean hasErrors () {return !errs.isEmpty();}

    public static void addNewError (UserError error) {
        errs.add(error);}

    public static ArrayList<UserError> getCurrentErrors () {return errs;}
}
