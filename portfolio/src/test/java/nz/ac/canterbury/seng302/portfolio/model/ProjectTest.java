package nz.ac.canterbury.seng302.portfolio.model;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectTest {

    @Test
    public void test_B_createDefaultProject () {
        Project prj = new Project();
        Assertions.assertEquals("Project 2022", prj.getName());
        Assertions.assertEquals("Project 2022", prj.getDescription());
        Assertions.assertTrue(prj.getStartDate().before(prj.getEndDate()));
    }

    @Test
    public void test_project_constructor_string_date_formats () {
        Project newProject = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        Assertions.assertEquals("Project 1", newProject.getName());
        Assertions.assertEquals("A project", newProject.getDescription());
        Assertions.assertEquals("2022-01-22", newProject.getStartDateIsoString());
        Assertions.assertEquals("2022-07-29", newProject.getEndDateIsoString());
    }

    @Test
    public void test_project_constructor_date_object_dates () {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("dd/MM/yyyy").parse("22/01/2022");
            endDate = new SimpleDateFormat("dd/MM/yyyy").parse("29/07/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Project newProject = new Project("Project 1", "A project", startDate, endDate);
        Assertions.assertEquals("Project 1", newProject.getName());
        Assertions.assertEquals("A project", newProject.getDescription());
        Assertions.assertEquals("2022-01-22", newProject.getStartDateIsoString());
        Assertions.assertEquals("2022-07-29", newProject.getEndDateIsoString());
    }

    @Test
    public void test_project_to_string_format_and_contents () {
        Project prj = new Project("Project 1", "A project", "22/Jan/2022", "29/Jul/2022");
        String expected = "Project[id=0, projectName='Project 1', projectStartDate='Sat Jan 22 00:00:00 NZDT 2022', projectEndDate='Fri Jul 29 00:00:00 NZST 2022', projectDescription='A project', isDefaultProject='null']";
        Assertions.assertEquals(expected, prj.toString());
    }

}
