package nz.ac.canterbury.seng302.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import nz.ac.canterbury.seng302.portfolio.dto.GenericDetailsWithDateDto;
import nz.ac.canterbury.seng302.portfolio.dto.MinimalProjectDetailsDto;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.service.*;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(UserClientGRPCService.class), @MockBean(UserService.class), @MockBean(ProjectService.class), @MockBean(SprintService.class), @MockBean(GroupService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {ProjectController.class, UserClientGRPCService.class, UserService.class, ProjectService.class, SprintService.class, GroupService.class})
public class ProjectControllerTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private GroupService groupService;

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
    public void updateProjectDetails_validProject_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(projectService.save(project)).thenReturn(project);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/" + project.getId() + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    public void updateProjectDetails_validProject_notAdmin() throws Exception {
        int userId = 1;
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(projectService.save(project)).thenReturn(project);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/" + project.getId() + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateProjectDetails_invalidProject_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(projectService.save(project)).thenReturn(project);

        GenericDetailsWithDateDto body = new GenericDetailsWithDateDto(
                "Moses", "BDAY", "2001-11-16", "2001-11-18");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(body);
        System.out.println(requestJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/" + (project.getId() +10) + "/Update")
                .contentType(APPLICATION_JSON)
                .content(requestJson)
                .accept(MediaType.ALL);

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getMinProjectDetails_validProject() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(projectService.save(project)).thenReturn(project);

        MinimalProjectDetailsDto body = new MinimalProjectDetailsDto();
        body.setName(project.getName());
        body.setDescription(project.getDescription());
        body.setEndDate(project.getFormattedEndDate());
        body.setStartDate(project.getFormattedStartDate());

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String responseJson= ow.writeValueAsString(body);
        System.out.println(responseJson);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/" + project.getId() + "/MinDetails");

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(responseJson));
    }

    @Test
    public void getMinProjectDetails_invalidProject() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        // Mock
        Mockito.when(projectService.getProjectById(project.getId())).thenReturn(project);
        Mockito.when(projectService.save(project)).thenReturn(project);

        //Build the request and set the content and content-type of the request
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/" + (project.getId() + 10) + "/MinDetails");

        //Mock the request
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void linkProjectAndGroup_notAdmin() throws Exception {
        Project project = Mockito.spy(new Project());

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/" + project.getId() + "/linkGroup/" + 1);

        mvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void linkProjectAndGroup_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());
        ProjectGroup pG = new ProjectGroup(project.getId(), 1);
        Mockito.when(projectService.linkProjectAndGroup(project.getId(), 1)).thenReturn(pG);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/" + project.getId() + "/linkGroup/" + 1);

        mvc.perform(request).andExpect(status().isCreated());
    }

    @Test
    public void linkProjectAndGroup_isAdmin_InvalidGroup() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/" + project.getId() + "/linkGroup/" + 1234);

        mvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void unlinkProjectAndGroup_notAdmin() throws Exception {
        Project project = Mockito.spy(new Project());

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/" + project.getId() + "/unlinkGroup/" + 1);

        mvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void unlinkProjectAndGroup_isAdmin() throws Exception {
        int userId = 1;
        runTestAsAdmin(userId);
        Project project = Mockito.spy(new Project());

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/" + project.getId() + "/unlinkGroup/" + 1);

        mvc.perform(request).andExpect(status().isOk());
    }
}
