package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.group.GroupDTO;
import nz.ac.canterbury.seng302.portfolio.dto.group.MinimalGroupDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectGroup;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectGroupRepository;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectUserRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Service
public class GroupService {
    @Autowired
    private GroupClientGRPCService groupClientGRPCService;
    @Autowired
    private ProjectGroupRepository projectGroups;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService users;
    @Autowired
    private ProjectUserService projectUserService;
    @Autowired
    private ProjectUserRepository projectUsers;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    /**
     * Checks if a user exists in a group
     *
     * @param principal ID of the user to check
     * @param groupId   ID of the group to check
     * @return true if user is in the group, false otherwise
     */
    public boolean userInGroup(AuthState principal, int groupId) {
        if (users.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR) return true;
        int userId = users.getIdFromAuthState(principal);
        return userIdInGroup(userId, groupId);
    }

    /**
     * Checks if a user exists in a group
     *
     * @param userId  ID of the user to check
     * @param groupId ID of the group to check
     * @return true if user is in the group, false otherwise
     */
    public boolean userIdInGroup(int userId, int groupId) {
        GroupDetailsResponse groupResponse = groupClientGRPCService.getGroup(groupId);
        if (groupResponse == null) {
            return false;
        }
        List<UserResponse> usersInGroup = groupResponse.getMembersList();
        for (UserResponse user : usersInGroup) {
            if (user.getId() == userId) return true;
        }
        return false;
    }

    /**
     * Gets all the users in a group in the form of a List of UserDTO
     *
     * @param groupId ID of the group to get the users for
     * @return ArrayList of all users in the group in the form of a UserDTO
     */
    public List<UserDTO> getUserDTOInGroup(int groupId) {
        GroupDetailsResponse groupResponse = groupClientGRPCService.getGroup(groupId);
        if (groupResponse == null) {
            return null;
        }
        List<UserResponse> usersInGroup = groupResponse.getMembersList();
        ArrayList<UserDTO> usersDTOInGroup = new ArrayList<>();
        for (UserResponse user : usersInGroup) {
            usersDTOInGroup.add(users.getUserDTO(user.getId()));
        }
        return usersDTOInGroup;
    }

    /**
     * Checks if a user can edit information about a group. This will return true if the group is not MWAG and the user
     * is a part of the group, except in the case that the group is the teachers group, and then will only return true
     * if the user is also an admin
     *
     * @param principal user to check
     * @param groupId   ID of the group to check
     * @return true if the user can edit, false otherwise
     */
    public boolean userCanEditGroup(AuthState principal, int groupId) {
        GroupDetailsResponse groupResponse = groupClientGRPCService.getGroup(groupId);
        if (groupResponse.getShortName().equals("MWAG")) return false;
        if (users.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR) return true;
        if (groupResponse.getShortName().equals("TS")) {
            UserRole highestRole = users.getHighestRole(principal);
            return highestRole == UserRole.COURSE_ADMINISTRATOR;
        }
        return userInGroup(principal, groupId);
    }

    /**
     * Get a list of Projects linked to a specific group ID
     *
     * @return List<Project>
     */
    public List<Project> getProjectsForGroup(int groupId) {
        List<Project> projectDetails = new ArrayList<>();
        List<ProjectGroup> li = projectGroups.findProjectGroupsByGroupId(groupId);

        if (li != null) {
            for (ProjectGroup p : li) {
                projectDetails.add(projectService.getProjectById(p.getProjectId()));
            }
        }

        return projectDetails;
    }

    /**
     * Get UserDTO(s) for initial group pagination view
     *
     * @param groupId ID of the group to get users for
     * @return list of paginated UserDTO that belong in the initial thymeleaf paginated user view
     */
    public List<UserDTO> getUserDTOListFormat(int groupId) {
        List<Integer> userIDResponses = groupClientGRPCService.getGroupUserIdsPaginated(groupId, 5, 1);
        List<UserDTO> userDTOs = new ArrayList<>();
        for (Integer userId : userIDResponses) {
            userDTOs.add(users.getCompleteUserDTO(userClientGRPCService.receiveGetUserAccountById(userId)));
        }
        return userDTOs;
    }

    /**
     * Gets all the groups in the application in a GroupDTO format
     *
     * @param principal Authentication Principal of user making request
     * @return List of all groups in the application in the form of a GroupDTO
     */
    public List<GroupDTO> getAllGroupDTO(AuthState principal) {
        ArrayList<GroupDTO> allGroupDTO = new ArrayList<>();
        List<GroupDetailsResponse> allGroups = groupClientGRPCService.getGroups().getGroupsList();
        for (GroupDetailsResponse groupResponse : allGroups) {
            boolean canEdit = userCanEditGroup(principal, groupResponse.getGroupId());
            boolean isDefault = groupResponse.getLongName().equals("Members without a group") || groupResponse.getLongName().equals("Teaching Staff");
            boolean isAdmin = users.isAdmin(principal);
            int numPages = groupClientGRPCService.getGroupUsersPageCount(groupResponse.getGroupId(), 5);
            boolean userCourseAdmin = users.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR;
            allGroupDTO.add(new GroupDTO(groupResponse, canEdit, isDefault, isAdmin, numPages, userCourseAdmin, new ArrayList<>()));
        }
        return allGroupDTO;
    }

    public List<MinimalGroupDTO> getGroupsForUser (AuthState principal) {
        List<GroupDTO> allGroups = getAllGroupDTO(principal);
        List<MinimalGroupDTO> userGroups = new ArrayList<>();
        for (GroupDTO group : allGroups) {
            if (userInGroup(principal, group.getGroup().getGroupId())) {
                userGroups.add(new MinimalGroupDTO(group.getGroup().getShortName(), group.getGroup().getGroupId()));
            }
        }
        return userGroups;
    }

    /**
     * Check if a project exists in a group and add user to the existing group
     *
     * @param groupId ID of the group that user is added
     * @param userId  ID of the user
     */
    public void addUserToGroupProjects(int groupId, int userId) {
        List<Project> project = getProjectsForGroup(groupId);
        logger.info(format("Found %s projects for group %s", project.size(), groupId));
        for (Project p : project) {
            if (!projectUserService.isUserInProject(p.getId(), userId)) {
                logger.info(format("Added user %s to project %s", userId, p.getId()));
                projectUserService.addUserToProject(p.getId(), userId);
            }
        }
    }

    /**
     * Remove user from existing project
     *
     * @param groupId ID of the group that user is removed
     * @param userId  ID of the user
     */
    public void removeUserFromGroupProjects(int groupId, int userId) {
        List<Project> project = getProjectsForGroup(groupId);
        for (Project p : project) {
            for (GroupDetailsResponse group : projectService.getGroupsForProject(p.getId())) {
                //If there are no other groups that the user is in that also have that project
                if (projectUserService.isUserInProject(p.getId(), userId)) {
                    projectUserService.removeUserFromProject(p.getId(), userId);
                }
            }
        }
    }
}


