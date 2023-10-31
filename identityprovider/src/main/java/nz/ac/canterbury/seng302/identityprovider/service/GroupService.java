package nz.ac.canterbury.seng302.identityprovider.service;

import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.GroupRepository;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groups;

    @Autowired
    private UserService userService;

    private final Logger logger = LoggerFactory.getLogger(GroupService.class);

    /**
     * Get all groups
     * @return List of Groups
     */
    public List<Group> getGroups() {
        return groups.findAll();
    }

    /**
     * Get the group with the specified ID
     * @param id - ID of the group being requested
     * @return The group that matches the ID supplied
     */
    public Group getGroupById(int id) {
        return groups.findById(id);
    }

    /**
     * Saves a group
     * @param shortName - Short name of the group about to be saved or updated
     * @param longName - Long name of the group about to be saved or updated
     * @return The group that was just created or updated
     */
    public Group save(String shortName, String longName) {
        Group group = new Group(shortName, longName);

        return groups.save(group);
    }

    /**
     * Get the group with the specified short name
     * @param name - Short name of the group being requested
     * @return The group that matches the short name supplied
     */
    public List<Group> getGroupByShortName(String name) {
        return groups.findByShortName(name);
    }

    /**
     * Get the group with the specified long name
     * @param name - Long name of the group being requested
     * @return The group that matches the long name supplied
     */
    public List<Group> getGroupByLongName(String name) {
        return groups.findByLongName(name);
    }

    /**
     * Removes one or more users from a group
     * @param groupId - ID of the group to remove users from
     * @param userIds - List of the User ID's to be removed from the group
     * @return A boolean stating whether the removal worked or not
     */
    public boolean removeGroupUsers(int groupId, List<Integer> userIds) {
        Group g;
        if (groupId == -1) {
            g = getGroupByShortName("TS").get(0);
        } else {
            g = getGroupById(groupId);
        }

        for (Integer i : userIds) {
            // If user is being removed from TS remove Teacher role (same for add)
            User u = userService.getUserById(i);
            if (Objects.equals(g.getLongName(), "Teaching Staff")) {
                if (!Arrays.asList(new String[]{"admin200", "teacher200", "student200"}).contains(u.getUsername())) {
                    if (!u.getRoles().contains(Integer.toString(0))) {
                        u.addRole(UserRole.STUDENT);
                    }
                    if (u.getRoles().contains(Integer.toString(1))) {
                        u.removeRole(UserRole.TEACHER);
                    }
                    if (u.getRoles().contains(Integer.toString(2))) {
                        u.removeRole(UserRole.COURSE_ADMINISTRATOR);
                    }
                }
            }
            if (u.getGroups().size() == 1) {
                List<Group> mwag = getGroupByLongName("Members without a group");
                if (!mwag.isEmpty()) {
                    mwag.get(0).addGroupMember(u);
                    groups.save(mwag.get(0));
                    userService.flushAndUpdate(u);
                }
            }

            g.removeGroupMember(i);
            groups.save(g);
            userService.flushAndUpdate(u);
        }

        return !g.getGroupMembers().contains(userService.getUserById(userIds.get(0))); // Checks for remove
    }

    /**
     * Removes all associates of given group from all its members, and deletes the group
     * @param groupId - ID of the group to delete users from
     * @return A boolean stating whether the removal worked or not
     */
    public boolean deleteGroup(int groupId) {
        Group group = getGroupById(groupId);
        List<Group> mwagList = getGroupByLongName("Members without a group");
        Group mwag = mwagList.get(0);

        for (User i : group.getGroupMembers()) { // Goes through all users in the group and removes that group
            if (i.getGroups().size() == 1) {
                mwag.addGroupMember(i);
                mwag = groups.save(mwag);
                userService.flushAndUpdate(i);
            }

            i.removeGroup(group);                // from the groups that user is in
            userService.flushAndUpdate(i);
        }

        groups.delete(group); // Deletes the group
        return !groups.findAll().contains(group);
    }

    /**
     * Adds one or more users to a group
     * @param groupId - ID of the group to add users to
     * @param userIds - List of the User ID's to be added to the source group
     * @return A boolean stating whether the removal worked or not
     */
    public boolean addGroupUsers(int groupId, List<Integer> userIds) {
        Group group = getGroupById(groupId);
        Group mwag = groups.findByLongName("Members without a group").get(0);

        for (int userId : userIds) {
             User user = userService.getUserById(userId);

             // Remove user from MWAG
             if (mwag.getGroupMembers().contains(user)) {
                 mwag.removeGroupMember(userId);
                 user.removeGroup(mwag);

                 mwag = groups.save(mwag);
                 logger.info("Removing from MWAG: " + user.getUserId());
             }
             if (group.getShortName().equals("TS") && !Objects.equals(user.getUsername(), "student200")) { // If adding to Teaching Staff Group, need to add a teacher role to the user
                if (!user.getRoles().contains(Integer.toString(1))) {
                    user.addRole(UserRole.TEACHER);
                }
                userService.flushAndUpdate(user);

                 group = groups.save(group);
            }

             group.addGroupMember(user);
             user.addGroup(group);
             userService.save(user);
        }

        groups.save(group);

        return true;
    }

    /**
     * Updates a groups short and long name
     * @param groupId ID of the group to update
     * @param longName new Long Name for the group
     * @param shortName new Short Name for the group
     * @return true if success, false otherwise
     */
    public boolean updateGroupInfo (int groupId, String longName, String shortName) {
        Group group = getGroupById(groupId);
        if (group != null) {
            group.setLongName(longName);
            group.setShortName(shortName);
            groups.save(group);
            return true;
        }
        return false;
    }


    /**
     * Create a default group for teaching staff and members without group
     */
    @PostConstruct
    private void createDefaultGroups() {
        List<Group> currentGroup = groups.findByLongName("Teaching Staff");
        /* Adds all teachers and admins to TS */
        if (currentGroup.isEmpty()) {
            Group teachingStaffGroup = new Group("TS", "Teaching Staff");
            for (User u : userService.getAll()) {
                if (u.isTeacher()) {
                    teachingStaffGroup.addGroupMember(u);
                    userService.flushAndUpdate(u);
                }
            }
            groups.save(teachingStaffGroup);
        }
        /* Adds all users without groups to MWAG */
        currentGroup = groups.findByLongName("Members without a group");
        if (currentGroup.isEmpty()) {
            Group specialMembers = new Group("MWAG", "Members without a group");
            for (User u : userService.getAll()) {
                if (u.getGroups().size() == 0) {
                    specialMembers.addGroupMember(u);
                    userService.flushAndUpdate(u);
                }
            }
            groups.save(specialMembers);
        }
    }
}
