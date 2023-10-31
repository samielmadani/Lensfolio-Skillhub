package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SprintValidator {

    public static void validateSprint (Sprint currentSprint) {
        validateDates(currentSprint);
        validateName(currentSprint);
    }

    public static void validateName (Sprint currentSprint) {
        if (currentSprint.getName().replaceAll("\\s", "").equals("")) {
            SprintService.addNewError(new UserError("SprintName", "Sprint name cannot be null."));
        }
    }

    /**
     * Ensures that the dates for the sprint are valid (inside the project timeframe and not overlapping with
     * any other sprints
     */
    public static void validateDates (Sprint currentSprint) {
        Project prj = currentSprint.getParentProject();
        List<Sprint> allSprints = prj.getSprints();

        if (currentSprint.getStartDate() == null) {
            ProjectService.addNewError(new UserError("SprintStartDate", "Sprint start date is a required field."));
            return;
        }
        if (currentSprint.getEndDate() == null) {
            ProjectService.addNewError(new UserError("SprintEndDate", "Sprint end date is a required field."));
            return;
        }

        if (currentSprint.getStartDate().after(currentSprint.getEndDate())) {
            SprintService.addNewError(new UserError("SprintStartDate", "Sprint start date: "+
                    DateUtil.dateToFormattedString(currentSprint.getStartDate())+" must occur before Sprint end date: "
                    +DateUtil.dateToFormattedString(currentSprint.getEndDate())+ "."));
        }

        //Makes sure the sprint startDate is within the project timeframe
        if (currentSprint.getStartDate().getTime() < prj.getStartDate().getTime()) {
            //this.startDate = prj.getStartDate();
            SprintService.addNewError(new UserError("SprintStartDate", "Sprint start date: "
                    +DateUtil.dateToFormattedString(currentSprint.getStartDate())+" must be after Project start date: "
                    + DateUtil.dateToFormattedString(prj.getStartDate())+ "."));
        }
        //Makes sure the sprint endDate is within the project timeframe
        if (currentSprint.getEndDate().getTime() > prj.getEndDate().getTime()) {
            //this.endDate = prj.getEndDate();
            SprintService.addNewError(new UserError("SprintEndDate", "Sprint end date: "
                    +DateUtil.dateToFormattedString(currentSprint.getEndDate())+" must be before Project end date: "
                    + DateUtil.dateToFormattedString(prj.getEndDate())+ "."));
        }

        //Ensures no overlapping sprints
        for (Sprint sprint : allSprints) {
            if (Objects.equals(sprint.getLabel(), currentSprint.getLabel())) {
                continue;
            }

            if (currentSprint.getStartDate().getTime() >= sprint.getStartDate().getTime() && currentSprint.getStartDate().getTime() <= sprint.getEndDate().getTime()) {
                SprintService.addNewError(new UserError("SprintStartDate", "Sprint start date: "
                        +DateUtil.dateToFormattedString(currentSprint.getStartDate())+" is overlapping with the dates in "
                        + sprint.getLabel()+ "."));
            }
            if (currentSprint.getEndDate().getTime() >= sprint.getStartDate().getTime() && currentSprint.getEndDate().getTime() <= sprint.getEndDate().getTime()) {
                SprintService.addNewError(new UserError("SprintEndDate", "Sprint end date: "
                        +DateUtil.dateToFormattedString(currentSprint.getEndDate())+" is overlapping with the dates in "
                        + sprint.getLabel()+ "."));
            }
        }
    }
}
