package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.BadRequest;
import nz.ac.canterbury.seng302.portfolio.model.entities.UserState;
import nz.ac.canterbury.seng302.portfolio.model.repositories.UserStateRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import nz.ac.canterbury.seng302.shared.util.PaginationResponseOptions;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock
    private UserStateRepository stateRepo;

    @Mock
    private UserClientGRPCService grpcService;

    @Spy
    @InjectMocks
    private UserService userService;

    @Test
    void test_saveState_saves() {
        // Setup
        UserState userState = new UserState(0);
        UserState returnedState = new UserState(1);

        // Mock
        Mockito.when(stateRepo.save(userState)).thenReturn(returnedState);

        // Run
        UserState result = userService.saveState(userState);

        // Assert
        Assertions.assertEquals(result, returnedState);
    }

    @Test
    void test_getStateByUserId_returns() {
        // Setup
        int userId = 0;
        UserState userState = new UserState(userId);

        // Mock
        Mockito.when(stateRepo.findByUserId(userId)).thenReturn(userState);

        // Run
        UserState result = userService.getStateByUserId(userId);

        // Assert
        Assertions.assertEquals(result, userState);
    }

    @Test
    void test_getStateByAuthState_matchesId() {
        // Setup
        int userId = 0;
        AuthState principal = AuthState.getDefaultInstance();
        UserState userState = new UserState(userId);

        // Mock
        Mockito.when(stateRepo.findByUserId(userId)).thenReturn(userState);
        Mockito.when(userService.getIdFromAuthState(principal)).thenReturn(userId);

        // Run
        UserState result = userService.getStateByAuthState(principal);

        // Assert
        Assertions.assertEquals(result, userState);
    }

    @Test
    void test_getHighestRole_student() {
        // Setup
        int userId = 0;
        AuthState principal = AuthState.getDefaultInstance();
        UserResponse userDetails = UserResponse.newBuilder().addRoles(UserRole.STUDENT).build();

        // Mock
        Mockito.when(userService.getIdFromAuthState(principal)).thenReturn(userId);
        Mockito.when(grpcService.receiveGetUserAccountById(userId)).thenReturn(userDetails);

        // Run
        UserRole role = userService.getHighestRole(principal);

        // Assert
        Assertions.assertEquals(role, UserRole.STUDENT);
    }

    @Test
    void test_getHighestRole_teacher() {
        // Setup
        int userId = 0;
        AuthState principal = AuthState.getDefaultInstance();
        UserResponse userDetails = UserResponse.newBuilder().addRoles(UserRole.STUDENT).addRoles(UserRole.TEACHER).build();

        // Mock
        Mockito.when(userService.getIdFromAuthState(principal)).thenReturn(userId);
        Mockito.when(grpcService.receiveGetUserAccountById(userId)).thenReturn(userDetails);

        // Run
        UserRole role = userService.getHighestRole(principal);

        // Assert
        Assertions.assertEquals(role, UserRole.TEACHER);
    }

    @Test
    void test_getHighestRole_courseAdmin() {
        // Setup
        int userId = 0;
        AuthState principal = AuthState.getDefaultInstance();
        UserResponse userDetails = UserResponse.newBuilder().addRoles(UserRole.STUDENT).addRoles(UserRole.TEACHER).addRoles(UserRole.COURSE_ADMINISTRATOR).build();

        // Mock
        Mockito.when(userService.getIdFromAuthState(principal)).thenReturn(userId);
        Mockito.when(grpcService.receiveGetUserAccountById(userId)).thenReturn(userDetails);

        // Run
        UserRole role = userService.getHighestRole(principal);

        // Assert
        Assertions.assertEquals(role, UserRole.COURSE_ADMINISTRATOR);
    }

    @Test
    void test_getHighestRole_unrecognized() {
        // Setup
        int userId = 0;
        AuthState principal = AuthState.getDefaultInstance();
        UserResponse userDetails = UserResponse.newBuilder().build();

        // Mock
        Mockito.when(userService.getIdFromAuthState(principal)).thenReturn(userId);
        Mockito.when(grpcService.receiveGetUserAccountById(userId)).thenReturn(userDetails);

        // Run
        UserRole role = userService.getHighestRole(principal);

        // Assert
        Assertions.assertEquals(role, UserRole.UNRECOGNIZED);
    }

    @Test
    void test_isAdmin_false() {
        // Setup
        AuthState principal = AuthState.getDefaultInstance();

        // Mock
        Mockito.when(userService.getHighestRole(principal)).thenReturn(UserRole.STUDENT);

        // Run
        boolean result = userService.isAdmin(principal);

        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void test_isAdmin_true_teacher() {
        // Setup
        AuthState principal = AuthState.getDefaultInstance();

        // Mock
        Mockito.when(userService.getHighestRole(principal)).thenReturn(UserRole.TEACHER);

        // Run
        boolean result = userService.isAdmin(principal);

        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void test_isAdmin_true_courseAdmin() {
        // Setup
        AuthState principal = AuthState.getDefaultInstance();

        // Mock
        Mockito.when(userService.getHighestRole(principal)).thenReturn(UserRole.COURSE_ADMINISTRATOR);

        // Run
        boolean result = userService.isAdmin(principal);

        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void test_convertRoleToString_student() {
        // Run
        String result = UserService.convertRoleToString(UserRole.STUDENT);

        // Assert
        Assertions.assertEquals(result, "Student");
    }

    @Test
    void test_convertRoleToString_teacher() {
        // Run
        String result = UserService.convertRoleToString(UserRole.TEACHER);

        // Assert
        Assertions.assertEquals(result, "Teacher");
    }

    @Test
    void test_convertRoleToString_courseAdmin() {
        // Run
        String result = UserService.convertRoleToString(UserRole.COURSE_ADMINISTRATOR);

        // Assert
        Assertions.assertEquals(result, "Course Administrator");
    }

    @Test
    void test_convertRoleToString_unknown() {
        // Run
        String result = UserService.convertRoleToString(UserRole.UNRECOGNIZED);

        // Assert
        Assertions.assertEquals(result, "Unknown");
    }

    @Test
    void testGetUserDTO_validUserId_studentRole () {
        UserResponse expected = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").addRoles(UserRole.STUDENT).build();
        Mockito.when(grpcService.receiveGetUserAccountById(1)).thenReturn(expected);
        UserDTO result = userService.getUserDTO(1);
        Assertions.assertEquals("Steve Jobs", result.getName());
        Assertions.assertEquals(UserRole.STUDENT, result.getRoles().get(0));
    }

    @Test
    void testGetUserDTO_validUserId_multipleRoles () {
        UserResponse expected = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").addRoles(UserRole.STUDENT).addRoles(UserRole.COURSE_ADMINISTRATOR).build();
        Mockito.when(grpcService.receiveGetUserAccountById(1)).thenReturn(expected);
        UserDTO result = userService.getUserDTO(1);
        Assertions.assertEquals(2, result.getRoles().size());
    }

    @Test
    void testGetUserDTO_invalidUserId () {
        Mockito.when(grpcService.receiveGetUserAccountById(1)).thenReturn(null);
        Assertions.assertNull(userService.getUserDTO(1));
    }

    @Test
    void testGetCompleteUserDTO_validData () {
        UserResponse user = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").setUsername("AppleMan").setId(1).addRoles(UserRole.STUDENT).build();
        UserDTO userDTO = userService.getCompleteUserDTO(user);
        Assertions.assertEquals(1, userDTO.getId());
        Assertions.assertEquals("Steve Jobs", userDTO.getName());
        Assertions.assertEquals("AppleMan", userDTO.getUsername());
    }

    @Test
    void testGetCompleteUserDTO_testRoles () {
        UserResponse user = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").setUsername("AppleMan").setId(1).addRoles(UserRole.STUDENT).addRoles(UserRole.TEACHER).build();
        UserDTO userDTO = userService.getCompleteUserDTO(user);
        Assertions.assertEquals(2, userDTO.getRoles().size());
        Assertions.assertEquals(UserRole.STUDENT, userDTO.getRoles().get(0));
        Assertions.assertEquals(UserRole.TEACHER, userDTO.getRoles().get(1));
    }

    @Test
    void testGetListOfUsers_validQuery_returns1User () {
        UserResponse user = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").addRoles(UserRole.STUDENT).setUsername("appleMan").setId(1).build();
        PaginatedUsersResponse response = PaginatedUsersResponse.newBuilder().addUsers(user).setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(1).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("steve", 0, UserService.USER_LIST_SIZE, "name", true)).thenReturn(response);

        List<UserDTO> usersReturned = userService.getFilteredPaginatedUsers("Steve", 0);
        Assertions.assertEquals(1, usersReturned.size());
        Assertions.assertEquals("Steve Jobs", usersReturned.get(0).getName());
    }

    @Test
    void testGetListOfUsers_validQuery_returnsNoUsers () {
        PaginatedUsersResponse response = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(0).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("steve", 0, UserService.USER_LIST_SIZE, "name", true)).thenReturn(response);

        List<UserDTO> usersReturned = userService.getFilteredPaginatedUsers("Steve", 0);
        Assertions.assertEquals(0, usersReturned.size());
    }

    @Test
    void testGetListOfUsers_validQuery_returns5Users () {
        UserResponse user1 = UserResponse.newBuilder().setFirstName("ASteve").setLastName("Jobs").addRoles(UserRole.STUDENT).setUsername("appleMan").setId(1).build();
        UserResponse user2 = UserResponse.newBuilder().setFirstName("AJoe").setLastName("Biden").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("bigprez").setId(2).build();
        UserResponse user3 = UserResponse.newBuilder().setFirstName("AThin").setLastName("Matrix").addRoles(UserRole.TEACHER).setUsername("ThinMatrix").setId(3).build();
        UserResponse user4 = UserResponse.newBuilder().setFirstName("ASteve").setLastName("Carell").addRoles(UserRole.STUDENT).setUsername("StevieBoy").setId(4).build();
        UserResponse user5 = UserResponse.newBuilder().setFirstName("AJacinda").setLastName("Ardern").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("cindy").setId(5).build();

        PaginatedUsersResponse response = PaginatedUsersResponse.newBuilder().addUsers(user1).addUsers(user2).addUsers(user3).addUsers(user4).addUsers(user5).setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("a", 0, UserService.USER_LIST_SIZE, "name", true)).thenReturn(response);

        List<UserDTO> usersReturned = userService.getFilteredPaginatedUsers("A", 0);
        Assertions.assertEquals(5, usersReturned.size());
    }

    @Test
    void testGetListOfUsers_validQuery_searchFindsNothing () {
        UserResponse user1 = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").addRoles(UserRole.STUDENT).setUsername("appleMan").setId(1).build();
        UserResponse user2 = UserResponse.newBuilder().setFirstName("Joe").setLastName("Biden").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("bigprez").setId(2).build();
        UserResponse user3 = UserResponse.newBuilder().setFirstName("Thin").setLastName("Matrix").addRoles(UserRole.TEACHER).setUsername("ThinMatrix").setId(3).build();
        UserResponse user4 = UserResponse.newBuilder().setFirstName("Steve").setLastName("Carell").addRoles(UserRole.STUDENT).setUsername("StevieBoy").setId(4).build();
        UserResponse user5 = UserResponse.newBuilder().setFirstName("Jacinda").setLastName("Ardern").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("cindy").setId(5).build();

        PaginatedUsersResponse response = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("friendssss", 0, UserService.USER_LIST_SIZE, "name", true)).thenReturn(response);

        List<UserDTO> usersReturned = userService.getFilteredPaginatedUsers("Friendssss", 0);
        Assertions.assertEquals(0, usersReturned.size());
    }

    @Test
    void testGetListOfUsers_validQuery_searchFindsPartial () {
        UserResponse user1 = UserResponse.newBuilder().setFirstName("Steve").setLastName("Jobs").addRoles(UserRole.STUDENT).setUsername("appleMan").setId(1).build();
        UserResponse user2 = UserResponse.newBuilder().setFirstName("Joe").setLastName("Biden").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("bigprez").setId(2).build();
        UserResponse user3 = UserResponse.newBuilder().setFirstName("Thin").setLastName("Matrix").addRoles(UserRole.TEACHER).setUsername("ThinMatrix").setId(3).build();
        UserResponse user4 = UserResponse.newBuilder().setFirstName("Steve").setLastName("Carell").addRoles(UserRole.STUDENT).setUsername("StevieBoy").setId(4).build();
        UserResponse user5 = UserResponse.newBuilder().setFirstName("Jacinda").setLastName("Ardern").addRoles(UserRole.COURSE_ADMINISTRATOR).setUsername("cindy").setId(5).build();

        PaginatedUsersResponse response = PaginatedUsersResponse.newBuilder().addUsers(user1).addUsers(user4).setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("steve", 0, UserService.USER_LIST_SIZE, "name", true)).thenReturn(response);

        List<UserDTO> usersReturned = userService.getFilteredPaginatedUsers("Steve", 0);
        Assertions.assertEquals(2, usersReturned.size());
    }

    @Test
    void testGetListOfUsers_invalidQuery () throws BadRequest {
        Assertions.assertThrows(BadRequest.class, () -> userService.getFilteredPaginatedUsers(":)", 0));
    }

    @Test
    void testGetTotalPages_validQuery_noResults () {
        PaginatedUsersResponse expected = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(0).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("hey", 1, UserService.USER_LIST_SIZE, "name", true)).thenReturn(expected);
        Assertions.assertEquals(0, userService.getTotalPages("hey"));
    }

    @Test
    void testGetTotalPages_validQuery_2Results () {
        PaginatedUsersResponse expected = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(PaginationResponseOptions.newBuilder().setResultSetSize(2).build()).build();
        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("hey", 1, UserService.USER_LIST_SIZE, "name", true)).thenReturn(expected);
        Assertions.assertEquals(2, userService.getTotalPages("hey"));
    }

    @Test
    void testGetTotalPages_userList_0results () {
        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(0).build()).build();

        int result = userService.getTotalPages(list);
        Assertions.assertEquals(1, result);
    }

    @Test
    void testGetTotalPages_userList_lessThanFullPage () {
        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(UserService.USER_LIST_SIZE - 1).build()).build();

        int result = userService.getTotalPages(list);
        Assertions.assertEquals(19, result);
    }

    @Test
    void testGetTotalPages_userList_fullPage () {
        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(UserService.USER_LIST_SIZE).build()).build();

        int result = userService.getTotalPages(list);
        Assertions.assertEquals(20, result);
    }

    @Test
    void testGetTotalPages_userList_oneMoreThanFullPage () {
        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(UserService.USER_LIST_SIZE + 1).build()).build();

        int result = userService.getTotalPages(list);
        Assertions.assertEquals(21, result);
    }

    @Test
    void testGetPaginatedUsers_state_negativePage () {
        UserState state = new UserState(0);
        state.setPage(-1);

        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();

        Mockito.when(grpcService.receiveGetPaginatedUsers(0, UserService.USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending()))
                .thenReturn(list);

        Assertions.assertNotNull(userService.getPaginatedUsers(state));
    }

    @Test
    void testGetPaginatedUsers_state_positivePage () {
        UserState state = new UserState(0);
        state.setPage(2);

        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();

        Mockito.when(grpcService.receiveGetPaginatedUsers(1, UserService.USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending()))
                .thenReturn(list);

        Assertions.assertNotNull(userService.getPaginatedUsers(state));
    }

    @Test
    void testGetFilteredPaginatedUsers_state_validSearch () {
        UserState state = new UserState(0);
        state.setPage(2);

        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();

        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("a",1, UserService.USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending()))
                .thenReturn(list);

        Assertions.assertNotNull(userService.getFilteredPaginatedUsers("A", state));
    }

    @Test
    void testGetFilteredPaginatedUsers_state_invalidSearch () {
        UserState state = new UserState(0);
        state.setPage(2);

        Assertions.assertThrows(BadRequest.class, () -> userService.getFilteredPaginatedUsers("😀😀", state));
    }

    @Test
    void testGetFilteredPaginatedUsersResponse_state_validSearch () {
        UserState state = new UserState(0);
        state.setPage(2);

        PaginatedUsersResponse list = PaginatedUsersResponse.newBuilder().setPaginationResponseOptions(
                PaginationResponseOptions.newBuilder().setResultSetSize(5).build()).build();

        Mockito.when(grpcService.receiveGetFilteredPaginatedUsers("a",1, UserService.USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending()))
                .thenReturn(list);

        Assertions.assertNotNull(userService.getFilteredPaginatedUsers("A", state));
    }

    @Test
    void testGetFilteredPaginatedUsersResponse_state_invalidSearch () {
        UserState state = new UserState(0);
        state.setPage(2);

        Assertions.assertThrows(BadRequest.class, () -> userService.getFilteredPaginatedUsers("😀😀", state));
    }
}
