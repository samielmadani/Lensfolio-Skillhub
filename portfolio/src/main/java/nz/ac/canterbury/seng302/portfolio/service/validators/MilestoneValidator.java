package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.service.MilestoneService;
import org.springframework.stereotype.Service;

@Service
public class MilestoneValidator {
    /**
     * Checks that an milestone has the correct dates and name values.
     * @param currentMilestone Milestone to check for errors
     */
    public static void validateMilestone (Milestone currentMilestone) {
        if (currentMilestone.getName().replaceAll("\\s", "").equals("")) {
            MilestoneService.addNewError(new UserError("MilestoneName", "Milestone name cannot be null!"));
        }
    }
}
