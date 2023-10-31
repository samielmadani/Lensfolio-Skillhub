package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectGroupRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectRepository;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.PaginatedGroupsResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectGroupRepository projectGroupRepository;

    @Mock
    private GroupClientGRPCService groupClientGRPCService;

    @Spy
    @InjectMocks
    private ProjectService projectService;

    @Test
    public void test_get_project_by_id () {
        Mockito.when(projectRepository.findById(1)).thenReturn(new Project());
        Assertions.assertNotNull(projectService.getProjectById(1));
    }

    @Test
    public void test_check_if_project_exists () {
        Mockito.when(projectRepository.existsById(0)).thenReturn(Boolean.FALSE);
        Assertions.assertFalse(projectService.hasProject(0));
    }

    @Test
    public void test_save_project () {
        Mockito.when(projectRepository.save(any(Project.class))).thenReturn(new Project());
        Assertions.assertNotNull(projectService.save(new Project()));
    }

    @Test
    public void test_ui_error_handling () {
        ProjectService.clearErrors();
        Assertions.assertTrue(ProjectService.getCurrentErrors().isEmpty());
        ProjectService.addNewError(new UserError("test", "testing errors"));
        Assertions.assertFalse(ProjectService.getCurrentErrors().isEmpty());
        Assertions.assertTrue(ProjectService.hasErrors());
        ProjectService.clearErrors();
        Assertions.assertFalse(ProjectService.hasErrors());
    }

    @Test
    public void test_getProjectStartDateLimits_noSprints() {
        // Setup
        Project project = new Project(); //Default projects have current date as the start date, and end date 8 months from now

        Calendar lowerLimit = Calendar.getInstance();
        lowerLimit.add(Calendar.YEAR, -1); // Previous year

        Calendar upperLimit = Calendar.getInstance();
        upperLimit.add(Calendar.YEAR, 5); // 5 years from now

        List<Date> expectedResult = Arrays.asList(
                DateUtil.stripTimeFromDate(lowerLimit.getTime()),
                DateUtil.stripTimeFromDate(upperLimit.getTime())
        );

        // Run
        List<Date> result = projectService.getProjectStartDateLimits(project);

        // Assert
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void test_getProjectStartDateLimits_withSprints() {
        // Setup
        Project project = Mockito.spy(new Project());
        List<Sprint> sprints = new ArrayList<>();

        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint firstSprint = new Sprint(project);
        firstSprint.setStartDate(DateUtil.addDaysToDate(firstSprint.getStartDate(), 5));
        sprints.add(firstSprint);

        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint secondSprint = new Sprint(project);
        sprints.add(secondSprint);

        Calendar lowerLimit = Calendar.getInstance();
        lowerLimit.add(Calendar.YEAR, -1); // Previous year

        List<Date> expectedResult = Arrays.asList(
                DateUtil.stripTimeFromDate(lowerLimit.getTime()),
                firstSprint.getStartDate()
        );

        // Run
        Mockito.when(project.getSprints()).thenReturn(sprints);
        List<Date> result = projectService.getProjectStartDateLimits(project);

        // Assert
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void test_getProjectEndDateLimits_noSprints() {
        // Setup
        Project project = new Project(); //Default projects have current date as the start date, and end date 8 months from now
        project.setStartDate(DateUtil.stripTimeFromDate(DateUtil.addDaysToDate(new Date(), -5)));

        Calendar upperLimit = Calendar.getInstance();
        upperLimit.setTime(project.getStartDate());
        upperLimit.add(Calendar.YEAR, 5); // 5 years from start date
        upperLimit.add(Calendar.DATE, 1);

        // Can't end in past, so today should be the date
        List<Date> expectedResult = Arrays.asList(
                DateUtil.stripTimeFromDate(new Date()),
                DateUtil.stripTimeFromDate(upperLimit.getTime()));

        // Run
        List<Date> result = projectService.getProjectEndDateLimits(project);

        // Assert
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void test_getProjectEndDateLimits_withSprints() {
        // Setup
        Project project = Mockito.spy(new Project());
        List<Sprint> sprints = new ArrayList<>();

        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint firstSprint = new Sprint(project);
        firstSprint.setStartDate(DateUtil.addDaysToDate(firstSprint.getStartDate(), 5));
        sprints.add(firstSprint);

        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint secondSprint = new Sprint(project);
        sprints.add(secondSprint);

        Calendar upperLimit = Calendar.getInstance();
        upperLimit.setTime(project.getStartDate());
        upperLimit.add(Calendar.YEAR, 5); // 5 years from start date
        upperLimit.add(Calendar.DATE, 1);

        List<Date> expectedResult = Arrays.asList(
                DateUtil.stripTimeFromDate(secondSprint.getEndDate()),
                DateUtil.stripTimeFromDate(upperLimit.getTime())
        );

        // Run
        Mockito.when(project.getSprints()).thenReturn(sprints);
        List<Date> result = projectService.getProjectEndDateLimits(project);

        // Assert
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void test_getGroupsForProject() {
        // Setup
        Project project = Mockito.spy(new Project());
        List<ProjectGroup> pGS = new ArrayList<>();
        pGS.add(new ProjectGroup(project.getId(), 1));
        Mockito.when(projectGroupRepository.findProjectGroupsByProjectId(0)).thenReturn(pGS);
        Mockito.when(groupClientGRPCService.getGroup(1)).thenReturn(GroupDetailsResponse.newBuilder().setGroupId(1).setLongName("Test Group").setShortName("TG").build());

        // Run
        List<GroupDetailsResponse> ret = projectService.getGroupsForProject(0);

        // Assert
        Assertions.assertEquals("Test Group", ret.get(0).getLongName());
    }

    @Test
    public void test_linkProjectAndGroup() {
        // Setup
        Project project = Mockito.spy(new Project());
        ProjectGroup pG = new ProjectGroup(project.getId(), 1);
        Mockito.when(projectGroupRepository.save(Mockito.any(ProjectGroup.class))).thenReturn(pG);

        // Run
        ProjectGroup test = projectService.linkProjectAndGroup(0, 1);

        // Assertion
        Assertions.assertEquals(pG, test);
    }

    @Test
    public void test_unlinkProjectAndGroup() {
        ProjectGroup pG = new ProjectGroup(0, 1);

        projectService.unlinkProjectAndGroup(0, 1);
        Mockito.verify(projectGroupRepository, Mockito.times(1)).delete(pG);
    }

    @Test
    public void test_generateDefaultProject_notExists() {
        // Setup
        Project realDefaultProject = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectRepository.findByIsDefaultProject(true)).thenReturn(new ArrayList<>());
        // Return the value passed into the save method
        Mockito.when(projectRepository.save(any(Project.class))).then(returnsFirstArg());

        // Run
        Project returnedDefaultProject = projectService.generateDefaultProject();

        // Assert
        realDefaultProject.setAsDefaultProject();
        Assertions.assertEquals(realDefaultProject.getIsDefaultProject(), returnedDefaultProject.getIsDefaultProject());
        Assertions.assertEquals(realDefaultProject.getName(), returnedDefaultProject.getName());
        Assertions.assertEquals(realDefaultProject.getId(), returnedDefaultProject.getId());
    }

    @Test
    public void test_generateDefaultProject_exists() {
        // Mock
        // Return the value passed into the save method
        Mockito.when(projectRepository.save(any(Project.class))).then(returnsFirstArg());

        // Create default project
        Project realDefaultProject = projectService.generateDefaultProject();
        Mockito.when(projectRepository.findByIsDefaultProject(true)).thenReturn(List.of(realDefaultProject));

        // Run
        Project returnedDefaultProject = projectService.generateDefaultProject();

        // Assert
        realDefaultProject.setAsDefaultProject();
        Assertions.assertEquals(realDefaultProject.getIsDefaultProject(), returnedDefaultProject.getIsDefaultProject());
        Assertions.assertEquals(realDefaultProject.getName(), returnedDefaultProject.getName());
        Assertions.assertEquals(realDefaultProject.getId(), returnedDefaultProject.getId());
    }
}
