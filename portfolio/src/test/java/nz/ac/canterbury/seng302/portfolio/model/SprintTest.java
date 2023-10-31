package nz.ac.canterbury.seng302.portfolio.model;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;

import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.*;

public class SprintTest {
    Project prj;
    List<Sprint> sprints = new ArrayList<>();
    Project newProject;
    Sprint sprt1;
    Sprint sprt2;

    @BeforeEach
    public void setup () {
        newProject = new Project("", "", "20/Jan/2022", "25/Aug/2022");
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
        Mockito.when(prj.getSprints()).thenReturn(sprints);

        sprt1 = new Sprint(newProject, "Sprint 1", "Sprint A", "", startDate, endDate);
        sprt2 = new Sprint(newProject, "Sprint 2", "Sprint B", "A Sprint", startDate1, endDate1);

        sprints.add(sprt1);
    }

    @Test
    public void test_A_createSprintsTest () {
        // Test correct project ID
        Assertions.assertEquals(0, newProject.getId());

        // Test Sprints added to correct project
        Assertions.assertEquals(newProject.getId(), sprt1.getParentProject().getId());
        Assertions.assertEquals(newProject.getId(), sprt2.getParentProject().getId());

        // Test Sprint Details are correctly constructed
        Assertions.assertEquals("Sprint A", sprt1.getLabel());
        Assertions.assertEquals("A Sprint", sprt2.getDescription());
        Assertions.assertTrue(sprt2.getEndDate().before(newProject.getEndDate()));
        Assertions.assertTrue(sprt2.getStartDate().after(sprt1.getEndDate()));
        Assertions.assertTrue(sprt1.getStartDate().after(newProject.getStartDate()));
    }

    @Test
    public void test_B_testSprintDefaultConstructor () {
        Sprint testSprint = new Sprint(prj);
        Assertions.assertTrue(testSprint.getStartDate().after(prj.getStartDate()));
        Assertions.assertTrue(testSprint.getEndDate().before(prj.getEndDate()));
        Assertions.assertEquals(prj, testSprint.getParentProject());
        Assertions.assertEquals("", testSprint.getDescription());
    }

    @Test
    public void  test_sprint_to_string () {
        String expected = "Sprint[id=0, parentProjectId='0', sprintName='Sprint 1', sprintLabel='Sprint A', sprintStartDate='Sat Jan 22 00:00:00 NZDT 2022', sprintEndDate='Sat Jan 29 00:00:00 NZDT 2022', sprintDescription='']";
        Assertions.assertEquals(expected, sprt1.toString());
    }
}
