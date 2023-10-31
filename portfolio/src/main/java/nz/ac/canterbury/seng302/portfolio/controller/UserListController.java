package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.dto.user.UserListDetailsResponseDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.BadRequest;
import nz.ac.canterbury.seng302.portfolio.exceptions.ForbiddenException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ServerException;
import nz.ac.canterbury.seng302.portfolio.model.entities.UserState;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static nz.ac.canterbury.seng302.portfolio.service.UserService.USER_LIST_SIZE;

@Controller
public class UserListController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    private final Logger logger = LoggerFactory.getLogger(UserListController.class);

    /**
     * Users table page
     * @param principal - User authenticationPrincipal
     * @param model - Page model
     * @return - Users table page template
     */
    @GetMapping("/users")
    public String users(@AuthenticationPrincipal AuthState principal, Model model,
                        @RequestParam(name="errors", defaultValue = "") String errors) {
        if (principal == null) return "redirect:/login";

        int userId = userService.getIdFromAuthState(principal);

        // Fetch user state
        UserState state = userService.getStateByUserId(userId);
        if (state == null) {
            // Create new state if not already created
            state = new UserState(userId);
        }
        state.setPage(1);
        userService.saveState(state);

        //Get the current users details
        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(userId);
        } catch (StatusRuntimeException e){
            return "redirect:/login";
        }

        PaginatedUsersResponse usersList = userService.getPaginatedUsers(state);

        int totalPages = Math.max((int)Math.ceil((double) userService.getTotalPages(usersList) / (double) USER_LIST_SIZE), 1);

        List<UserDTO> javascriptUsers = new ArrayList<>();
        for (UserResponse userResponse : usersList.getUsersList()) {
            javascriptUsers.add(userService.getCompleteUserDTO(userResponse));
        }

        //Adding attributes
        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("users", javascriptUsers);
        model.addAttribute("previousDisabled", state.getPage() == 1);
        model.addAttribute("nextDisabled", state.getPage() >= totalPages);
        model.addAttribute("error", errors);
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        model.addAttribute("userId", userId);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("userState", state);
        model.addAttribute("pageSize", USER_LIST_SIZE);
        model.addAttribute("numberOfPages", totalPages);

        return "users/users";
    }

    @GetMapping("/getUsersPage/{page}")
    public String getUsersTableBody(@AuthenticationPrincipal AuthState principal, @PathVariable int page, @RequestParam String searchTerm, Model model) {
        if (principal == null) throw new ForbiddenException();
        searchTerm = searchTerm.replaceAll("[\n\r\t]", "_");

        try {
            logger.warn("Search term: {}", searchTerm);
            UserState state = userService.getStateByAuthState(principal);
            state.setPage(page);
            userService.saveState(state);

            List<UserDTO> usersList = userService.getFilteredPaginatedUsers(searchTerm, state);

            model.addAttribute("users", usersList);
            model.addAttribute("isAdmin", userService.isAdmin(principal));

            return "users/userTableBody";
        } catch (Exception e) {
            logger.error("[getUserTableBody] - Cannot get user page");
            logger.error(e.getMessage());
            throw new ServerException();
        }
    }

    @ResponseBody
    @GetMapping("/api/userListDetails")
    public UserListDetailsResponseDTO getUserListDetails(@AuthenticationPrincipal AuthState principal, @RequestParam String searchTerm) {
        if (principal == null) throw new ForbiddenException();

        try {
            UserState state = userService.getStateByAuthState(principal);

            UserListDetailsResponseDTO response = new UserListDetailsResponseDTO();

            PaginatedUsersResponse usersList = userService.getFilteredPaginatedUsersResponse(searchTerm, state);
            if (searchTerm.isEmpty() || searchTerm.isBlank()) {
                response.setTotalPages(Math.max((int)Math.ceil((double) userService.getTotalPages(usersList) / (double) USER_LIST_SIZE), 1));
            } else {
                response.setTotalPages(userService.getTotalPages(usersList));
            }
            response.setCurrentPage(state.getPage());

            return response;
        } catch (Exception e) {
            logger.error("[getUserListDetails] - Cannot get user state");
            logger.error(e.getMessage());
            throw new ServerException();
        }
    }

    public void validateUserRoleModification (UserResponse user) {
        // Checks the user being modified exists
        if (user == null) {
            logger.error("User does not exist");
            throw new BadRequest("User does not exist");
        }

        // Checks a default account is not being modified
        if (user.getUsername().equals("admin200") || user.getUsername().equals("teacher200") || (user.getUsername().equals("student200"))) {
            logger.error("Cannot edit default users.");
            throw new BadRequest("Cannot edit default users.");
        }
    }

    @ResponseBody
    @PostMapping("/api/user/{id}/addRole")
    public boolean addRole(@AuthenticationPrincipal AuthState principal,
                          @PathVariable int id,
                          @RequestParam(value = "role") String role) {
        // Authenticate
        if (!userService.isAdmin(principal)) throw new ForbiddenException();
        role = role.replaceAll("[\n\r\t]", "_");

        UserResponse user = userClientGRPCService.receiveGetUserAccountById(id);
        UserRole userRole = UserService.convertStringToRole(role);

        validateUserRoleModification (user);

        // Checks a defined role is being added
        if (userRole == UserRole.UNRECOGNIZED) {
            logger.error("Unrecognized role being added");
            throw new BadRequest("Unrecognized role being added");
        }

        // Checks that the user does not already have the role
        if (user.getRolesList().contains(userRole)) {
            logger.error("{} already has the role {}", user.getUsername(), role);
            throw new BadRequest(user.getUsername() + " already has the role " + role);
        }

        // Adds the role
        UserRoleChangeResponse response = userClientGRPCService.receiveAddRoleToUser(id, userRole);

        return response.getIsSuccess();
    }

    @ResponseBody
    @DeleteMapping("/api/user/{id}/removeRole")
    public boolean removeRole(@AuthenticationPrincipal AuthState principal,
                           @PathVariable int id,
                           @RequestParam(value = "role") String role) {
        // Authenticate
        if (!userService.isAdmin(principal)) throw new ForbiddenException();

        UserResponse user = userClientGRPCService.receiveGetUserAccountById(id);
        UserRole userRole = UserService.convertStringToRole(role);

        validateUserRoleModification (user);

        // Checks a defined role is being removed
        if (userRole == UserRole.UNRECOGNIZED) {
            logger.error("Unrecognized role being removed");
            throw new BadRequest("Unrecognized role being removed");
        }

        // Checks that the user has more than one role
        if (user.getRolesList().size() == 1) {
            logger.error("Cannot remove last role");
            throw new BadRequest("Cannot remove last role");
        }

        // Removes the role
        UserRoleChangeResponse response = userClientGRPCService.receiveRemoveRoleFromUser(id, userRole);

        return response.getIsSuccess();
    }

    /**
     * Change the table sorting.
     * @param sortBy - New sort by string
     * @return true for ascending, false for descending
     */
    @ResponseBody
    @GetMapping("/api/userList/sort")
    public boolean sortBy(@AuthenticationPrincipal AuthState principal, @RequestParam String sortBy) {
        if (principal == null) throw new ForbiddenException();

        UserState state = userService.getStateByAuthState(principal);

        // If already sorted by this column, swap direction
        if (state.getSortBy().equalsIgnoreCase(sortBy)) {
            state.setAscending(!state.isAscending());
        } else {
            state.setSortBy(sortBy);
            state.setAscending(true);
        }

        userService.saveState(state);
        return state.isAscending();
    }
}
