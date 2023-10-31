package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.service.GroupClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.GroupService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class UserSearchController {

    private static final Logger logger = LoggerFactory.getLogger(UserSearchController.class);

    @Autowired
    private UserService users;
    @Autowired
    private GroupService groups;
    @Autowired
    private GroupClientGRPCService groupClientGRPCService;

    /**
     * Searches all users by a search query, and gives the stats about if they are in the current group or not.
     * @param groupId ID of the group to check if the users are in
     * @param query Search query to match first name, last name, or username
     * @param page current pagination page
     * @return List of all Users that match the query in UserDTO form
     */
    @GetMapping("api/users/search/{groupId}")
    public String searchUsersInGroup (@PathVariable int groupId, @RequestParam(name = "query") String query, @RequestParam(value = "page") int page, Model model, @AuthenticationPrincipal AuthState principal) {
        if (!query.matches("[a-zA-Z0-9\s]*") || query.isBlank()) {
            model.addAttribute("groupId", groupId);
            model.addAttribute("users", new ArrayList<UserDTO>());
            return "groups/userPaginatedResponse";
        }
        List<UserDTO> allUsers = users.getFilteredPaginatedUsers(query.toLowerCase(), page);
        logger.info("Got {} users for query {} in page {}", allUsers.size(), query.toLowerCase(), page);
        List<UserDTO> allUsersGroupStats = new ArrayList<>();
        //Find all users in the group
        for (UserDTO user : allUsers) {
            if (groups.userIdInGroup(user.getId(), groupId)) {
                user.setInGroup(true);
            }
            allUsersGroupStats.add(user);
        }
        model.addAttribute("users", allUsersGroupStats);
        model.addAttribute("groupId", groupId);
        model.addAttribute("isAdmin", users.isAdmin(principal));
        model.addAttribute("isCourseAdmin", users.getHighestRole(principal)==UserRole.COURSE_ADMINISTRATOR);
        model.addAttribute("getGroup", groupClientGRPCService.getGroup(groupId).getShortName());
        logger.info("Returning {} users as HTML fragment", allUsersGroupStats.size());
        return "groups/userPaginatedResponse";
    }

    /**
     * Searches all users by a search query, and shows the matched query on the particular page
     * @param groupId ID of the group to check if the users are in
     * @param query Search query to match first name, last name, or username
     * @param model - page model
     * @return a string of direction to show the users
     */
    @GetMapping("api/users/search/{groupId}/buttons")
    public String getPaginationButton (@PathVariable int groupId, @RequestParam(name = "query") String query, Model model) {
        if (!query.matches("[a-zA-Z0-9\s]*") || query.isBlank()) {
            model.addAttribute("pages", 1);
            model.addAttribute("id", groupId);

            return "groups/userSearchPagination";
        }
        int pageCount = users.getTotalPages(query);
        logger.info("page count {}", pageCount);
        model.addAttribute("pages", pageCount);
        model.addAttribute("id", groupId);

        return "groups/userSearchPagination";
    }



}
