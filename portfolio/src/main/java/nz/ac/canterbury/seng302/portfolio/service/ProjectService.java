package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.RangeDTO;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectGroupRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectRepository;
import nz.ac.canterbury.seng302.portfolio.service.validators.ProjectValidator;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ProjectService {
    private final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private ProjectGroupRepository projectGroups;

    @Autowired
    private GroupClientGRPCService groups;

    /* Collection for errors to be represented to the user */
    private static final ArrayList<UserError> errs = new ArrayList<>();

    /**
     * Get a project from the database by its Id
     * @param id - Id of the project
     * @return - Project object
     */
    public Project getProjectById(int id) {
        return projects.findById(id);
    }

    public ArrayList<Project> getAllProjects(){
        return (ArrayList<Project>) projects.findAll();
    }

    /**
     * Check if a project exists in the datbase by its Id
     * @param id - Id of the project
     * @return True if the project exists
     */
    public boolean hasProject(int id) {
        return projects.existsById(id);
    }

    /**
     * Insert or update a project (by id) into the database. The project is first validated. If there are errors return null.
     * @param project - Project object to insert/update
     * @return - The project for further operations
     */
    public Project save(Project project) {
        clearErrors();
        ProjectValidator.validateName(project);
        ProjectValidator.validateDates(project);

        if (hasErrors()) return null;

        return projects.save(project);
    }

    /**
     * Generate a new default project, if the project does not exist. There should only ever be one default project.
     */
    public Project generateDefaultProject() {
        List<Project> defaultProjects = projects.findByIsDefaultProject(true);

        // Because there should only ever be one default project
        if (defaultProjects.isEmpty()) {
            logger.info("Creating new default project");
            Project defaultProject = new Project();
            defaultProject.setAsDefaultProject();
            return projects.save(defaultProject);
        }

        return defaultProjects.get(0);
    }

    /**
     * Get upper and lower bounds for a project start date
     * @param project - Project to find bounds for
     * @return List\<Date\> [lowerLimit, upperLimit]
     */
    public List<Date> getProjectStartDateLimits (Project project) {
        List<Date> result = new ArrayList<>();

        // Lower bound (one year ago, or the currently set lower bound)
        Calendar prevYear = Calendar.getInstance();
        prevYear.add(Calendar.YEAR, -1);
        Date datePrevYear = DateUtil.stripTimeFromDate(prevYear.getTime());

        Date currentLowest = project.getStartDate();
        Date lowerBound = datePrevYear.before(currentLowest)? datePrevYear : currentLowest;

        // Upper bound (5 years from now, or sprint start date)
        Calendar future = Calendar.getInstance();
        future.add(Calendar.YEAR, 5);
        Date upperBound = DateUtil.stripTimeFromDate(future.getTime());

        List<Sprint> sprints = project.getSprints();
        if (sprints != null && sprints.size() > 0) {
            upperBound = sprints.get(0).getStartDate();
        }

        result.add(lowerBound);
        result.add(upperBound);
        return result;
    }

    /**
     * Get upper and lower bounds for a project end date
     * @param project - Project to find bounds for
     * @return List\<Date\> [lowerLimit, upperLimit]
     */
    public List<Date> getProjectEndDateLimits(Project project) {
        List<Date> result = new ArrayList<>();

        // Lower bound (1 day after the project start day, or last sprint end date)
        Date lowerBound = DateUtil.addDaysToDate(project.getStartDate(), 1);

        List<Sprint> sprints = project.getSprints();
        if (sprints != null && sprints.size() > 0) {
            lowerBound = sprints.get(sprints.size() - 1).getEndDate();
        }

        //Lower bound has to be at least today
        Date today = DateUtil.stripTimeFromDate(new Date());
        lowerBound = lowerBound.before(today)? today : lowerBound;

        // Upper bound (5 years from start date or day after lower bound)
        Calendar upperBoundCal = Calendar.getInstance();
        upperBoundCal.setTime(project.getStartDate());
        upperBoundCal.add(Calendar.YEAR, 5);
        upperBoundCal.add(Calendar.DATE, 1);

        Date upperBound = DateUtil.stripTimeFromDate(upperBoundCal.getTime());

        upperBound = upperBound.after(lowerBound)? upperBound : DateUtil.addDaysToDate(lowerBound, 1);

        result.add(lowerBound);
        result.add(upperBound);

        return result;
    }

    /**
     * Gets all the date ranges in terms of sprints for a certain project. This function will return all non-zero ranges
     * in the project range, including all sprints as a single range each, and the amount of time between the sprints.
     * The returned list is in order of occurrence
     * @param project Project to get ranges for
     * @param sprints All the sprints in the project
     * @return List of all the ranges in the project
     */
    public List<RangeDTO> getProjectRanges (Project project, List<Sprint> sprints) {
        logger.info("Creating project ranges for project " + project.getId());
        ArrayList<RangeDTO> ranges = new ArrayList<>();
        //First we need to add the total project range
        ranges.add(new RangeDTO(project.getStartDate(), project.getEndDate(), "ProjectStart"));

        for (Sprint sprint : sprints) {
            //For each sprint, subdivide the previous range to include the current sprint in the range, and the time between the end of the sprint and the end of the project
            ranges.set(ranges.size() - 1, new RangeDTO(ranges.get(ranges.size() - 1).getStart(), sprint.getStartDate(), ranges.get(ranges.size() - 1).getLocation()));
            ranges.add(new RangeDTO(sprint.getStartDate(), sprint.getEndDate(), "InnerSprint" + sprint.getId()));
            ranges.add(new RangeDTO(sprint.getEndDate(), project.getEndDate(), "OuterSprint" + sprint.getId()));

        }
        //Filter out ranges that have a 0-day length, e.g. the start and end dates are the same :)
        ranges = new ArrayList<>(ranges.stream().filter(range -> !range.getStart().equals(range.getEnd())).toList());

        if (ranges.size() >= 5) {
            RangeDTO nextRange = ranges.get(2);
            int counter = 1;

            while (counter < ranges.size() - 2) {
                RangeDTO range = ranges.get(counter);

                long time = nextRange.getEnd().getTime() - range.getEnd().getTime();
                if (time == 86400000) {
                    ranges.remove(ranges.indexOf(range) + 1);
                    nextRange = ranges.get(ranges.indexOf(range) + 2);

                    counter += 1;
                } else {
                    nextRange = ranges.get(ranges.indexOf(range) + 2);
                    counter += 2;
                }
            }
        }

        if (ranges.size() >= 2) {
            if (ranges.get(0).getStart().getTime() == ranges.get(1).getStart().getTime()) {
                ranges.remove(0);
            }
        }

        return ranges;
    }

    /**
     * Get groups for the projects that linked
     * @param projectId project that are included in the group
     * @return details of the group
     */
    public List<GroupDetailsResponse> getGroupsForProject(int projectId) {
        List<ProjectGroup> li = projectGroups.findProjectGroupsByProjectId(projectId);
        List<GroupDetailsResponse> groupDetails = new ArrayList<>();
        for (ProjectGroup p : li) {
            groupDetails.add(groups.getGroup(p.getGroupId()));
        }

        return groupDetails;
    }

    /**
     * Link project and group
     * @param projectId ID of the project to be linked
     * @param groupId ID of the group that linked with the project
     * @return null
     */
    public ProjectGroup linkProjectAndGroup(int projectId, int groupId) {
        List<Project> allProjects = getProjectsForGroup(groupId);
        if (!(allProjects.contains(projects.findById(projectId)))) {
            return projectGroups.save(new ProjectGroup(projectId, groupId));
        }

        return null;
    }

    /**
     * Unlink project and group
     * @param projectId ID of the project to be unlinked
     * @param groupId ID of the group that unlinked with the project
     */
    public void unlinkProjectAndGroup(int projectId, int groupId) {
        projectGroups.delete(new ProjectGroup(projectId, groupId));
    }

    /**
     * Get list of projects for group to link
     * @param groupId ID of the group
     * @return project to linked to group
     */
    public List<Project> getProjectsForGroup(int groupId) {
        List<ProjectGroup> li = projectGroups.findProjectGroupsByGroupId(groupId);
        List<Project> projectsLinkedToGroup = new ArrayList<>();
        for (ProjectGroup p : li) {
            projectsLinkedToGroup.add(projects.findById(p.getProjectId()));
        }

        return projectsLinkedToGroup;
    }

    /**
     * Get the projects that are not in the group
     * @param groupId ID of the group
     * @return the projects that are not in group
     */
    public ArrayList<Project> getProjectsNotInGroup (int groupId) {
        ArrayList<Project> projectsNotInGroup = getAllProjects();
        for (Project prj : getAllProjects()) {
            List<GroupDetailsResponse> groups = getGroupsForProject(prj.getId());
            for (GroupDetailsResponse r : groups) {
                if (r.getGroupId() == groupId) {
                    projectsNotInGroup.remove(prj);
                }
            }
        }
        return projectsNotInGroup;
    }

    public List<String> getJSFriendlyDates (String endDateString, List<Date> bounds) {
        Date endDate = null;
        if (endDateString.length() > 0) {
            endDate = DateUtil.stringToISODate(endDateString);
            endDateString = endDateString.replaceAll("[\n\r\t]", "_");
            if (endDate == null) logger.error("Failed to format date string: {}", endDateString);

            endDate = DateUtil.stripTimeFromDate(endDate);
            endDate = DateUtil.addDaysToDate(endDate, -1); // Must be at least the day before
        }

        // Set the max start date to be before the selected end date
        if (endDate != null && endDate.before(bounds.get(1))) {
            bounds.set(1, endDate);
        }

        // Format to javascript array (yyyy-mm-dd)
        List<String> javaScriptFriendly = new ArrayList<>();
        javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(0)));
        javaScriptFriendly.add(DateUtil.dateToISOString(bounds.get(1)));

        logger.debug("[getStartDateBounds] - {}", javaScriptFriendly);

        return javaScriptFriendly;
    }

    /**
     * Project authentication methods.
     * Handles UI errors.
     */
    public static void addNewError (UserError err) { errs.add(err);}

    public static ArrayList<UserError> getCurrentErrors () { return errs;}

    public static void clearErrors () {errs.clear();}

    public static Boolean hasErrors () {return errs.size() > 0;}
}
