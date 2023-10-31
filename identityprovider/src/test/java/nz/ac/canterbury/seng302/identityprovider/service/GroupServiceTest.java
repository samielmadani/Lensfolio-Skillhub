package nz.ac.canterbury.seng302.identityprovider.service;

import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.GroupRepository;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {
    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserService userService;

    private Group group;

    @BeforeEach
    public void setup() {
        group = new Group("Test", "A Test Group");
    }

    @Test
    public void test_get_groups() {
        Mockito.when(groupRepository.findAll()).thenReturn(List.of(group));
        List<Group> groups = groupService.getGroups();
        assertEquals(1, groups.size());
    }

    @Test
    public void test_get_group_by_id() {
        group = groupService.save("Test", "Test Group Number 1");
        assertEquals(group, groupService.getGroupById(0));
    }

    @Test
    public void test_save_group() {
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenReturn(group);
        Assertions.assertNotNull(groupService.save("Test", "A Test Group"));
    }

    @Test
    public void test_get_by_short_name() {
        Mockito.when(groupRepository.findByShortName("Test")).thenReturn(List.of(group));
        assertEquals(List.of(group), groupService.getGroupByShortName("Test"));
    }

    @Test
    public void test_get_by_long_name() {
        Mockito.when(groupRepository.findByLongName("A Test Group")).thenReturn(List.of(group));
        assertEquals(List.of(group), groupService.getGroupByLongName("A Test Group"));
    }

    @Test
    public void test_update_group_group_exists () {
        Mockito.when(groupRepository.findById(1)).thenReturn(new Group("old short", "old long"));
        Mockito.when(groupRepository.save(Mockito.any(Group.class))).thenReturn(new Group("new short", "new long"));
        boolean response = groupService.updateGroupInfo(1, "new long", "new short");
        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertEquals("new long", groupCaptor.getValue().getLongName());
        assertTrue(response);
    }

    @Test
    public void test_update_group_group_doesnt_exist () {
        Mockito.when(groupRepository.findById(1)).thenReturn(null);
        boolean response = groupService.updateGroupInfo(1, "new long", "new short");
        assertFalse(response);
        verify(groupRepository, never()).save(Mockito.any(Group.class));
    }


    // TODO: This needs to be changed \/ \/ \/
//    @Test
//    public void test_remove_group_user() {
//        group = groupService.save("Test", "Test Group Number 1");
//
//        List<Integer> ids = new ArrayList<>();
//        for (User i : group.getGroupMembers()) {
//            ids.add(i.getUserId());
//        }
//
//        boolean remove = groupService.removeGroupUsers(group.getGroupId(), ids);
//        Assertions.assertEquals(true, remove);
//
//    }
}
