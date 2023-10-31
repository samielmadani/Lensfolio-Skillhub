package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.model.repositories.MilestoneRepository;
import nz.ac.canterbury.seng302.portfolio.service.validators.MilestoneValidator;
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
public class MilestoneService {

    @Autowired
    private MilestoneRepository milestones;

    private final Logger logger = LoggerFactory.getLogger(MilestoneService.class);

    /* Collection for errors to be represented to the user */
    private static final ArrayList<UserError> errs = new ArrayList<>();

    /**
     * Gets a single milestone from the database with the ID passed in
     * @param id ID of the milestone to fetch from the database
     * @return Milestone with ID from the database if found
     */
    public Milestone getMilestoneById (int id) {
        logger.info(format("Finding milestone %s in the database", id));
        Milestone milestone = milestones.findById(id);
        if (milestone == null) {
            logger.error(format("Couldn't find milestone %s in the database", id));
        } else {
            logger.info(format("Got milestone %s from the database", id));
        }
        return milestone;
    }

    /**
     * Gets all the milestones that exist for a given project
     * @param projectId ID of the project to get all the milestones for
     * @return All the milestones in the project
     */
    public ArrayList<Milestone> getMilestonesForProject (int projectId) {
        logger.info(format("Getting all milestones for project %s", projectId));
        ArrayList<Milestone> projectMilestones = new ArrayList<>();
        try {
            projectMilestones = (ArrayList<Milestone>) milestones.findByProjectId(projectId);
            logger.info(format("Got all milestones for project %s (%s milestones)", projectId, projectMilestones.size()));
        } catch (Exception e) {
            logger.error(format("Unable to find project with id: %s in the database!", projectId));
            logger.error(e.getMessage());
        }
        Collections.sort(projectMilestones); // We need the milestones in order of start date
        return projectMilestones;
    }

    /**
     * Saves an Milestone in the database
     * @param milestone Milestone to save in the database
     * @return Milestone that has been saved in the database (if successful)
     */
    public Milestone save (Milestone milestone) {
        logger.info(format("Saving milestone for project %s", milestone.getProjectId()));
        MilestoneValidator.validateMilestone(milestone); // Ensures fields are expected values

        if (hasErrors()){
            logger.info("Milestone cannot be saved as there was an error in one or more of the milestone fields");
            return null; //There has been an error in one or more of the Milestone fields
        }
        Milestone savedMilestone = milestones.save(milestone);
        logger.info(format("Milestone %s saved for project %s", savedMilestone.getMilestoneId(), savedMilestone.getProjectId()));
        return savedMilestone;
    }

    /**
     * Deletes an existing Milestone from the database
     * @param milestone Milestone to delete (This Milestone should already exist in the database)
     * @return True if successful, false otherwise
     */
    public boolean delete (Milestone milestone) {
        logger.info(format("Deleting milestone %s for project %s", milestone.getMilestoneId(), milestone.getProjectId()));
        try {
            milestones.delete(milestone);
            logger.info(format("Successfully deleted milestone %s for project %s", milestone.getMilestoneId(), milestone.getProjectId()));
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting milestone!");
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Gets all the milestones that exist in a project between two dates
     * @param projectId ID of the project to get the milestones for
     * @param startDate Start date of the range you want to search for Milestones in
     * @param endDate End date of the range you want to search for Milestones in
     * @return List of all milestones in the project that are in the date range
     */
    public List<Milestone> getMilestonesInRange(int projectId, Date startDate, Date endDate) {
        ArrayList<Milestone> allMilestones = getMilestonesForProject(projectId);
        ArrayList<Milestone> milestonesInRange = new ArrayList<>();
        for (Milestone e : allMilestones) {
            if (!e.getStartDate().before(startDate) && !e.getStartDate().after(endDate)) {
                milestonesInRange.add(e);
            }
        }
        return milestonesInRange;
    }

    public static void clearErrors () {errs.clear();}

    public static boolean hasErrors () {return !errs.isEmpty();}

    public static void addNewError (UserError error) {
        errs.add(error);}

    public static ArrayList<UserError> getCurrentErrors () {return errs;}
}
