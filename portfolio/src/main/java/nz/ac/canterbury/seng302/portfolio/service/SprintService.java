package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.repositories.SprintRepository;
import nz.ac.canterbury.seng302.portfolio.service.validators.SprintValidator;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SprintService {
    private final String DATE_FORMAT = "yyyy-MM-dd";
    private final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);

    @Autowired
    private SprintRepository sprints;

    /* Collection for errors to be represented to the user */
    private static final ArrayList<UserError> errs = new ArrayList<>();

    /**
     * Get a sprint from the database by its Id
     * @param id - Id of the sprint
     * @return - Sprint object
     */
    public Sprint getSprintById(int id) {
        return sprints.findById(id);
    }

    /**
     * Get a sprint from the database by its Label
     * @param label - Label of the sprint
     * @return - Sprint object
     */
    public Sprint getSprintByLabel(String label, Project project) {
        ArrayList<Sprint> projectSprints = new ArrayList<Sprint>(sprints.findByProject(project));

        for (Sprint sprint : projectSprints) {
            if (sprint.getLabel().equals(label)) return sprint;
        }

        return null;
    }

    /**
     * Get all sprints with a given project
     * @param project - Project to get sprints for
     * @return List of Sprints with parent project
     */
    public List<Sprint> getSprintsByProject(Project project) {
        return sprints.findByProject(project);
    }

    /**
     * Check if a sprint exists in the datbase by its Id
     * @param id - Id of the sprint
     * @return True if the sprint exists
     */
    public boolean hasSprint(int id) {
        return sprints.existsById(id);
    }

    /**
     * Inserts or updates a sprint (by id) into the database. The sprint is first validated. If there are errors return null.
     * @param sprint - Sprint object to insert
     * @return - The sprint for further operations
     */
    public Sprint save(Sprint sprint) {
        if (sprint.getLabel() == null) sprint.setLabel("Sprint " + (countForProject(sprint.getParentProject()) + 1));
        if (sprint.getName() == null) sprint.setName(sprint.getLabel());

        clearErrors();
        SprintValidator.validateSprint(sprint);

        if (hasErrors()) return null;

        return sprints.save(sprint);
    }

    /**
     * Get total number of sprints in the database
     * @return - Total number of sprints in the database
     */
    public long count() {
        return sprints.count();
    }

    /**
     * Get total number of sprints in the database in a given project
     * @param project - Project that you want to count
     * @return Total number of sprints in the database in a given project
     */
    public long countForProject(Project project) {
        return sprints.countByProject(project);
    }

    /**
     * Delete a sprint from the database
     * @param sprint - Sprint to delete from the database
     * @return - True of false depending on success
     */
    public boolean delete(Sprint sprint) {
        try {
            sprints.delete(sprint);
        } catch (IllegalArgumentException e) {
            return false;
        }

        List<Sprint> allSprints = getSprintsByProject(sprint.getParentProject());

        //Updating sprint labels
        int index = 1;
        for (Sprint nextSprint : allSprints) {
            nextSprint.setLabel("Sprint " + index);
            save(nextSprint);
            index++;
        }

        return true;
    }

    /**
     * Get the sprints start date upper and lower bounds
     * @param sprint - Sprint to get the bounds for
     * @return - List\<Date\> [lowerDateBound, upperDateBound]
     */
    public List<Date> getSprintStartDateBounds(Sprint sprint) {
        if (sprint == null) {
            return null;
        }

        List<Date> result = new ArrayList<>();
        Project project = sprint.getParentProject();

        List<Sprint> sprintsInProject = getSprintsByProject(project);

        //Get the sprint before this one
        Sprint previousSprint = null;
        for (Sprint sprint1 : sprintsInProject) {
            if (sprint.getId() == sprint1.getId()) {
                break;
            }
            previousSprint = sprint1;
        }

        //We are editing the first sprint
        if (previousSprint == null) {
            result.add(project.getStartDate());
        } else {
            result.add(DateUtil.addDaysToDate(previousSprint.getEndDate(), 1));
        }

        //Get the sprint after the current
        Sprint nextSprint = null;
        int index = 0;
        for (Sprint sprint1 : sprintsInProject) {
            if (sprint.getId() == sprint1.getId()) {
                if (index == sprintsInProject.size() - 1) break;

                nextSprint = sprintsInProject.get(index + 1);
                break;
            }
            index++;
        }

        if (nextSprint == null)
        {
            result.add(project.getEndDate());
        } else {
            result.add(DateUtil.addDaysToDate(nextSprint.getStartDate(), -1));
        }

        // Remove one day from the end
        result.set(1, DateUtil.addDaysToDate(result.get(1), -1));

        return result;
    }

    /**
     * Get the sprints end date upper and lower bounds
     * @param sprint - Sprint to get the bounds for
     * @return - List\<Date\> [lowerDateBound, upperDateBound]
     */
    public List<Date> getSprintEndDateBounds(Sprint sprint) {
        if (sprint == null) {
            return null;
        }

        List<Date> result = new ArrayList<>();
        Project project = sprint.getParentProject();

        List<Sprint> sprintsInProject = getSprintsByProject(project);

        // Min
        result.add(DateUtil.addDaysToDate(sprint.getStartDate(), 1));

        //Get the sprint after the current
        Sprint nextSprint = null;
        int index = 0;
        for (Sprint sprint1 : sprintsInProject) {
            if (index == sprintsInProject.size() - 1) break;

            if (sprint.getId() == sprint1.getId()) {
                nextSprint = sprintsInProject.get(index + 1);
                break;
            }
            index++;
        }

        //We are editing the last sprint
        if (nextSprint == null) {
            result.add(project.getEndDate()); //Max
        } else {
            result.add(DateUtil.addDaysToDate(nextSprint.getStartDate(), -1)); //Max
        }

        return result;
    }

    public static boolean containsDate (Sprint sprint, Date date) {
        return !date.before(sprint.getStartDate()) && !date.after(sprint.getEndDate());
    }

    /**
     * Sprint authentication methods.
     * Handles UI errors.
     */
    public static void addNewError (UserError err) { errs.add(err);}

    public static ArrayList<UserError> getCurrentErrors () { return errs;}

    public static void clearErrors () {errs.clear();}

    public static Boolean hasErrors () {return errs.size() > 0;}

    public static List<String> getBackgroundColours() {
        ArrayList<String> colours = new ArrayList<>();
        colours.add("rgb(153, 0, 0)");
        colours.add("rgb(51, 102, 0)");
        colours.add("rgb(0, 51, 102)");
        colours.add("rgb(102, 0, 102)");
        colours.add("rgb(0, 102, 102)");
        colours.add("rgb(32, 32, 32)");
        return colours;
    }

    public static List<String> getOpaqueBackgroundColours() {
        ArrayList<String> colours = new ArrayList<>();
        colours.add("rgba(153, 0, 0, 0.1)");
        colours.add("rgba(51, 102, 0, 0.1)");
        colours.add("rgba(0, 51, 102, 0.1)");
        colours.add("rgba(102, 0, 102, 0.1)");
        colours.add("rgba(0, 102, 102, 0.1)");
        colours.add("rgba(32, 32, 32, 0.1)");
        return colours;
    }
}
