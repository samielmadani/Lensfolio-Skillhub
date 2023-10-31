package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.BadRequest;
import nz.ac.canterbury.seng302.portfolio.model.entities.UserState;
import nz.ac.canterbury.seng302.portfolio.model.repositories.UserStateRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * User service manages the application user details on portfolio side
 */
@Service
public class UserService {
    @Autowired
    private UserStateRepository userStateRepository;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public static final int USER_LIST_SIZE = 20;

    /**
     * Save/update the UserState to the database
     * @param state - UserState to update
     * @return - Updated UserState
     */
    public UserState saveState(UserState state) {
        return userStateRepository.save(state);
    }

    /**
     * Get a UserState entity by the owners Id. Returns null if not found
     * @param userId - UserId of the state owner
     * @return - UserState for user - Null if not found
     */
    public UserState getStateByUserId(int userId) {
        return userStateRepository.findByUserId(userId);
    }

    /**
     * Get a UserState entity by the AuthState principle. Returns null if not found
     * @param principle - AuthState from requesting client
     * @return - UserState for user - Null if not found
     */
    public UserState getStateByAuthState(AuthState principle) {
        int userId = getIdFromAuthState(principle);
        if (userId == -100) {
            return null;
        }

        return userStateRepository.findByUserId(userId);
    }

    /**
     * Get highest power user Role from AuthState principle
     * @param principal - AuthState from requesting client
     * @return - Highest power UserRole for user. Urecognized, if not found
     */
    public UserRole getHighestRole(AuthState principal) {
        int userId = getIdFromAuthState(principal);
        if (userId < 0) return UserRole.UNRECOGNIZED;

        UserResponse user = userClientGRPCService.receiveGetUserAccountById(userId);
        List<UserRole> roles = user.getRolesList();

        logger.info("User Roles: " + roles);

        if (roles.contains(UserRole.COURSE_ADMINISTRATOR)) return UserRole.COURSE_ADMINISTRATOR;
        if (roles.contains(UserRole.TEACHER)) return UserRole.TEACHER;
        if (roles.contains(UserRole.STUDENT)) return UserRole.STUDENT;
        return UserRole.UNRECOGNIZED;
    }

    /**
     * Get userId from AuthState principle
     * @param principal - AuthState from requesting client
     * @return - int, userId
     */
    public int getIdFromAuthState(AuthState principal) {
        return Integer.parseInt(principal.getClaimsList().stream()
                .filter(claim -> claim.getType().equals("nameid"))
                .findFirst()
                .map(ClaimDTO::getValue)
                .orElse("-100"));
    }

    /**
     * Check if the user is an admin (they are not an admin if they are a student)
     * @param principal - AuthState from requesting client
     * @return True if user is not a student
     */
    public boolean isAdmin(AuthState principal) {
        UserRole role = getHighestRole(principal);

        return role == UserRole.COURSE_ADMINISTRATOR || role == UserRole.TEACHER;
    }

    /**
     * Convert UserRole to lower case string.
     * @param role - Role to convert
     * @return student|teacher|course_administrator|unknown
     */
    public static String convertRoleToString(UserRole role) {
        return switch (role) {
            case COURSE_ADMINISTRATOR -> "Course Administrator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
            default -> "Unknown";
        };
    }

    /**
     * Convert string to UserRole.
     * @param role - String to convert
     * @return Correlating UserRole
     */
    public static UserRole convertStringToRole(String role) {
        return switch (role.toLowerCase().replace("_", " ")) {
            case "student" -> UserRole.STUDENT;
            case "teacher" -> UserRole.TEACHER;
            case "course administrator" -> UserRole.COURSE_ADMINISTRATOR;
            default -> UserRole.UNRECOGNIZED;
        };
    }


    /**
     * Getting a userDTO by creating a new userDTO
     * @param userId - the request from user
     * @return new UserDTO with name and list of user's role
     */
    public UserDTO getUserDTO (int userId) {
        UserResponse user = userClientGRPCService.receiveGetUserAccountById(userId);
        if (user == null) return null;

        UserDTO userDto = new UserDTO();
        userDto.setId(userId);
        userDto.addRoles(user.getRolesList());
        userDto.setName(user.getFirstName() + " " + user.getLastName());
        userDto.setUsername(user.getUsername());

        return userDto;
    }


    /**
     * Converts a UserResponse object into a UserDTO
     * @param user UserResponse GRPC object to construct the object based off
     * @return Full Information UserDTO representation of input UserResponse
     */
    public UserDTO getCompleteUserDTO (UserResponse user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setName(user.getFirstName() + " " + user.getLastName());
        userDTO.setUsername(user.getUsername());
        userDTO.setId(user.getId());
        userDTO.setNickname(user.getNickname());
        userDTO.addRoles(user.getRolesList());

        return userDTO;
    }

    /**
     * Finds all the users that match a search query and returns in a paginated form
     * @param query Search query to match with user
     * @param page current page number for pagination
     * @return List of UserDTO representing users found by the query
     */
    public List<UserDTO> getFilteredPaginatedUsers (String query, int page) {
        List<UserDTO> userDTOList = new ArrayList<>();
        PaginatedUsersResponse response;

        // Don't search for an empty string
        if (query.isBlank() || query.isEmpty()) {
            UserState temp = new UserState(-1);
            temp.setPage(0);
            temp.setSortBy("name");
            temp.setAscending(true);

            response = getPaginatedUsers(temp);
        } else {
            //Test input has been sanitised, query should only have letters or numbers
            if (!query.matches("[a-zA-Z0-9\s]*")) throw new BadRequest("Search term not formatted correctly");
            response = userClientGRPCService.receiveGetFilteredPaginatedUsers(query.toLowerCase(), page, USER_LIST_SIZE, "name", true);
        }

        for (UserResponse userResponse : response.getUsersList()) {
            userDTOList.add(getCompleteUserDTO(userResponse));
        }

        return userDTOList;
    }

    /**
     * Finds all the users that match a search query and returns in a paginated form
     * @param query Search query to match with user
     * @param state UserState for accessing UserList variables
     * @return List of UserDTO representing users found by the query
     */
    public List<UserDTO> getFilteredPaginatedUsers (String query, UserState state) {
        List<UserDTO> userDTOList = new ArrayList<>();
        PaginatedUsersResponse response;

        // Don't search for an empty string
        if (query.isBlank() || query.isEmpty()) {
            response = getPaginatedUsers(state);
        } else {
            //Test input has been sanitised, query should only have letters or numbers
            if (!query.matches("[a-zA-Z0-9\s]*")) throw new BadRequest("Search term not formatted correctly");
            response = userClientGRPCService.receiveGetFilteredPaginatedUsers(query.toLowerCase(), Math.max(state.getPage() - 1, 0), USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending());
        }

        for (UserResponse userResponse : response.getUsersList()) {
            userDTOList.add(getCompleteUserDTO(userResponse));
        }

        return userDTOList;
    }

    public PaginatedUsersResponse getFilteredPaginatedUsersResponse (String query, UserState state) {
        // Don't search for an empty string
        if (query.isBlank() || query.isEmpty()) {
            return getPaginatedUsers(state);
        }

        //Test input has been sanitised, query should only have letters or numbers
        if (!query.matches("[a-zA-Z0-9\s]*")) throw new BadRequest("Search term not formatted correctly");
        return userClientGRPCService.receiveGetFilteredPaginatedUsers(query.toLowerCase(), Math.max(state.getPage() - 1, 0), USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending());
    }

    /**
     * Get unfiltered paginated users by user state.
     * @param state - User state containing sorting and page information.
     * @return - The requested page matching the user state requirements.
     */
    public PaginatedUsersResponse getPaginatedUsers(UserState state) {
        // Get paginated users list
        return userClientGRPCService.receiveGetPaginatedUsers(Math.max(state.getPage() - 1, 0), USER_LIST_SIZE, state.getSortBy().toLowerCase(), state.isAscending());
    }

    /**
     * Get the total page for all users that matched the query
     * @param query Search query to match with user
     * @return total of users that matched the query on the particular page
     */
    public int getTotalPages (String query) {
        PaginatedUsersResponse response = userClientGRPCService.receiveGetFilteredPaginatedUsers(query, 1, USER_LIST_SIZE, "name", true);
        logger.info(format("Got %s total users that matched query %s", response.getPaginationResponseOptions().getResultSetSize(), query));
        return response.getPaginationResponseOptions().getResultSetSize();
    }

    /**
     * Get the total number of pages from a given paginated user response
     * @param userList - A single page of users returned from GRPC
     * @return The total number of pages in the database
     */
    public int getTotalPages(PaginatedUsersResponse userList) {
        long totalUsers = userList.getPaginationResponseOptions().getResultSetSize();
        return Math.max((int) totalUsers, 1);
    }
}
