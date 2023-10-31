package nz.ac.canterbury.seng302.identityprovider.service;

import io.grpc.channelz.v1.Address;
import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.GroupRepository;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.identityprovider.model.UserRepository;
import nz.ac.canterbury.seng302.identityprovider.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Test
    public void test_encrypt () {
        Assertions.assertTrue(userService.matchPassword("ABC", userService.encrypt("ABC")));
    }

    @Test
    public void test_user_register () {
        User testUser = new User("a@a", "Password", UserRole.STUDENT);
        Group group = new Group("MWAG", "Members without a group");
        UserRegisterRequest req = UserRegisterRequest.newBuilder().setUsername(testUser.getUsername()).setEmail(testUser.getEmail()).setPassword(testUser.getPassword()).build();
        Mockito.when(groupRepository.findByLongName("Members without a group")).thenReturn(List.of(group));
        Mockito.when(userRepository.save(testUser)).thenReturn(testUser);

        Assertions.assertEquals(testUser.getUserId(), userService.register(req).getUserId());
    }

    @Test
    public void test_user_add_group() {
        User testUser = new User("a@a", "Password", UserRole.STUDENT);
        Group group = new Group("MWAG", "Members without a group");
        testUser.addGroup(group);

        Assertions.assertTrue(testUser.getGroups().contains(group));
    }

    @Test
    public void test_user_remove_group() {
        User testUser = new User("a@a", "Password", UserRole.STUDENT);
        Group group = new Group("MWAG", "Members without a group");
        testUser.addGroup(group);

        testUser.removeGroup(group);
        Assertions.assertFalse(testUser.getGroups().contains(group));
    }

    @Test
    public void test_update_user () {
        User testUser = new User("a@a", "Password", UserRole.STUDENT);

        EditUserRequest req = EditUserRequest.newBuilder().setEmail(testUser.getEmail()).setFirstName("John").setUserId(0).build();
        Mockito.when(userRepository.findById(0)).thenReturn(testUser);

        testUser.setFirstName("John");
        Mockito.when(userRepository.save(testUser)).thenReturn(testUser);

        Assertions.assertEquals(testUser, userService.updateUser(req));
    }

    @Test
    public void test_update_user_password () {
        User user = new User("a@a", "Password1", UserRole.STUDENT);
        ChangePasswordRequest req = ChangePasswordRequest.newBuilder().setUserId(user.getUserId()).setCurrentPassword(user.getPassword()).setNewPassword("UpdatedPassword1").build();
        Mockito.when(userRepository.findById(user.getUserId())).thenReturn(user);
        User updatedUser = userService.updateUserPassword(req);
        Assertions.assertTrue(userService.matchPassword("UpdatedPassword1", updatedUser.getPassword()));
    }

    @Test
    public void test_update_user_password_user_doesnt_exist () {
        User user = new User("a@a", "Password1", UserRole.STUDENT);
        ChangePasswordRequest req = ChangePasswordRequest.newBuilder().setUserId(user.getUserId()).setCurrentPassword(user.getPassword()).setNewPassword("UpdatedPassword1").build();
        Mockito.when(userRepository.findById(0)).thenReturn(null);
        Assertions.assertNull(userService.updateUserPassword(req));
    }

    @Test
    public void test_remove_user_role () {
        User user = new User("a@a", "Password1", UserRole.STUDENT);
        user.addRole(UserRole.TEACHER);
        ModifyRoleOfUserRequest req = ModifyRoleOfUserRequest.newBuilder().setUserId(user.getUserId()).setRole(UserRole.TEACHER).build();
        Mockito.when(userRepository.findById(user.getUserId())).thenReturn(user);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
        List<Group> group = new ArrayList<>();
        Group group1 = new Group("TS", "Teaching Staff");
        group.add(group1);
        Mockito.when(groupRepository.findByShortName("TS")).thenReturn(group);
        List<Group> group2 = new ArrayList<>();
        Group group3 = new Group("MWAG", "Members without a group");
        group2.add(group3);
        Mockito.when(groupRepository.findByShortName("MWAG")).thenReturn(group2);
        Assertions.assertTrue(userService.removeUserRole(req));
        
    }

    @Test
    public void test_add_user_role () {
        User user = new User("a@a", "Password1", UserRole.STUDENT);
        ModifyRoleOfUserRequest req = ModifyRoleOfUserRequest.newBuilder().setUserId(user.getUserId()).setRole(UserRole.TEACHER).build();
        Mockito.when(userRepository.findById(user.getUserId())).thenReturn(user);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
        List<Group> group = new ArrayList<>();
        Group group1 = new Group("TS", "Teaching Staff");
        group.add(group1);
        Mockito.when(groupRepository.findByShortName("TS")).thenReturn(group);
        List<Group> group2 = new ArrayList<>();
        Group group3 = new Group("MWAG", "Members without a group");
        group2.add(group3);
        Mockito.when(groupRepository.findByShortName("MWAG")).thenReturn(group2);
        Assertions.assertTrue(userService.addUserRole(req));
    }

    @Test
    public void test_get_user_by_email () {
        Mockito.when(userRepository.findByEmail("a@a")).thenReturn(new User("a@a", "Password", UserRole.STUDENT));
        Assertions.assertNotNull(userService.getUserByEmail("a@a"));
    }
}
