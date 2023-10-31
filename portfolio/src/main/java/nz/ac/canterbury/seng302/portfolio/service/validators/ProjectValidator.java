package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.SprintRepository;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

/**
 * Authenticator class for the data in Project. This class will take in a Project object and check for any errors in
 * the data. Any errors found here will be sent to the ProjectControllerAuth class to be handled and shown to the user
 */
@Service
public class ProjectValidator {
    @Autowired
    private ProjectRepository projects;

    @Autowired
    private SprintRepository sprints;

    /**
     * Checks that the dates for the project are within one year of the current date, and that the start date for the
     * project is before the end date.
     * Any errors in the dates will be sent to ProjectService to be shown to the user
     * @param project project to check the dates for
     */
    public static void validateDates (Project project) {
        if (project.getStartDate() == null) {
            ProjectService.addNewError(new UserError("ProjectStartDate", "Project start date is a required field."));
            return;
        }

        if (project.getEndDate() == null) {
            ProjectService.addNewError(new UserError("ProjectEndDate", "Project end date is a required field."));
            return;
        }

        //Ensures startDate is not more than one year prior than current date and compares at time 00:00 for both
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date datePrevYear = new Date (cal.getTimeInMillis());

        cal.setTime(project.getStartDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        project.setStartDate(new java.util.Date(cal.getTimeInMillis()));

        if (project.getStartDate().before(datePrevYear)) {
            //Sets startDate to the minimum required startDate (one year prior)
            //this.startDate = datePrevYear;
            ProjectService.addNewError(new UserError("ProjectStartDate", "Can't set project start date to: "
                    + DateUtil.dateToFormattedString(project.getStartDate())+" as it is more than a year ago."));
        }

        if (project.getStartDate().after(project.getEndDate())) {
            //this.endDate = this.startDate;
            ProjectService.addNewError(new UserError("ProjectStartDate", "Project Start Date: "+
                    DateUtil.dateToFormattedString(project.getStartDate())+" must be before Project End Date: " +
                    DateUtil.dateToFormattedString(project.getEndDate())+ ".")) ;
        }

        List<Sprint> sprints = project.getSprints();

        //If there are sprints, then ensure project dates are outside the dates of the sprints
        if (sprints != null && sprints.size() > 0) {
            boolean afterSprintStart = project.getStartDate().after(sprints.get(0).getStartDate());
            boolean beforeSprintEnd = project.getEndDate().before(sprints.get(sprints.size() - 1).getEndDate());
            if (afterSprintStart) {
                ProjectService.addNewError(new UserError("ProjectStartDate", "Project Start date: "
                        +DateUtil.dateToFormattedString(project.getStartDate())+" cannot occur after Sprint 0 Start date: "
                        + DateUtil.dateToFormattedString(sprints.get(0).getStartDate())+ "."));
            }
            if (beforeSprintEnd) {
                Sprint lastSprint = sprints.get(sprints.size() - 1);
                ProjectService.addNewError(new UserError("ProjectEndDate", "Project End date: "
                        +DateUtil.dateToFormattedString(project.getEndDate())+" cannot occur before "+lastSprint.getLabel()
                        +" End date: " + DateUtil.dateToFormattedString(lastSprint.getEndDate())+ "."));
            }
        }
    }

    /**
     * Checks the dates of the sprints to check if the dates for the project don't overlap with the sprint dates,
     * in other words ensures that the sprints all occur within the dates of the project
     * Any errors in the dates will be sent to ProjectService to be shown to the user
     * @param project project to check the dates and sprints for
     */
    public static void checkSprintDates (Project project) {
        List<Sprint> sprints = project.getSprints();
        if (sprints != null && sprints.size() > 0) {
            //ensures the first sprint starts after the project start date
            if (sprints.get(0).getStartDate().before(project.getStartDate())) {
                Sprint spr = sprints.get(0);
                ProjectService.addNewError(new UserError("ProjectStartDate", "Project start date: "
                        + DateUtil.dateToFormattedString(project.getStartDate()) + " is overlapping with " + spr.getLabel()
                        + " dates, as this sprints start date is " + DateUtil.dateToFormattedString(spr.getStartDate())+ "."));
            }
            //ensures the last sprint ends before the end of the project
            if (sprints.get(sprints.size() - 1).getEndDate().after(project.getEndDate())) {
                Sprint spr = sprints.get(sprints.size() - 1);
                ProjectService.addNewError(new UserError("ProjectEndDate", "Project end date: "
                        + DateUtil.dateToFormattedString(project.getEndDate()) + " is overlapping with " + spr.getLabel()
                        + " dates, as this sprints end date is " + DateUtil.dateToFormattedString(spr.getEndDate())+ "."));
            }
        }
    }

    /**
     * Checks that the name of the project is not null.
     * Any errors in the project name will be sent to ProjectService to be shown to the user
     * @param project project to check the name for
     */
    public static void validateName (Project project) {
        if (project.getName().trim().isEmpty()) {
            ProjectService.addNewError(new UserError("ProjectName", "Project Name is a required field."));
        }
    }
}