package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.repositories.SprintRepository;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class SprintServiceTest {
    @Mock
    private SprintRepository sprintRepository;

    @Spy
    @InjectMocks
    private SprintService sprintService;

    private Project project;

    @BeforeEach
    public void setup() {
        project = Mockito.mock(Project.class);

        Date projectStart = null;
        Date projectEnd = null;
        try {
            projectStart = new SimpleDateFormat("dd/MM/yyyy").parse("20/01/2022");
            projectEnd = new SimpleDateFormat("dd/MM/yyyy").parse("25/08/2022");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Mockito.lenient().when(project.getStartDate()).thenReturn(projectStart);
        Mockito.lenient().when(project.getEndDate()).thenReturn(projectEnd);
    }

    @Test
    public void test_get_sprint_by_id() {
        Mockito.when(sprintRepository.findById(0)).thenReturn(new Sprint(new Project(), "", "", "", new Date(), new Date()));
        Assertions.assertNotNull(sprintService.getSprintById(0));
    }


    @Test
    public void test_get_sprint_by_project() {
        Project prj = new Project();
        Mockito.when(sprintRepository.findByProject(prj)).thenReturn(Arrays.asList(new Sprint(prj, "", "test", "", new Date(), new Date()),
                new Sprint(prj, "", "test1", "", new Date(), new Date())));
        Assertions.assertEquals(2, sprintService.getSprintsByProject(prj).size());
    }

    @Test
    public void test_has_sprint() {
        Mockito.when(sprintRepository.existsById(0)).thenReturn(Boolean.TRUE);
        Assertions.assertTrue(sprintService.hasSprint(0));
    }

    @Test
    public void test_save_sprint() {
        Sprint spr = new Sprint(project);
        Mockito.when(sprintRepository.save(spr)).thenReturn(spr);
        Mockito.when(project.getSprints()).thenReturn(List.of(spr));
        Assertions.assertEquals(spr, sprintService.save(spr));
    }

    @Test
    public void test_sprint_count() {
        Mockito.when(sprintRepository.count()).thenReturn(Integer.toUnsignedLong(1));
        Assertions.assertEquals(1, sprintService.count());
    }

    @Test
    public void test_sprint_count_for_project() {
        Mockito.lenient().when(sprintRepository.countByProject(new Project())).thenReturn(Integer.toUnsignedLong(0));
        Assertions.assertEquals(0, sprintService.countForProject(new Project()));
    }

    @Test
    public void test_sprint_delete() {
        Sprint spr1 = new Sprint(new Project(), "", "test", "", new Date(), new Date());

        sprintService.delete(spr1);
        Mockito.verify(sprintRepository, Mockito.times(1)).delete(spr1);
    }

    @Test
    public void test_ui_error_handling() {
        SprintService.clearErrors();
        Assertions.assertTrue(SprintService.getCurrentErrors().isEmpty());
        SprintService.addNewError(new UserError("test", "testing errors"));
        Assertions.assertFalse(SprintService.getCurrentErrors().isEmpty());
        Assertions.assertTrue(SprintService.hasErrors());
        SprintService.clearErrors();
        Assertions.assertFalse(SprintService.hasErrors());
    }

    @Test
    public void test_getSprintStartDateBounds_noSprints() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);
        sprints.add(sprint);

        // Expected result
        List<Date> expected = Arrays.asList(
                project.getStartDate(),
                DateUtil.addDaysToDate(project.getEndDate(), -1)
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintStartDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }

    @Test
    public void test_getSprintStartDateBounds_sprintsBefore() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        // Add sprints
        sprints.add(new Sprint(project));
        Sprint sprint = Mockito.spy(new Sprint(project));
        Mockito.when(sprint.getId()).thenReturn(2); //Id's are not set, so need to specify
        sprints.add(sprint);

        // Expected result
        List<Date> expected = Arrays.asList(
                DateUtil.addDaysToDate(sprints.get(0).getEndDate(), 1),
                DateUtil.addDaysToDate(project.getEndDate(), -1)
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintStartDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }

    @Test
    public void test_getSprintStartDateBounds_sprintsAfter() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        // Add sprints
        Sprint sprint = Mockito.spy(new Sprint(project));
        Mockito.when(sprint.getId()).thenReturn(2); //Id's are not set, so need to specify
        sprints.add(sprint);

        Sprint sprint2 = new Sprint(project);
        sprints.add(sprint2);
        sprints.add(new Sprint(project));

        // Expected result
        List<Date> expected = Arrays.asList(
                project.getStartDate(),
                DateUtil.addDaysToDate(sprint2.getStartDate(), -2)
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintStartDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }

    @Test
    public void test_getSprintEndDateBounds_noSprints() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);
        sprints.add(sprint);

        // Expected result
        List<Date> expected = Arrays.asList(
                DateUtil.addDaysToDate(project.getStartDate(), 1),
                project.getEndDate()
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintEndDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }

    @Test
    public void test_getSprintEndDateBounds_sprintsBefore() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        // Add sprints
        sprints.add(new Sprint(project));
        Sprint sprint = Mockito.spy(new Sprint(project));
        Mockito.when(sprint.getId()).thenReturn(2); //Id's are not set, so need to specify
        sprints.add(sprint);

        // Expected result
        List<Date> expected = Arrays.asList(
                DateUtil.addDaysToDate(sprints.get(0).getEndDate(), 2),
                project.getEndDate()
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintEndDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }

    @Test
    public void test_getSprintEndDateBounds_sprintsAfter() {
        // Setup
        List<Sprint> sprints = new ArrayList<>();

        project = Mockito.spy(Project.class);
        project.setStartDate(DateUtil.stripTimeFromDate(new Date()));
        project.setEndDate(DateUtil.addDaysToDate(DateUtil.stripTimeFromDate(new Date()), 90));

        Mockito.when(project.getSprints()).thenReturn(sprints);

        // Add sprints
        Sprint sprint = Mockito.spy(new Sprint(project));
        Mockito.when(sprint.getId()).thenReturn(2); //Id's are not set, so need to specify
        sprints.add(sprint);

        Sprint sprint2 = new Sprint(project);
        sprints.add(sprint2);
        sprints.add(new Sprint(project));

        // Expected result
        List<Date> expected = Arrays.asList(
                DateUtil.addDaysToDate(project.getStartDate(), 1),
                DateUtil.addDaysToDate(sprint2.getStartDate(), -1)
        );

        // Mock
        Mockito.when(sprintRepository.findByProject(project)).thenReturn(sprints);

        // Run
        List<Date> bounds = sprintService.getSprintEndDateBounds(sprint);

        // Assert
        Assertions.assertEquals(expected, bounds);
    }
}
