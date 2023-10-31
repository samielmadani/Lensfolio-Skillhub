package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.entities.Repo;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(RepositoryService.class), @MockBean(UserService.class), @MockBean(GroupClientGRPCService.class), @MockBean(GroupService.class), @MockBean(UserClientGRPCService.class), @MockBean(ProjectService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers= GroupsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {GroupSettingsController.class, RepositoryService.class, UserClientGRPCService.class, UserService.class, GroupClientGRPCService.class, GroupService.class, ProjectService.class})
class GroupSettingsControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private GroupClientGRPCService groupClientGRPCService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserClientGRPCService userClientGRPCService;

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

        Mockito.when(userService.isAdmin(authState))
                .thenReturn(true);
    }

    void testGroupSettingsPage_groupExists_userInGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/groupSettings?groupID=" + 1);
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(true);
        Mockito.when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().setId(1).setUsername("test").build());
        Mockito.when(groupService.getUserDTOInGroup(1)).thenReturn(null);
        Mockito.when(groupClientGRPCService.getGroup(1)).thenReturn(GroupDetailsResponse.newBuilder().setLongName("long").setShortName("short").build());
        Repo validRepo = new Repo(1);
        validRepo.setRepoAlias("This Repo");
        Mockito.when(repositoryService.getRepoForGroup(1)).thenReturn(validRepo);

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateRepositorySettings_validGroup_userInGroup () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/linkRepository")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoAPIKey\": \"_0h27h\", \"projectId\": 10, \"repoAlias\": \"My Repo\", \"branches\": null}");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn(1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(true);
        Repo r = new Repo(1);
        r.setRepoAPIKey("-1");
        r.setProjectId(-1);
        Mockito.when(repositoryService.getRepoForGroup(1)).thenReturn(r);
        ArgumentCaptor<Repo> repoCaptor = ArgumentCaptor.forClass(Repo.class);
        mvc.perform(request)
                        .andExpect(status().isOk());
        verify(repositoryService).save(repoCaptor.capture());
        assertEquals("_0h27h", repoCaptor.getValue().getRepoAPIKey());
    }

    @Test
    void testUpdateRepositorySettings_validGroup_userNotInGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/linkRepository")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoAPIKey\": \"_0h27h\", \"projectId\": 10, \"repoAlias\": \"My Repo\", \"branches\": null}");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn(1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRepositorySettings_invalidGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/linkRepository")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoAPIKey\": \"_0h27h\", \"projectId\": 10, \"repoAlias\": \"My Repo\", \"branches\": null}");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn(1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRepositorySettings_validGroup_userInGroup_invalidBody () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/linkRepository")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sickness\": 14.03}");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRepositoryAlias_validGroup_userInGroup () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/updateRepoAlias?alias=newAlias");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(true);
        Mockito.when(repositoryService.getRepoForGroup(1)).thenReturn(new Repo(1));
        ArgumentCaptor<Repo> repoCaptor = ArgumentCaptor.forClass(Repo.class);
        mvc.perform(request)
                .andExpect(status().isOk());
        verify(repositoryService).save(repoCaptor.capture());
        assertEquals("newAlias", repoCaptor.getValue().getRepoAlias());
    }

    @Test
    void testUpdateRepositoryAlias_validGroup_userNotInGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/updateRepoAlias?alias=newAlias");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRepositoryAlias_invalidGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/updateRepoAlias?alias=newAlias");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRepositoryAlias_validGroup_userInGroup_badRequestParam () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/group/1/updateRepoAlias");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCommitsForBranch_validBody () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/repository/commitsFragment?branchName=tomato")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"commitId\": \"1\", \"commitAuthor\": \"mga114\", \"commitDate\": 1659793457231, \"commitName\": \"my commit\"}]");
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void testGetCommitsForBranch_invalidBody () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/repository/commitsFragment?branchName=tomato")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"commitId\": \"1\", \"commitAuthor\": \"mga114\", \"commitDate\": 1659793457231, \"commitName\": \"my commit\"}");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCommitsForBranch_invalidSearchParam () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/repository/commitsFragment")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"commitId\": \"1\", \"commitAuthor\": \"mga114\", \"commitDate\": 1659793457231, \"commitName\": \"my commit\"}]");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateGroupNames_groupExists_userInGroup () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.put("/api/groups/1?longName=LONNGGG&shortName=sht");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(true);
        ModifyGroupDetailsResponse response = ModifyGroupDetailsResponse.newBuilder().setIsSuccess(true).build();
        Mockito.when(groupClientGRPCService.modifyGroupDetails(1, "LONNGGG", "sht")).thenReturn(response);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateGroupNames_groupExists_userNotInGroup () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/groups/1?longName=LONNGGG&shortName=sht");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateGroupNames_groupDoesntExist () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/groups/1?longName=LONNGGG&shortName=sht");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateGroupNames_groupExists_userInGroup_GRPCerror () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.put("/api/groups/1?longName=LONNGGG&shortName=sht");
        Mockito.when(userService.getIdFromAuthState(any())).thenReturn (1);
        Mockito.when(groupService.userInGroup(createAuthState("1"), 1)).thenReturn(true);
        ModifyGroupDetailsResponse response = ModifyGroupDetailsResponse.newBuilder().setIsSuccess(false).build();
        Mockito.when(groupClientGRPCService.modifyGroupDetails(1, "LONNGGG", "sht")).thenReturn(response);
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateGroupNames_groupExists_userInGroup_badParams () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/groups/1");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }
}
