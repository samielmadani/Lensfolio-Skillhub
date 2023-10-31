package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectGroupRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Spy
    @InjectMocks
    private GroupService groupService;
    @Mock
    private GroupClientGRPCService groupClientGRPCService;
    @Mock
    private ProjectGroupRepository projectGroups;
    @Mock
    private UserClientGRPCService userClientGRPCService;
    @Mock
    private UserService users;
    @Mock
    private ProjectService projectService;

    protected GroupDetailsResponse groupResponseContainingUsers;
    protected GroupDetailsResponse groupResponseNoUsers;

    @BeforeEach
    public void setDefaultValues () {
        List<UserResponse> userResponseList = new ArrayList<>();
        userResponseList.add(UserResponse.newBuilder().setId(1).build());
        userResponseList.add(UserResponse.newBuilder().setId(2).build());
        userResponseList.add(UserResponse.newBuilder().setId(3).build());
        groupResponseContainingUsers = GroupDetailsResponse.newBuilder().addAllMembers(userResponseList).build();
        groupResponseNoUsers = GroupDetailsResponse.newBuilder().addAllMembers(new ArrayList<>()).build();
    }

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

    @Test
    void testUserInGroup_groupExists_userExists () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(groupResponseContainingUsers);
        when(users.getHighestRole(any())).thenReturn(UserRole.STUDENT);
        when(users.getIdFromAuthState(any())).thenReturn(1);
        assertTrue(groupService.userInGroup(createAuthState("1"), 1));
    }

    @Test
    void testUserInGroup_groupExists_userDoesntExists () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(groupResponseContainingUsers);
        when(users.getHighestRole(any())).thenReturn(null);
        when(users.getIdFromAuthState(any())).thenReturn(4);
        assertFalse(groupService.userInGroup(createAuthState("4"), 1));
    }

    @Test
    void testUserInGroup_groupDoesntExist () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(null);
        when(users.getHighestRole(any())).thenReturn(UserRole.STUDENT);
        when(users.getIdFromAuthState(any())).thenReturn(1);
        assertFalse(groupService.userInGroup(createAuthState("1"), 1));
    }

    @Test
    void testGetUserDTOInGroup_groupExists_containingUsers () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(groupResponseContainingUsers);
        assertEquals(3, groupService.getUserDTOInGroup(1).size());
    }

    @Test
    void testGetUserDTOInGroup_groupExists_containingNoUsers_correctUserInfo () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(groupResponseNoUsers);
        assertEquals(0, groupService.getUserDTOInGroup(1).size());
    }

    @Test
    void testGetUserDTOInGroup_groupDoesntExist () {
        when(groupClientGRPCService.getGroup(1)).thenReturn(null);
        assertNull(groupService.getUserDTOInGroup(1));
    }

    @Test
    void testGetProjectsForGroup_groupDoesntExist () {
        when(projectGroups.findProjectGroupsByGroupId(1)).thenReturn(null);
        List<Project> results = groupService.getProjectsForGroup(1);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetProjectsForGroup_groupDoesExist_linkedProject () {
        List<ProjectGroup> projectGroupsList = new ArrayList<>();
        projectGroupsList.add(new ProjectGroup(1, 1));

        Project project = Mockito.spy(new Project());
        project.setName("TestName");

        when(projectGroups.findProjectGroupsByGroupId(1)).thenReturn(projectGroupsList);
        when(projectService.getProjectById(1)).thenReturn(project);

        List<Project> results = groupService.getProjectsForGroup(1);
        assertSame(results.get(0).getName(), project.getName());
    }

    @Test
    void testGetProjectsForGroup_groupDoesExist_linkedProjects () {
        List<ProjectGroup> projectGroupsList = new ArrayList<>();
        projectGroupsList.add(new ProjectGroup(1, 1));
        projectGroupsList.add(new ProjectGroup(2, 1));

        Project project1 = Mockito.spy(new Project());
        Project project2 = Mockito.spy(new Project());
        project2.setName("TestName");

        when(projectGroups.findProjectGroupsByGroupId(1)).thenReturn(projectGroupsList);
        when(projectService.getProjectById(1)).thenReturn(project1);
        when(projectService.getProjectById(2)).thenReturn(project2);

        List<Project> results = groupService.getProjectsForGroup(1);
        assertSame(results.get(1).getName(), project2.getName());
    }

    @Test
    void testGetProjectsForGroup_groupDoesExist_noLinkedProjects () {
        List<ProjectGroup> projectGroupsList = new ArrayList<>();

        when(projectGroups.findProjectGroupsByGroupId(1)).thenReturn(projectGroupsList);

        List<Project> results = groupService.getProjectsForGroup(1);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetUserDTOListFormat_noUsers () {
        when(groupClientGRPCService.getGroupUserIdsPaginated(1, 5, 1)).thenReturn(new ArrayList<>());
        assertEquals(0, groupService.getUserDTOListFormat(1).size());
    }

    @Test
    void testGetUserDTOListFormat_containingUsers () {
        ArrayList<Integer> userIds = new ArrayList<>();
        userIds.add(1);
        UserResponse response = UserResponse.newBuilder().build();
        when(groupClientGRPCService.getGroupUserIdsPaginated(1, 5, 1)).thenReturn(userIds);
        when(userClientGRPCService.receiveGetUserAccountById(1)).thenReturn(response);
        when(users.getCompleteUserDTO(response)).thenReturn(new UserDTO("Test Test", List.of("teacher")));
        assertEquals(1, groupService.getUserDTOListFormat(1).size());
    }

    @Test
    void testGetAllGroupDTO_noGroups () {
        PaginatedGroupsResponse response = PaginatedGroupsResponse.newBuilder().build();
        when(groupClientGRPCService.getGroups()).thenReturn(response);
        assertEquals(0, groupService.getAllGroupDTO(createAuthState("1")).size());
    }

    @Test
    void testGetAllGroupDTO_containingDefaultGroup () {
        AuthState principal = createAuthState("1");
        GroupDetailsResponse singleGroup = GroupDetailsResponse.newBuilder().setGroupId(1).setLongName("Members without a group").setShortName("MWAG").build();
        PaginatedGroupsResponse response = PaginatedGroupsResponse.newBuilder().addGroups(singleGroup).build();
        when(groupClientGRPCService.getGroups()).thenReturn(response);
        when(groupClientGRPCService.getGroup(1)).thenReturn(singleGroup);
        when(users.isAdmin(principal)).thenReturn(false);
        when(groupClientGRPCService.getGroupUsersPageCount(1, 5)).thenReturn(1);
        when(users.getHighestRole(principal)).thenReturn(UserRole.STUDENT);
        ArrayList<Integer> userIds = new ArrayList<>();
        userIds.add(1);
        UserResponse userResponse = UserResponse.newBuilder().build();
        assertEquals(1, groupService.getAllGroupDTO(principal).size());
    }

    @Test
    void testGetAllGroupDTO_containingNormalGroup () {
        AuthState principal = createAuthState("1");
        GroupDetailsResponse singleGroup = GroupDetailsResponse.newBuilder().setGroupId(1).setLongName("Test Group").setShortName("Test").build();
        PaginatedGroupsResponse response = PaginatedGroupsResponse.newBuilder().addGroups(singleGroup).build();
        when(groupClientGRPCService.getGroups()).thenReturn(response);
        when(groupClientGRPCService.getGroup(1)).thenReturn(singleGroup);
        when(users.isAdmin(principal)).thenReturn(false);
        when(groupClientGRPCService.getGroupUsersPageCount(1, 5)).thenReturn(1);
        when(users.getHighestRole(principal)).thenReturn(UserRole.STUDENT);
        ArrayList<Integer> userIds = new ArrayList<>();
        userIds.add(1);
        UserResponse userResponse = UserResponse.newBuilder().build();
        assertEquals(1, groupService.getAllGroupDTO(principal).size());
    }
}
