package nz.ac.canterbury.seng302.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserListDetailsResponseDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.UserState;
import nz.ac.canterbury.seng302.shared.identityprovider.PaginatedUsersResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@MockBeans({@MockBean(UserClientGRPCService.class), @MockBean(UserService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserListController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {UserListController.class, UserClientGRPCService.class, UserService.class})
class UserListControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService users;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

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
    void init() { MockitoAnnotations.openMocks(this); } //Required for mocking to work

    @Test
    void getUsersListPageTest() throws Exception {
        runTestAsAdmin();

        UserState state = new UserState(1);

        Mockito.when(users.getStateByUserId(1)).thenReturn(state);
        Mockito.when(users.getIdFromAuthState(any(AuthState.class))).thenReturn(1);
        Mockito.when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(UserResponse.newBuilder().build());
        Mockito.when(users.getPaginatedUsers(state)).thenReturn(PaginatedUsersResponse.newBuilder().build());
        Mockito.when(users.getTotalPages(any(PaginatedUsersResponse.class))).thenReturn(1);

        RequestBuilder request = MockMvcRequestBuilders.get("/users");

        mvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void getUsersTableBodyTest() throws Exception {
        runTestAsAdmin();

        UserState state = new UserState(1);

        Mockito.when(users.getStateByAuthState(any(AuthState.class))).thenReturn(state);
        Mockito.when(users.getIdFromAuthState(any(AuthState.class))).thenReturn(1);
        Mockito.when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(null);
        Mockito.when(users.getPaginatedUsers(state)).thenReturn(PaginatedUsersResponse.newBuilder().build());
        Mockito.when(users.getTotalPages(any(PaginatedUsersResponse.class))).thenReturn(1);
        Mockito.when(users.saveState(any(UserState.class))).thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.get("/getUsersPage/0").queryParam("searchTerm", "");

        mvc.perform(request).andExpect(status().isOk());
    }

    @Test
    void getUserListDetailsTest() throws Exception {
        runTestAsAdmin();

        UserState state = new UserState(1);

        UserListDetailsResponseDTO response = new UserListDetailsResponseDTO();
        response.setTotalPages(1);
        response.setCurrentPage(state.getPage());

        Mockito.when(users.getStateByAuthState(any(AuthState.class))).thenReturn(state);
        Mockito.when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(null);
        Mockito.when(users.getPaginatedUsers(state)).thenReturn(PaginatedUsersResponse.newBuilder().build());
        Mockito.when(users.getFilteredPaginatedUsersResponse("", state)).thenReturn(PaginatedUsersResponse.newBuilder().build());
        Mockito.when(users.getTotalPages(any(PaginatedUsersResponse.class))).thenReturn(1);

        RequestBuilder request = MockMvcRequestBuilders.get("/api/userListDetails").queryParam("searchTerm", "");

        // Convert body to json
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String responseJson = ow.writeValueAsString(response);

        mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(responseJson));
    }

    @Test
    void sortByTest() throws Exception {
        runTestAsAdmin();

        UserState state = new UserState(1);

        Mockito.when(users.getStateByAuthState(any(AuthState.class))).thenReturn(state);

        RequestBuilder request = MockMvcRequestBuilders.get("/api/userList/sort").queryParam("sortBy", "Username");
        mvc.perform(request).andExpect(status().isOk());

        ArgumentCaptor<UserState> argument = ArgumentCaptor.forClass(UserState.class);
        verify(users).saveState(argument.capture());

        Assertions.assertEquals("Username", argument.getValue().getSortBy());
        Assertions.assertFalse(argument.getValue().isAscending());
    }

    @Test
    void sortByTest_newColumn() throws Exception {
        runTestAsAdmin();

        UserState state = new UserState(1);

        Mockito.when(users.getStateByAuthState(any(AuthState.class))).thenReturn(state);

        RequestBuilder request = MockMvcRequestBuilders.get("/api/userList/sort").queryParam("sortBy", "Nickname");

        mvc.perform(request).andExpect(status().isOk());

        ArgumentCaptor<UserState> argument = ArgumentCaptor.forClass(UserState.class);
        verify(users).saveState(argument.capture());

        Assertions.assertTrue(argument.getValue().isAscending());
        Assertions.assertEquals("Nickname", argument.getValue().getSortBy());
    }
}
