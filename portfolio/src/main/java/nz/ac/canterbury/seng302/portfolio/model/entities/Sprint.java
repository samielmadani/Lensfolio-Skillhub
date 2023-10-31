package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Entity // this is an entity, assumed to be in a table called Sprint
public class Sprint {
    @Id
    @Column(name = "sprint_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int sprintId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String label;

    private String name;
    private String description;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date endDate;

    public Sprint(Project parentProject) {
        description = "";
        project = parentProject;
        SprintService.clearErrors();
        startDate = getDefaultStartDate();
        endDate = getDefaultEndDate();
    }

    public Sprint(Project parentProject, String sprintName, String sprintLabel, String sprintDescription, Date sprintStartDate, Date sprintEndDate) {
        this.project = parentProject;
        this.name = sprintName;
        this.label = sprintLabel;
        this.description = sprintDescription;
        this.startDate = sprintStartDate;
        this.endDate = sprintEndDate;
    }

    protected Sprint() {}

    @Override
    public String toString() {
        return String.format(
                "Sprint[id=%d, parentProjectId='%d', sprintName='%s', sprintLabel='%s', sprintStartDate='%s', sprintEndDate='%s', sprintDescription='%s']",
                sprintId, project.getId(), name, label, startDate, endDate, description);
    }

    /**
     * Returns the first available start date for a sprint in a project.
     * If no sprints exist then the start date is the first day of the project, otherwise it is one day after the end of
     * the last sprint in the project.
     * @return the date that the default start date should be
     */
    private Date getDefaultStartDate () {
        List<Sprint> sprintsInProject = project.getSprints();

        //If this is the first sprint in the project
        if (sprintsInProject.isEmpty()) {
            //startDate should be set to the day that the project began
            return project.getStartDate();
        }

        //Loop over all sprints in the project and get the latest end date from all sprints
        Date latestDate = project.getStartDate();
        for (Sprint sprint : sprintsInProject) {
            if (sprint.getEndDate().after(latestDate)) {
                latestDate = sprint.getEndDate();
            }
        }

        //Returns the latest date found, but adds two days.
        //New start date will be one day after the previous sprint ends
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(latestDate.getTime());
        cal.add(Calendar.DATE, 1);
        Date defStartDate = DateUtil.stripTimeFromDate(cal.getTime());

        if (!defStartDate.before(project.getEndDate())) {
            SprintService.addNewError(new UserError("SprintCreation", "Not enough time in the project to add another Sprint!"));
            return null;
        } else {
            return defStartDate;
        }
    }

    /**
     * Returns the date 3 weeks from the startDate, except if the new endDate is after the end of the project, then
     * the new endDate will be at the endDate of the project.
     * @return the default end date for a sprint
     */
    private Date getDefaultEndDate () {
        //If the startDate is undefined then so is the end date
        if (startDate == null) {
            return null;
        }

        //Get startDate and add 3 weeks to it
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate.getTime());
        cal.add(Calendar.WEEK_OF_YEAR, 3);
        Date date = DateUtil.stripTimeFromDate(cal.getTime());

        if (date.after(project.getEndDate())) {
            date = project.getEndDate();
        }

        //If the default end date is before or on the startDate
        if (!date.after(startDate)) {
            SprintService.addNewError(new UserError("SprintDates", "Sprint Start Date can't overlap!"));
            return null;
        } else {
            return date;
        }
    }

    // Getters + Setters

    public int getId() {
        return sprintId;
    }

    public Project getParentProject() {
        return project;
    }

    public void changeParentProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String sprintName) {
        this.name = sprintName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) { this.label = label; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String sprintDescription) {
        this.description = sprintDescription;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getStartDateString() {
        return DateUtil.dateToMonthString(this.startDate);
    }

    public String getStartDateIsoString() {
        return DateUtil.dateToISOString(this.startDate);
    }

    public void setStartDate(Date sprintStartDate) {
        this.startDate = sprintStartDate;
    }

    public String getFormattedStartDate () {return DateUtil.dateToFormattedString(startDate);}

    public String getFormattedEndDate () {return DateUtil.dateToFormattedString(endDate);}

    public Date getEndDate() {
        return endDate;
    }

    public String getEndDateString() {
        return DateUtil.dateToMonthString(this.endDate);
    }

    public String getEndDateIsoString() {
        return DateUtil.dateToISOString(this.endDate);
    }

    public void setEndDate(Date sprintEndDate) {
        this.endDate = sprintEndDate;
    }
}
