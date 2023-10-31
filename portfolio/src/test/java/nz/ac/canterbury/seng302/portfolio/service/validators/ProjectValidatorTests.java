package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.validators.ProjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ProjectValidatorTests {
    private Project prj;
    List<Sprint> sprints;

    @BeforeEach
    public void resetControllerAuthenticator () {
        ProjectService.clearErrors();
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
    public void testValidateDates_ExpectedDate () {
        Project newProject = new Project("New Project", "Project test!", "20/Feb/2022", "20/Jul/2022");
        ProjectValidator.validateDates (newProject);
        assertFalse(ProjectService.hasErrors());
    }

    @Test
    public void testValidateDates_DateTooEarly () {
        Project newProject = new Project("New Project", "Project test!", "20/Feb/2020", "20/Jul/2020");
        ProjectValidator.validateDates (newProject);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testValidateDates_OutOfOrderDates () {
        Project newProject = new Project("New Project", "Project test!", "20/Feb/2023", "20/Jul/2022");
        ProjectValidator.validateDates (newProject);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testValidateDates_OutOfOrderDates_andDateTooEarly () {
        Project newProject = new Project("New Project", "Project test!", "20/Feb/2020", "20/Jul/2019");
        ProjectValidator.validateDates (newProject);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testValidateName_notNull (){
        Project newProject = new Project("New Project", "Project test!", "20/Feb/2022", "20/Jul/2022");
        ProjectValidator.validateName (newProject);
        assertFalse(ProjectService.hasErrors());
    }

    @Test
    public void testValidateName_Null (){
        Project newProject = new Project("", "Project test!", "20/Feb/2022", "20/Jul/2022");
        ProjectValidator.validateName(newProject);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testValidateName_whiteSpaceAndTabNull (){
        Project newProject = new Project("   ", "Project test!", "20/Feb/2022", "20/Jul/2022");
        ProjectValidator.validateName (newProject);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testCheckSprintDates_withSprintsInProject () {
        ProjectValidator.checkSprintDates(prj);
        assertFalse(ProjectService.hasErrors());
    }

    @Test
    public void testCheckSprintDates_withSprintBeforeProjectStart () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("19/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("29/01/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint newSprint = new Sprint(prj, "", "", "", startDate, endDate);
        sprints.clear();
        sprints.add(newSprint);
        ProjectValidator.checkSprintDates(prj);
        assertTrue(ProjectService.hasErrors());
    }

    @Test
    public void testCheckSprintDates_withSprintAfterProjectEnd () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("29/09/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Sprint newSprint = new Sprint(prj, "", "", "", startDate, endDate);
        sprints.clear();
        sprints.add(newSprint);
        ProjectValidator.checkSprintDates(prj);
        assertTrue(ProjectService.hasErrors());
    }
}
