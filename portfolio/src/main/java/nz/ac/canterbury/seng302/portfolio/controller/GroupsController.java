package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.dto.group.GroupDTO;
import nz.ac.canterbury.seng302.portfolio.dto.group.GroupUserDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.CopiedUsersResponseDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.BadRequest;
import nz.ac.canterbury.seng302.portfolio.exceptions.ForbiddenException;
import nz.ac.canterbury.seng302.portfolio.exceptions.NotFoundException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ServerException;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class GroupsController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private GroupClientGRPCService groupClientGRPCService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ProjectUserService projectUserService;


    private final Logger logger = LoggerFactory.getLogger(GroupsController.class);

    private static final int USER_LIST_SIZE = 5;
    private static final String TEACHING_GROUP_NAME = "Teaching Staff";
    private static final String MWAG_GROUP_NAME = "Members without a group";

    /**
     * Groups page controller
     *
     * @param principal - User authenticationPrincipal
     * @param model     - Page model
     * @return - Users table page template
     */
    @GetMapping("/groups")
    public String groupsPage(@AuthenticationPrincipal AuthState principal, Model model) {

        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(userService.getIdFromAuthState(principal));
        } catch (StatusRuntimeException e){
            throw new ServerException(e.getMessage());
        }

        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("userId", userService.getIdFromAuthState(principal));
        model.addAttribute("courseAdmin", userService.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR);
        model.addAttribute("isAdmin", userService.isAdmin(principal));

        List<GroupDTO> allGroups = groupService.getAllGroupDTO (principal);
        model.addAttribute("groups", allGroups);

        return "groups/groups";
    }

    /**
     * Gets a list of all group IDS in JSON
     * @return List of group ID integers
     */
    @ResponseBody
    @GetMapping("api/groups/ids")
    public List<Integer> getGroupIds() {
        PaginatedGroupsResponse groups = groupClientGRPCService.getGroups();

        List<GroupDetailsResponse> groupsList = groups.getGroupsList();
        List<Integer> ids = new ArrayList<>();
        for (GroupDetailsResponse groupDetailsResponse : groupsList) {
            ids.add(groupDetailsResponse.getGroupId());
        }
        return ids;
    }

    /**
     * Get a group card fragment
     * @return Card fragment HTML
     */
    @GetMapping("group/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String groupFragment(@AuthenticationPrincipal AuthState principal, @PathVariable("id") int groupId, Model model) {
        GroupDetailsResponse group = groupClientGRPCService.getGroup(groupId);

        model.addAttribute("isDefault",
                group.getLongName().equals(MWAG_GROUP_NAME) || group.getLongName().equals(TEACHING_GROUP_NAME));

        int numberOfPages = groupClientGRPCService.getGroupUsersPageCount(groupId, USER_LIST_SIZE);

        logger.info("Number of pages for {}: {}", group.getShortName(), numberOfPages);

        model.addAttribute("pages", numberOfPages);
        model.addAttribute("group", group);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        model.addAttribute("canEdit", groupService.userCanEditGroup(principal, groupId));
        model.addAttribute("userCourseAdmin", userService.getHighestRole(principal) == UserRole.COURSE_ADMINISTRATOR);
        model.addAttribute("users", groupService.getUserDTOListFormat (groupId));
        return "groups/groupCard";
    }

    /**
     * API GET Method for getting group users ids
     * @param id - Group ID being displayed on the page
     * @return - List of lists containing group data
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("api/groups/{id}/users")
    public List<Integer> getGroupUsers(@PathVariable (value = "id") int id, @RequestParam(value = "page") int page) {
        List<Integer> userIds = groupClientGRPCService.getGroupUserIdsPaginated(id, USER_LIST_SIZE, page);
        logger.info("Paginated userIds: {}", userIds);
        return userIds;
    }

    /**
     * API GET Method for getting the count of all users within a group
     * @param id - GroupID
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("api/groups/{id}/users/count")
    public int getGroupUsersCount(@PathVariable (value = "id") int id) {
        return groupClientGRPCService.getGroupUsersTotalCount(id);
    }

    @ResponseBody
    @GetMapping("api/groups/{id}/name")
    public String getSingleGroupNameById(@AuthenticationPrincipal AuthState principal, @PathVariable String id) {
        GroupDetailsResponse groupInfo = groupClientGRPCService.getGroup(Integer.parseInt(id));

        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to have access.");
            throw new ForbiddenException();
        }
        return groupInfo.getShortName();
    }

    /**
     * Get a group user item fragment
     * @param id - UserID
     */
    @GetMapping("/api/groups/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String getSingleUserInfo(@PathVariable("id") int id, @RequestParam(value = "groupId") int groupId, Model model) {
        try {
            UserResponse user = userClientGRPCService.receiveGetUserAccountById(id);
            if (user == null) {
                throw new NotFoundException("user", id);
            }

            model.addAttribute("userId", id);
            model.addAttribute("groupId", groupId);
            model.addAttribute("userName", user.getUsername());
            model.addAttribute("fullName", user.getFirstName() + " " + user.getLastName());
            model.addAttribute("roles", user.getRolesList());

            return "groups/userItem";
        } catch (NotFoundException e) {
            logger.error("[getSingleUserFragment] - NOT FOUND: {}", e.getMessage());
            throw new ServerException("Couldn't find group");
        } catch (Exception e) {
            logger.error("[getSingleUserFragment] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get an entire page of users in the form of an HTML fragment
     */
    @GetMapping("/api/groups/{id}/users/page/{page}")
    @ResponseStatus(HttpStatus.OK)
    public String getGroupPage(@PathVariable("id") int groupId, @PathVariable("page") int page, Model model) {
        try {
            List<Integer> userIds = groupClientGRPCService.getGroupUserIdsPaginated(groupId, USER_LIST_SIZE, page);
            List<UserDTO> userDTOList = new ArrayList<>();

            for (Integer userId : userIds) {
                UserResponse user = userClientGRPCService.receiveGetUserAccountById(userId);
                if (user == null) {
                    throw new NotFoundException("user", groupId);
                }

                userDTOList.add(new UserDTO(user));
            }

            model.addAttribute("groupId", groupId);
            model.addAttribute("users", userDTOList);

            return "groups/userList";
        } catch (NotFoundException e) {
            logger.error("[getGroupPage] - NOT FOUND: {}", e.getMessage());
            throw new ServerException("Couldn't find group");
        } catch (Exception e) {
            logger.error("[getGroupPage] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * Get the paginated buttons html
     * @param groupId - Group to get buttons for
     */
    @GetMapping("/groups/{id}/buttons")
    @ResponseStatus(HttpStatus.OK)
    public String getPaginationButtons(@PathVariable("id") int groupId, Model model) {
        try {
            int pageCount = groupClientGRPCService.getGroupUsersPageCount(groupId, USER_LIST_SIZE);

            model.addAttribute("pages", pageCount);
            model.addAttribute("id", groupId);
            return "groups/paginationButtons";
        } catch (NotFoundException e) {
            logger.error("[getPaginationButtons] - NOT FOUND: {}", e.getMessage());
            throw new ServerException("Couldn't find group");
        } catch (Exception e) {
            logger.error("[getPaginationButtons] - {}", e.getMessage());
            throw new ServerException("Something went wrong");
        }
    }

    /**
     * API GET Method for getting all the current groups
     *
     * @return - List of lists containing group names and ids
     */
    @ResponseBody
    @GetMapping("api/groups/allGroups")
    public List<List<String>> getAllGroups() {
        List<GroupDetailsResponse> groups = groupClientGRPCService.getGroups().getGroupsList();

        //Split groups into name strings
        List<List<String>> finalList = new ArrayList<>();
        for (GroupDetailsResponse group : groups) {
            List<String> tempList = new ArrayList<>();
            tempList.add(group.getShortName());
            tempList.add(String.valueOf(group.getGroupId()));
            finalList.add(tempList);
        }

        return finalList;
    }

    /**
     * API GET Method for creating groups
     * @param shortName - Short name of group as inputted by user
     * @param longName - Long name of group as inputted by user
     */
    @ResponseBody
    @GetMapping("api/groups/new")
    public int createGroup(@RequestParam(value = "short") String shortName,
                              @RequestParam(value = "long") String longName,
                              @AuthenticationPrincipal AuthState principal) {
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to have access.");
            throw new ForbiddenException();
        }

        if (longName.equals(MWAG_GROUP_NAME) || longName.equals(TEACHING_GROUP_NAME)) {
            logger.error("Cannot create a group with the same name as the default groups");
            throw new BadRequest();
        }

        List<GroupDetailsResponse> groups = groupClientGRPCService.getGroups().getGroupsList();
        List<List<String>> finalList = new ArrayList<>();
        for (GroupDetailsResponse group : groups) {
            List<String> tempList = new ArrayList<>();
            tempList.add(group.getShortName());
            tempList.add(group.getLongName());
            finalList.add(tempList);
        }

        for (List<String> group : finalList) {
            if (Objects.equals(group.get(0), shortName)) {
                logger.error("Short name already exists");
                throw new BadRequest();
            }
            if (Objects.equals(group.get(1), longName)) {
                logger.error("Long name already exists");
                throw new BadRequest();
            }
        }

        CreateGroupResponse groupResponse = groupClientGRPCService.createGroup(shortName, longName);

        if (groupResponse.getIsSuccess()) {
            return groupResponse.getNewGroupId();
        } else {
            throw new ServerException("Failed to create group.");
        }
    }

    /**
     * API DELETE method that deletes a selected group
     */
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("api/groups/{id}")
    public int deleteGroup(@PathVariable(value = "id") int groupId, @AuthenticationPrincipal AuthState principal) {
        if (!userService.isAdmin(principal)) {
            throw new ForbiddenException();
        }

        GroupDetailsResponse response = groupClientGRPCService.getGroup(groupId);
        if (response.getLongName().equals(MWAG_GROUP_NAME) || response.getLongName().equals(TEACHING_GROUP_NAME)) {
            logger.error("Deleting default group.");
            throw new BadRequest();
        }

        groupClientGRPCService.deleteGroup(groupId);

        int mwagId = -1;
        List<GroupDetailsResponse> allGroups = groupClientGRPCService.getGroups().getGroupsList();
        for (GroupDetailsResponse group : allGroups) {
            if (group.getShortName().equals("MWAG"))
                mwagId = group.getGroupId();
        }

        return mwagId;
    }

    /**
     * API GET method that removes all the selected users
     *
     * @param principal - User authentication state
     * @return a RemovedUserResponseDTO with the users added to MWAG
     */
    @ResponseBody
    @DeleteMapping("api/groups/remove")
    public List<Integer> removeUser(@AuthenticationPrincipal AuthState principal,
                                             @RequestBody List<GroupUserDTO> selected) {
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to have access.");
        }
        Set<Integer> alteredGroups = new HashSet<>();

        // Getting the id of the group called Members Without a Group
        List<GroupDetailsResponse> allGroups = groupClientGRPCService.getGroups().getGroupsList();
        for (GroupDetailsResponse group : allGroups) {
            if (group.getShortName().equals("MWAG"))
                alteredGroups.add(group.getGroupId());
        }

        for (GroupUserDTO user : selected) {
            // Makes sure to not remove from MWAG
            if (!MWAG_GROUP_NAME.equals(groupClientGRPCService.getGroup(user.getGroupId()).getLongName())) {
                List<Integer> tempList = new ArrayList<>();
                tempList.add(user.getUserId());
                groupClientGRPCService.removeGroupMembers(tempList, user.getGroupId());
                groupService.removeUserFromGroupProjects(user.getGroupId(), user.getUserId());
                alteredGroups.add(user.getGroupId());
            }
        }

        return alteredGroups.stream().toList();
    }

    /**
     * API PUT method that adds the selected users to the groups
     *
     * @return a List<Integer> containing all modified groups, so that they can be updated
     */
    @ResponseBody
    @PutMapping("api/groups/add")
    public List<Integer> addUser(@AuthenticationPrincipal AuthState principal, @RequestBody List<GroupUserDTO> selected) {
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to have access.");
        }
        Set<Integer> alteredGroups = new HashSet<>();

        // Getting the id of the group called Members Without a Group
        List<GroupDetailsResponse> allGroups = groupClientGRPCService.getGroups().getGroupsList();
        for (GroupDetailsResponse group : allGroups) {
            if (group.getShortName().equals("MWAG"))
                alteredGroups.add(group.getGroupId());
        }

        for (GroupUserDTO user : selected) {
            // Makes sure to not add to MWAG
            if (!MWAG_GROUP_NAME.equals(groupClientGRPCService.getGroup(user.getGroupId()).getLongName())) {
                List<Integer> tempList = new ArrayList<>();
                tempList.add(user.getUserId());
                groupClientGRPCService.addGroupMembers(tempList, user.getGroupId());
                alteredGroups.add(user.getGroupId());
                //add to new projects
                groupService.addUserToGroupProjects(user.getGroupId(), user.getUserId());
            }
        }

        return alteredGroups.stream().toList();
    }

    @ResponseBody
    @PostMapping("api/groups/{id}/move")
    public CopiedUsersResponseDTO moveUser(@AuthenticationPrincipal AuthState principal,
                                           @PathVariable(value = "id") int targetGroupId,
                                           @RequestBody List<GroupUserDTO> selected) {
        CopiedUsersResponseDTO copiedUsersResponseDTO = new CopiedUsersResponseDTO();
        if (!userService.isAdmin(principal))
            throw new ForbiddenException();
        UserRole highestRole = userService.getHighestRole(principal);
        // Getting the id of the group called Members Without a Group
        List<GroupDetailsResponse> allGroups = groupClientGRPCService.getGroups().getGroupsList();
        int noGroupId = -1;
        int teacherGroupId = -1;
        for (GroupDetailsResponse group : allGroups) {
            if (group.getShortName().equals("MWAG"))
                noGroupId = group.getGroupId();
            else if (group.getShortName().equals("TS"))
                teacherGroupId = group.getGroupId();
        }

        if (teacherGroupId == targetGroupId && highestRole != UserRole.COURSE_ADMINISTRATOR)
            throw new ForbiddenException();


        // List of user's ids to move/copy
        Set<Integer> selectedIdsAll = new HashSet<>();

        // Fill lists and added user to the project
        for (GroupUserDTO user : selected) {
            selectedIdsAll.add(user.getUserId());
            groupService.addUserToGroupProjects(targetGroupId, user.getUserId());
        }

        if (noGroupId != targetGroupId) {
            AddGroupMembersResponse added = groupClientGRPCService.addGroupMembers(new ArrayList<>(selectedIdsAll), targetGroupId);

            if (added.getIsSuccess())
                copiedUsersResponseDTO.setCopied(new ArrayList<>(selectedIdsAll));

        }

        copiedUsersResponseDTO.setNoGroupId(noGroupId);
        return copiedUsersResponseDTO;
    }


}
