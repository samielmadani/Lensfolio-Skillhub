package nz.ac.canterbury.seng302.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import nz.ac.canterbury.seng302.portfolio.service.GroupClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.CreateGroupResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.DeleteGroupResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;


@RunWith(SpringRunner.class)
@MockBeans({@MockBean(UserClientGRPCService.class), @MockBean(UserService.class), @MockBean(GroupClientGRPCService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers= GroupsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {GroupsController.class, UserClientGRPCService.class, UserService.class, GroupClientGRPCService.class})
public class GroupsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService users;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    @Autowired
    private GroupClientGRPCService groupClientGRPCService;

    private AuthState createAuthState(String id) {
        return AuthState.newBuilder()
                .setIsAuthenticated(true)
                .setNameClaimType("name")
                .setRoleClaimType("role")
                .addClaims(ClaimDTO.newBuilder().setType("role").setValue("TEACHER").build())
                .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue(id).build())
                .build();
    }

    private void runTestAsAdmin() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test
		//Stub any UserService calls and override as we want this user to behave as an Admin
        Mockito.when(users.isAdmin(authState))
                .thenReturn(true);
    }

    @BeforeEach
    public void init() { MockitoAnnotations.openMocks(this); } //Required for mocking to work

    @Test
    public void getGroupPageTest() throws Exception {
        runTestAsAdmin();
        Mockito.when(users.getHighestRole(any(AuthState.class))).thenReturn(UserRole.TEACHER);
        Mockito.when(users.getIdFromAuthState(any(AuthState.class))).thenReturn(1);
        Mockito.when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.get("/groups");

        mvc.perform(request)
                .andExpect(status().isCreated());
    }

    @Test
    public void getGroupUsersTest() throws Exception {
        runTestAsAdmin();
        UserResponse userResponse = UserResponse.newBuilder().setId(1).setFirstName("John").setLastName("Smith").build();
        List<UserResponse> listOfUsers = new ArrayList<>();
        listOfUsers.add(userResponse);
        Mockito.when(groupClientGRPCService.getGroup(1).getMembersList()).thenReturn(listOfUsers);

        RequestBuilder request = MockMvcRequestBuilders.get("api/groups/groupUsers").param("groupId", String.valueOf(1));

        mvc.perform(request)
                .andExpect(status().is2xxSuccessful());

    }

    @Test
    public void getAllGroupsTest() throws Exception {
        runTestAsAdmin();
        List<GroupDetailsResponse> groupDetailsResponses = new ArrayList<>();
        Mockito.when(groupClientGRPCService.getGroups().getGroupsList()).thenReturn(groupDetailsResponses);

        RequestBuilder request = MockMvcRequestBuilders.get("api/groups/allGroups")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        mvc.perform(request)
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void createGroupTest() throws Exception {
        runTestAsAdmin();
        CreateGroupResponse groupResponse =  groupClientGRPCService.createGroup("L", "Lensfolio");
        Mockito.when(groupClientGRPCService.createGroup("L", "Lensfolio")).thenReturn(groupResponse);

        RequestBuilder request = MockMvcRequestBuilders.get("api/groups/new")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        mvc.perform(request)
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void deleteGroupTest() throws Exception {
        runTestAsAdmin();
        int groupId = 3;
        DeleteGroupResponse groupDelete = groupClientGRPCService.deleteGroup(groupId);
        Mockito.when(groupClientGRPCService.deleteGroup(groupId)).thenReturn(groupDelete);

        RequestBuilder request = MockMvcRequestBuilders.get("api/groups/delete")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        mvc.perform(request)
            .andExpect(status().is2xxSuccessful());
    }

}
