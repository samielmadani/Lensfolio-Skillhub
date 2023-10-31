package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.service.validators.SprintValidator;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SprintValidatorTests {
    private List<Sprint> sprints;
    private Project prj;

    @BeforeEach
    public void resetControllerAuthenticator () {
        SprintService.clearErrors();
        prj = Mockito.mock(Project.class);

        Date projectStart = null;
        Date projectEnd = null;
        Date startDate1 = null;
        Date endDate1 = null;
        Date startDate = null;
        Date endDate = null;
        try {
            projectStart = new SimpleDateFormat("dd/MM/yyyy").parse("20/01/2022");
            projectEnd = new SimpleDateFormat("dd/MM/yyyy").parse("25/08/2022");
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("22/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("29/01/2022");
            startDate1 = new SimpleDateFormat("dd/MM/yyyy").parse("30/03/2022");
            endDate1 = new SimpleDateFormat("dd/MM/yyyy").parse("22/04/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Mockito.when(prj.getStartDate()).thenReturn(projectStart);
        Mockito.when(prj.getEndDate()).thenReturn(projectEnd);

        sprints = new ArrayList<>();
        Sprint sprint1 = new Sprint(prj, "", "", "", startDate, endDate);
        Sprint sprint2 = new Sprint(prj, "Sprint 2", "Sprint B", "A Sprint", startDate1, endDate1);

        sprints.add(sprint1);
        sprints.add(sprint2);

        Mockito.when(prj.getSprints()).thenReturn(sprints);
    }

    @Test
    public void test_validate_dates_with_valid_date_sprint () {
        Sprint sprint = new Sprint(prj);
        sprint.setName("My Sprint");
        SprintValidator.validateSprint(sprint);
        assertFalse(SprintService.hasErrors());
    }

    @Test
    public void test_validate_dates_with_valid_manually_set_dates () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("25/04/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("30/05/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "My Sprint", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertFalse(SprintService.hasErrors());
    }

    @Test
    public void test_validate_dates_with_overlapping_sprint_start_date () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("25/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("30/01/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertTrue(SprintService.hasErrors());
        assertEquals(new UserError("SprintStartDate", "Sprint start date: "+ DateUtil.dateToFormattedString(startDate) + " is overlapping with the dates in ."), SprintService.getCurrentErrors().get(0));
    }

    @Test
    public void test_validate_dates_with_overlapping_sprint_end_date () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("15/02/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("04/04/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertTrue(SprintService.hasErrors());
        assertEquals(new UserError("SprintEndDate", "Sprint end date: "+DateUtil.dateToFormattedString(endDate)+" is overlapping with the dates in Sprint B."), SprintService.getCurrentErrors().get(0));
    }

    @Test
    public void test_validate_dates_with_sprint_start_date_after_end_date () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("23/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("22/01/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertTrue(SprintService.hasErrors());
        assertEquals(new UserError("SprintStartDate", "Sprint start date: "+DateUtil.dateToFormattedString(startDate)+" must occur before Sprint end date: " + DateUtil.dateToFormattedString(endDate) + "."), SprintService.getCurrentErrors().get(0));
    }

    @Test
    public void test_validate_dates_with_sprint_start_date_before_project_start_date () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("15/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("22/01/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertTrue(SprintService.hasErrors());
        assertEquals(new UserError("SprintStartDate", "Sprint start date: "+DateUtil.dateToFormattedString(startDate)+" must be after Project start date: " + DateUtil.dateToFormattedString(prj.getStartDate())+ "."), SprintService.getCurrentErrors().get(0));
    }

    @Test
    public void test_validate_dates_with_sprint_end_date_after_project_end_date () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("15/07/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("26/08/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint sprint = new Sprint(prj, "", "test", "", startDate, endDate);
        SprintValidator.validateSprint(sprint);
        assertTrue(SprintService.hasErrors());
        assertEquals(new UserError("SprintEndDate", "Sprint end date: "+DateUtil.dateToFormattedString(endDate)+" must be before Project end date: " + DateUtil.dateToFormattedString(prj.getEndDate())+ "."), SprintService.getCurrentErrors().get(0));
    }
}
