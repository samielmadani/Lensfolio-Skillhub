package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.MilestoneService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(MilestoneService.class), @MockBean(UserService.class), @MockBean(ProjectService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers= MilestoneController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {MilestoneController.class, MilestoneService.class, UserService.class, ProjectService.class})
class MilestoneControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    MilestoneService milestoneService;

    @Autowired
    UserService userService;

    @Autowired
    ProjectService projectService;

    private final Milestone defaultParamsMilestone = new Milestone (1);
    private final Milestone customParamsMilestoneFull = new Milestone(1);
    private final Milestone customParamsMilestonePartial = new Milestone(1);


    /**
     * Helper function to create a custom AuthState used for testing with full authorisation
     * @param id ID of the user to create
     * @return valid AuthState to pass in as AuthenticationToken
     */
    private AuthState createAuthState (String id) {
        return AuthState.newBuilder()
                .setIsAuthenticated(true)
                .setNameClaimType("name")
                .setRoleClaimType("role")
                .addClaims(ClaimDTO.newBuilder().setType("role").setValue("STUDENT").build())
                .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue(id).build())
                .build();
    }

    /**
     * Creates a Mocked SecurityContext override default behaviour of the MilestoneController built-in spring security
     * This allows api requests to be mocked as if the user making the request is an admin
     */
    private void runTestAsAdmin() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        Mockito.when(userService.isAdmin(authState))
                .thenReturn(true);
    }

    /**
     * Creates a Mocked SecurityContext override default behaviour of the MilestoneController built-in spring security
     * This allows api requests to be mocked as if the user making the request is not an admin
     */
    private void runTestAsStudent() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        Mockito.when(userService.isAdmin(authState))
                .thenReturn(false);
    }

    /**
     * Generates custom values for different types of milestones that are required in testing
     */
    private void populateMilestoneInformation() {
        customParamsMilestoneFull.setName("Custom Milestone");
        Calendar startCalendar = new GregorianCalendar(2022, Calendar.MARCH, 20);
        customParamsMilestoneFull.setStartDate(new Date(startCalendar.getTimeInMillis()));

        customParamsMilestonePartial.setName("My New Milestone");
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this); //Required for mocking to work
        populateMilestoneInformation();

    }

    /**
     * Makes an API request to get all the milestones for a project, assumes milestones already exist for the project
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void getMilestones_validProjectId_containingMilestones() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones");
        Mockito.when(milestoneService.getMilestonesForProject(1))
                .thenReturn(new ArrayList<>(List.of(defaultParamsMilestone)));

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    /**
     * Makes an API request to get all the milestones for a project that doesn't exist
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void getMilestones_invalidProjectId () throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones");
        Mockito.when(milestoneService.getMilestonesForProject(1))
                .thenReturn(null); //mock behaviour of MilestoneService.java when the projectId doesn't exist

        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API request to get all the milestones for a project that has no milestones
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void getMilestones_validProjectId_containingNoMilestones () throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones");
        Mockito.when(milestoneService.getMilestonesForProject(1))
                .thenReturn(new ArrayList<>(List.of()));

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getMilestonesJSON_validProjectId_containingNoMilestones() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestonesJSON");
        Mockito.when(milestoneService.getMilestonesForProject(1)).thenReturn(new ArrayList<>(List.of()));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void getMilestonesJSON_validProjectId_containingMilestones() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestonesJSON");
        Mockito.when(milestoneService.getMilestonesForProject(1)).thenReturn(new ArrayList<>(List.of(defaultParamsMilestone)));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[{}]")); //expect only one returned
    }

    @Test
    void getMilestonesJSON_invalidProjectId() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestonesJSON");
        Mockito.when(milestoneService.getMilestonesForProject(1)).thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API get request to get the edit template for a valid milestone
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_validProjectID_validMilestoneId () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones/1/edit");
        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(new Project());

        mvc.perform(request)
                .andExpect(status().isOk());
    }


    /**
     * Makes an API get request to get the edit template for an invalid milestone
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_validProjectId_milestoneDoesntExist () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones/1/edit");
        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(null);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(new Project());
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }


    /**
     * Makes an API get request to get the edit template for an milestone in a project that doesn't exist
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_invalidProjectId_validMilestoneID () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones/1/edit");
        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }


    /**
     * Makes an API get request to get the edit template for a valid milestone but with invalid authentication
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones/1/edit");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }


    /**
     * Makes an API get request to get the edit template for a valid milestone but with no authentication
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/milestones/1/edit");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }


    /**
     * Makes an API post request to create an milestone with valid data, but not information passed in for the milestone info
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_validAuthentication_validProjectId_emptyBody () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(milestoneService.save(any()))
                .thenReturn(defaultParamsMilestone);


        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"); //no data passed in
        mvc.perform(request)
                .andExpect(status().isCreated());
    }

    /**
     * Makes an API post request to create an milestone but no authentication is provided
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().is4xxClientError());
    }

    /**
     * Makes an API post request to create an milestone with valid authentication but the user is not authorised to
     * make this request
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_invalidAuthentication () throws Exception {
        runTestAsStudent(); //Students aren't authorised to create milestones

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Makes an API post request to create an milestone with valid authentication but invalid projectId
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(milestoneService.save(any()))
                .thenReturn(null); //Mocks what happens if the project doesn't exist in the database

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API post request to create an milestone with valid authentication and a valid projectId with all possible
     * custom values for the milestone to create passed in
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_validAuthentication_validProjectId_fullBodyContent () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(milestoneService.save(any()))
                .thenReturn(customParamsMilestoneFull);

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"Custom Milestone\", \"startDate\": \"2022-03-20T00:00+12:00\", \"endDate\": \"2022-04-20T00:00+12:00\", \"startTime\": \"12:00\", \"endTime\": \"12:30\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Milestone> milestoneCaptor = ArgumentCaptor.forClass(Milestone.class);
        Mockito.verify(milestoneService).save(milestoneCaptor.capture());
        //Tests the datetime values were set to the custom values passed in
        Assertions.assertEquals(customParamsMilestoneFull.toString(), milestoneCaptor.getValue().toString());
    }

    /**
     * Makes an API post request to create an milestone with valid authentication and a valid projectId with some
     * custom values for the milestone to create passed in
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void createMilestone_validAuthentication_validProjectId_partialBodyContent () throws Exception {
        runTestAsAdmin(); //post request requires user to be admin

        Mockito.when(milestoneService.save(any()))
                .thenReturn(customParamsMilestonePartial);

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/milestones")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"My New Milestone\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Milestone> milestoneCaptor = ArgumentCaptor.forClass(Milestone.class);
        Mockito.verify(milestoneService).save(milestoneCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsMilestonePartial.toString(), milestoneCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing milestone testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_validAuthentication_validProjectId_milestoneExists_emptyBody () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.save(any()))
                .thenReturn(defaultParamsMilestone);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Milestone> milestoneCaptor = ArgumentCaptor.forClass(Milestone.class);
        Mockito.verify(milestoneService).save(milestoneCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(defaultParamsMilestone.toString(), milestoneCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing milestone testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_validAuthentication_validProjectId_milestoneExists_fullBodyContent () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.save(any()))
                .thenReturn(customParamsMilestoneFull);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"Custom Milestone\", \"startDate\": \"2022-03-20T00:00+12:00\", \"endDate\": \"2022-04-20T00:00+12:00\", \"startTime\": \"12:00\", \"endTime\": \"12:30\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Milestone> milestoneCaptor = ArgumentCaptor.forClass(Milestone.class);
        Mockito.verify(milestoneService).save(milestoneCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsMilestoneFull.toString(), milestoneCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing milestone testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_validAuthentication_validProjectId_milestoneExists_partialBodyContent () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.save(any()))
                .thenReturn(customParamsMilestonePartial);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"My New Milestone\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Milestone> milestoneCaptor = ArgumentCaptor.forClass(Milestone.class);
        Mockito.verify(milestoneService).save(milestoneCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsMilestonePartial.toString(), milestoneCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating a non-existent milestone
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_validAuthentication_validProjectId_milestoneDoesntExist () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests an API put request for updating an milestone for a non-existent projectId
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.save(any()))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests an API put request for updating an milestone with no authentication provided
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests an API put request for updating an milestone with invalid authentication provided
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void updateMilestone_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/milestones/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests the API delete request for deleting an milestone with valid information
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void deleteMilestone_validAuthentication_validProjectId_validMilestoneId () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.delete(any()))
                .thenReturn(true);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/milestones/1");
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    /**
     * Tests the API delete request for a non-existent milestone
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void deleteMilestone_validAuthentication_validProjectId_invalidMilestoneId () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/milestones/1");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests the API delete request for a non-existent project
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void deleteMilestone_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin();

        Mockito.when(milestoneService.getMilestoneById(1))
                .thenReturn(defaultParamsMilestone);
        Mockito.when(milestoneService.delete(any()))
                .thenReturn(false);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/milestones/1");
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Tests the API delete request with no authentication passed in
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void deleteMilestone_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/milestones/1");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests the API delete request with invalid authentication passed in
     * @throws Exception custom HTTP exception thrown, details can be found in MilestonesExceptionAdvice.java
     */
    @Test
    void deleteMilestone_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/milestones/1");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
}
