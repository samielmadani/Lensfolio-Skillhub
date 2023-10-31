package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Controller
public class UserDetailsController {

    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private UserService users;

    @GetMapping("/user_details")
    public String register(
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(name="userId", required=false, defaultValue="-1") String userIdStr,
            Model model
    ) {

        if (principal == null) {
            return "redirect:/login";
        }

        UserResponse userReply;
        boolean myDetails = false;

        int userId = Integer.parseInt(userIdStr);
        if (userId == -1) {
            userId = users.getIdFromAuthState(principal);
            myDetails = true;
            int id = Integer.parseInt(principal.getClaimsList().stream()
                    .filter(claim -> claim.getType().equals("nameid"))
                    .findFirst()
                    .map(ClaimDTO::getValue)
                    .orElse("-100"));
            try {
                userReply = userClientGRPCService.receiveGetUserAccountById(id);
            } catch (StatusRuntimeException e) {
                model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
                return "redirect:/register";
            }
        } else {
            userReply = userClientGRPCService.receiveGetUserAccountById(userId);
        }

        LocalDate accountDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(accountDate, LocalDate.now());
        String formattedDate = accountDate + " (" + months + " months)";

        model.addAttribute("myDetails", myDetails);
        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("firstName", userReply.getFirstName());
        model.addAttribute("middleName", userReply.getMiddleName());
        model.addAttribute("lastName", userReply.getLastName());
        model.addAttribute("nickname", userReply.getNickname());
        model.addAttribute("bio", userReply.getBio());
        model.addAttribute("personalPronouns", userReply.getPersonalPronouns());
        model.addAttribute("email", userReply.getEmail());
        model.addAttribute("fullName", userReply.getFirstName() + " " + userReply.getLastName());
        model.addAttribute("userId", userId);
        model.addAttribute("userViewingName", users.getUserDTO(users.getIdFromAuthState(principal)).getUsername());

        model.addAttribute("registrationDate", formattedDate);

        addUserRolesToModel(model, userReply);

        return "user_details";
    }

    /**
     * Formats user roles into single String and adds to DOM
     * @param model HTML DOM
     * @param userReply UserResponse object of the user to format strings for
     */
    static void addUserRolesToModel(Model model, UserResponse userReply) {
        List<UserRole> roles = userReply.getRolesList();
        StringBuilder userRole = new StringBuilder("Roles:\n");

        if (roles.size() == 1) {
            String first = String.valueOf(roles.get(0).toString().charAt(0)).toUpperCase(Locale.ROOT);
            userRole = new StringBuilder(first + roles.get(0).toString().replace("_", " ").toLowerCase(Locale.ROOT).substring(1));
        }
        else {
            for (UserRole role : roles) {
                String first = String.valueOf(role.toString().charAt(0)).toUpperCase(Locale.ROOT);
                userRole.append(first).append(role.toString().replace("_", " ").toLowerCase(Locale.ROOT).substring(1)).append("\n");
            }
        }
        model.addAttribute("role", userRole.toString());
    }


}
