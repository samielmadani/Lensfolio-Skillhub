package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(ProjectService.class), @MockBean(UserService.class), @MockBean(UserClientGRPCService.class), @MockBean(ProjectUserService.class), @MockBean(EvidenceService.class), @MockBean(SprintService.class), @MockBean(GroupService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers=EvidenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes= {EvidenceController.class, ProjectService.class, UserClientGRPCService.class, ProjectUserService.class, EvidenceService.class, GroupService.class, SprintService.class})
class EvidenceControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService users;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private ProjectUserService projectUserService;
    @Autowired
    private EvidenceService evidence;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private GroupService groupService;

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
     * Creates a Mocked SecurityContext override default behaviour of the EventController built-in spring security
     * This allows api requests to be mocked as if the user making the request is an admin
     */
    private void runTestAsAdmin() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        Mockito.when(users.isAdmin(authState))
                .thenReturn(true);
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this); //Required for mocking to work
    }

    @Test
    void getEvidence_noAuthState () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/evidence");
        mvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login"));
    }

    @Test
    void getEvidence_validUserId_userHasNoProjects () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidence");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().setUsername("Test").build());
        when(projectUserService.getProjectsForUser(1)).thenReturn(new ArrayList<>());
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getEvidence_validUserId_userHasProject () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidence");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().setUsername("Test").build());
        ArrayList<Project> projects = new ArrayList<>();
        projects.add(new Project());
        when(projectUserService.getProjectsForUser(1)).thenReturn(projects);
        ArrayList<Evidence> e = new ArrayList<>();
        e.add(new Evidence("Evidence Name", new Date(), "Evidence Description", 1, "username"));
        when(evidence.getEvidenceByUser(1)).thenReturn(e);
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("evidence/evidence"));
    }

    @Test
    void getEvidence_invalidUserId () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidence");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenThrow(new StatusRuntimeException(Status.OK));
        mvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login"));
    }

    @Test
    void getEvidenceById_evidenceExists () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/1?projectId=1");
        Evidence e = new Evidence ("Evidence", new Date(), "Description", 1, "username");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.getEvidenceById(1)).thenReturn(e);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getEvidenceById_evidenceDoesntExist () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/1?projectId=1");
        when(evidence.getEvidenceById(1)).thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    @Test
    void getEvidenceById_badRequest () throws Exception  {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/hey");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void addWebLink_validAuthorization_evidenceExists () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(true);
        when(evidence.addWebLink(1, "https://www.google.com")).thenReturn(true);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void addWebLink_validAuthorization_evidenceDoesntExist () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void addWebLink_invalidAuthorization () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void addWebLink_serverError () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(true);
        when(evidence.addWebLink(1, "https://www.google.com")).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }

    @Test
    void addWebLink_noParam () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/evidence/1/webLink");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeWebLink_validAuthorization_evidenceExists_weblinkExists () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(true);
        when(evidence.removeWebLink(1, "https://www.google.com")).thenReturn(true);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void removeWebLink_validAuthorization_evidenceExists_weblinkDoesntExist () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(true);
        when(evidence.removeWebLink(1, "https://www.google.com")).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }

    @Test
    void removeWebLink_validAuthorization_evidenceDoesntExist () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void removeWebLink_invalidAuthorization () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1/webLink?weblink=https://www.google.com");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    void removeWebLink_noSearchParams () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1/webLink");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAutocompleteSkillResults_validQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/skills?query=test");
        List<String> response = new ArrayList<>();
        response.add("test");
        when (evidence.findSkillQueryMatch("test")).thenReturn(response);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getAutocompleteSkillResults_invalidQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/skills?query=");
        List<String> response = new ArrayList<>();
        when (evidence.findSkillQueryMatch("")).thenReturn(response);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getAutocompleteSkillResults_noQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/skills");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvidenceSkillPage () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidenceSkill?skill=test&projectId=1");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().setUsername("testusername").build());
        when(evidence.findEvidenceBySkill("test")).thenReturn(new ArrayList<>());
        when(projectService.getProjectById(1)).thenReturn(new Project());
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getEvidenceSkillPage_noquery () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidenceSkill");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvidenceCategoryPage () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/evidenceCategory?category=service&projectId=1");
        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().setUsername("testusername").build());
        when(projectService.getProjectById(1)).thenReturn(new Project());


        List<Evidence> evidenceList = new ArrayList<Evidence>();
        Evidence e = new Evidence("My evidence", new Date(), "My description", 1, "username");
        e.setCategories(Collections.singletonList("service"));
        evidenceList.add(e);
        when(evidence.getEvidenceByCategoryAndUserId("service", 1)).thenReturn(evidenceList);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getAllEvidenceSkills () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/evidence/allSkills");
        when(evidence.getSkillsForUser(anyInt())).thenReturn(List.of("skill 1", "skill 2"));
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();

        Assertions.assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void deleteEvidence () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/evidence/1");

        when(users.getIdFromAuthState(any())).thenReturn(1);
        when(evidence.userCanModifyEvidence(1, 1)).thenReturn(true);
        when(evidence.deleteEvidenceById(1)).thenReturn(true);

        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
    }
}
