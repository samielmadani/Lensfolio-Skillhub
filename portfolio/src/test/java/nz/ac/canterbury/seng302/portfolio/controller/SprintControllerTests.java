package nz.ac.canterbury.seng302.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import nz.ac.canterbury.seng302.portfolio.dto.GenericDetailsWithDateDto;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(SprintService.class), @MockBean(UserService.class), @MockBean(ProjectService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SprintController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {SprintController.class, SprintService.class, UserService.class, ProjectService.class})
public class SprintControllerTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private SprintService sprintService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    //Helper function to create a custom AuthState
    private AuthState createAuthState(String id) {
        return AuthState.newBuilder()
                .setIsAuthenticated(true)
                .setNameClaimType("name")
                .setRoleClaimType("role")
                .addClaims(ClaimDTO.newBuilder().setType("role").setValue("STUDENT").build())
                .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue(id).build())
                .build();
    }

    /**
     * Creates a Mocked SecurityContext override default behaviour of the EventController built-in spring security
     * This allows api requests to be mocked as if the user making the request is an admin
     */
    private void runTestAsAdmin(int id) {
        AuthState authState = createAuthState(Integer.toString(id));

        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);

        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK

        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        //Stub any UserService calls and override as we want this user to behave as an Admin
        Mockito.when(userService.isAdmin(authState)).thenReturn(true);
    }

    @BeforeEach
    public void init() { MockitoAnnotations.openMocks(this); } //Required for mocking to work

    @Test
    public void createSprint_validProject_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId); //Create valid SecurityContext for the API
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint newSprint = new Sprint(project);
        Mockito.when(sprintService.save(any())).thenReturn(newSprint);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + project.getId())
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json( Integer.toString(newSprint.getId()) ));
    }


    @Test
    public void createSprint_validProject_notAdmin() throws Exception {
        Project project = Mockito.spy(new Project());

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + project.getId())
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void createSprint_invalidProject_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId); //Create valid SecurityContext for the API
        Project project = Mockito.spy(new Project());

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + (project.getId() + 10))
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getSprintIds_validProject() throws Exception {
        int userId = 1;

        List<Sprint> sprints = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        for (int i=0; i < 5; i++) {
            Sprint newSprint = new Sprint(project);
            sprints.add(newSprint);
            ids.add(newSprint.getId());
        }

        Mockito.when(sprintService.getSprintsByProject(any())).thenReturn(sprints);
        Mockito.when(projectService.hasProject(project.getId())).thenReturn(true);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/getSprintIds/" + project.getId());

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(ids.toString()));
    }

    @Test
    public void getSprintIds_inValidProject() throws Exception {
        int userId = 1;
        Project project = Mockito.spy(new Project());

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/getSprintIds/" + (project.getId() + 10));

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void fetchSprintPartial_validSprint() throws Exception {
        int userId = 1;

        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/sprint/" + sprint.getId());

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    public void updateSprintDetails_validSprint_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + sprint.getId() + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    public void updateSprintDetails_invalidSprint_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + (sprint.getId() + 10) + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateSprintDetails_invalidSprint_notAdmin() throws Exception {
        int userId = 1;
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint sprint = new Sprint(project);

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/sprint/" + sprint.getId() + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void getStartDateBounds_endDateNotSelected() throws Exception {
        int userId = 1;
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint sprint = new Sprint(project);

        // Create date bounds
        List<Date> bounds = new ArrayList<>();
        DateFormat df1 = new SimpleDateFormat(DateUtil.ISO_PATTERN);

        Date minDate = df1.parse("2022-10-10");
        bounds.add(DateUtil.stripTimeFromDate(minDate));
        Date maxDate = df1.parse("2022-10-12");
        bounds.add(DateUtil.stripTimeFromDate(maxDate));

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);
        Mockito.when(sprintService.getSprintStartDateBounds(sprint)).thenReturn(bounds);
        Mockito.when(projectService.getJSFriendlyDates(anyString(), anyList())).thenReturn(Arrays.asList("2022-10-10", "2022-10-12"));

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/getStartDateBounds?sprintId=" + sprint.getId()).accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json("['2022-10-10', '2022-10-12']"));
    }

    @Test
    public void getStartDateBounds_endDateSelected() throws Exception {
        int userId = 1;
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint sprint = new Sprint(project);

        // Create date bounds
        List<Date> bounds = new ArrayList<>();
        DateFormat df1 = new SimpleDateFormat(DateUtil.ISO_PATTERN);

        Date minDate = df1.parse("2022-10-10");
        bounds.add(DateUtil.stripTimeFromDate(minDate));
        Date maxDate = df1.parse("2022-10-15");
        bounds.add(DateUtil.stripTimeFromDate(maxDate));

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);
        Mockito.when(sprintService.getSprintStartDateBounds(sprint)).thenReturn(bounds);
        Mockito.when(projectService.getJSFriendlyDates(anyString(), anyList())).thenReturn(Arrays.asList("2022-10-10", "2022-10-13"));

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get(
                "/api/sprint/getStartDateBounds?sprintId=" + sprint.getId() + "&endDate=2022-10-14")
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json("['2022-10-10', '2022-10-13']"));
    }

    @Test
    public void getEndDateBounds_startDateNotSelected() throws Exception {
        int userId = 1;
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint sprint = new Sprint(project);

        // Create date bounds
        List<Date> bounds = new ArrayList<>();
        DateFormat df1 = new SimpleDateFormat(DateUtil.ISO_PATTERN);

        Date minDate = df1.parse("2022-10-10");
        bounds.add(DateUtil.stripTimeFromDate(minDate));
        Date maxDate = df1.parse("2022-10-12");
        bounds.add(DateUtil.stripTimeFromDate(maxDate));

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);
        Mockito.when(sprintService.getSprintEndDateBounds(sprint)).thenReturn(bounds);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/getEndDateBounds?sprintId=" + sprint.getId()).accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json("['2022-10-10', '2022-10-12']"));
    }

    @Test
    public void getEndDateBounds_startDateSelected() throws Exception {
        int userId = 1;
        List<Sprint> sprints = new ArrayList<>();
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint sprint = new Sprint(project);

        // Create date bounds
        List<Date> bounds = new ArrayList<>();
        DateFormat df1 = new SimpleDateFormat(DateUtil.ISO_PATTERN);

        Date minDate = df1.parse("2022-10-10");
        bounds.add(DateUtil.stripTimeFromDate(minDate));
        Date maxDate = df1.parse("2022-10-15");
        bounds.add(DateUtil.stripTimeFromDate(maxDate));

        Mockito.when(sprintService.getSprintById(sprint.getId())).thenReturn(sprint);
        Mockito.when(sprintService.getSprintEndDateBounds(sprint)).thenReturn(bounds);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get(
                        "/api/sprint/getEndDateBounds?sprintId=" + sprint.getId() + "&startDate=2022-10-12")
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json("['2022-10-13', '2022-10-15']"));
    }

    @Test
    public void deleteSprint_validSprint_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId); //Create valid SecurityContext for the API
        Project project = Mockito.spy(new Project());

        List<Sprint> sprints = new ArrayList<>();

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint newSprint = new Sprint(project);
        Mockito.when(sprintService.delete(newSprint)).thenReturn(true);
        Mockito.when(sprintService.getSprintById(newSprint.getId())).thenReturn(newSprint);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/sprint/" + newSprint.getId());

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    public void deleteSprint_validSprint_notAdmin() throws Exception {
        int userId = 1;

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/sprint/0");

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteSprint_invalidSprint_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId); //Create valid SecurityContext for the API
        Project project = Mockito.spy(new Project());

        List<Sprint> sprints = new ArrayList<>();

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);

        Sprint newSprint = new Sprint(project);
        Mockito.when(sprintService.getSprintById(newSprint.getId())).thenReturn(newSprint);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/sprint/" + (newSprint.getId() + 10));

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getSprintLabel_validSprint() throws Exception {
        int userId = 1;
        Project project = Mockito.spy(new Project());
        List<Sprint> sprints = new ArrayList<>();

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint newSprint = Mockito.spy(new Sprint(project));

        Mockito.when(sprintService.getSprintById(newSprint.getId())).thenReturn(newSprint);
        Mockito.when(newSprint.getLabel()).thenReturn("Label 1");

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/" + newSprint.getId() + "/Label");

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(newSprint.getLabel()));
    }

    @Test
    public void getSprintLabel_invalidSprint() throws Exception {
        int userId = 1;
        Project project = Mockito.spy(new Project());
        List<Sprint> sprints = new ArrayList<>();

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(project.getSprints()).thenReturn(sprints);
        Sprint newSprint = Mockito.spy(new Sprint(project));

        Mockito.when(sprintService.getSprintById(newSprint.getId())).thenReturn(null);
        Mockito.when(newSprint.getLabel()).thenReturn("Label 1");

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/sprint/" + (newSprint.getId() + 10) + "/Label");

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }
}
