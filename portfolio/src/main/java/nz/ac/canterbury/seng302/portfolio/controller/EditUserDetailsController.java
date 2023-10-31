package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.portfolio.structs.Pronouns;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.EditUserResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class EditUserDetailsController {

    @Autowired
    private UserClientGRPCService userClientGRPCService;
    @Autowired
    private UserService users;


    @GetMapping("/editUserDetails")
    public String editDetails(
            @AuthenticationPrincipal AuthState principal,
            Pronouns pronoun,
            Model model
    ) {
        int id = users.getIdFromAuthState(principal);

        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(id);
        } catch (StatusRuntimeException e){
            model.addAttribute("editMessage", "Error connecting to Identity Provider...");
            return "register";
        }

        model.addAttribute("isSuccess", "");

        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("firstName", userReply.getFirstName());
        model.addAttribute("middleName", userReply.getMiddleName());
        model.addAttribute("lastName", userReply.getLastName());
        model.addAttribute("nickname", userReply.getNickname());
        model.addAttribute("bio", userReply.getBio());
        model.addAttribute("email", userReply.getEmail());
        model.addAttribute("fullName", userReply.getFirstName() + " " + userReply.getLastName());
        model.addAttribute("userId", users.getIdFromAuthState(principal));
        
        pronoun.setPronoun(userReply.getPersonalPronouns());
        model.addAttribute("pronouns", pronoun);
        model.addAttribute("personalPronouns", List.of("She/Her","He/Him","They/Them"));

        UserDetailsController.addUserRolesToModel(model, userReply);
        LocalDate accountDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(accountDate, LocalDate.now());
        String formattedDate = accountDate + " (" + months + " months)";
        model.addAttribute("registrationDate", formattedDate);

        UserDetailsController.addUserRolesToModel(model, userReply);

        return "editUserDetails";
    }

    @PostMapping("/editUserDetails")
    public String addEditChanges(
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(name="firstName", required=false, defaultValue="") String firstName,
            @RequestParam(name="middleName", required=false, defaultValue="") String middleName,
            @RequestParam(name="lastName", required=false, defaultValue="") String lastName,
            @RequestParam(name="nickname", required=false, defaultValue="") String nickname,
            @RequestParam(name="bio", required=false, defaultValue="") String bio,
            @RequestParam(name="pronoun", required=false, defaultValue="") String personalPronouns,
            @RequestParam(name="email", required=false, defaultValue="") String email,
            Pronouns pronoun,
            Model model
    ) {
        int id = users.getIdFromAuthState(principal);

        EditUserResponse editUserResponse = userClientGRPCService.receiveEditUserRequest(
                id,
                firstName,
                middleName,
                lastName,
                nickname,
                bio,
                personalPronouns,
                email);

        if (editUserResponse.getIsSuccess()) {
            model.addAttribute("isSuccess", "Change Successful!");
        } else {
            model.addAttribute("isSuccess", "");
        }

        UserResponse newUserReply = userClientGRPCService.receiveGetUserAccountById(id);

        UserDetailsController.addUserRolesToModel(model, newUserReply);
        LocalDate accountDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(accountDate, LocalDate.now());
        String formattedDate = accountDate + " (" + months + " months)";
        model.addAttribute("registrationDate", formattedDate);

        UserDetailsController.addUserRolesToModel(model, newUserReply);
        model.addAttribute("username", newUserReply.getUsername());
        model.addAttribute("firstName", firstName);
        model.addAttribute("middleName", middleName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("fullName", firstName + " " + lastName);
        model.addAttribute("nickname", nickname);
        model.addAttribute("bio", bio);
        model.addAttribute("email", email);
        model.addAttribute("pronouns", pronoun);
        model.addAttribute("personalPronouns", List.of("She/Her","He/Him","They/Them"));
        model.addAttribute("registerMessage", editUserResponse.getMessage());
        model.addAttribute("firstNameError", editUserResponse.getValidationErrors(0).getErrorText());
        model.addAttribute("middleNameError", editUserResponse.getValidationErrors(1).getErrorText());
        model.addAttribute("lastNameError", editUserResponse.getValidationErrors(2).getErrorText());
        model.addAttribute("nicknameError", editUserResponse.getValidationErrors(3).getErrorText());
        model.addAttribute("bioError", editUserResponse.getValidationErrors(4).getErrorText());
        model.addAttribute("emailError", editUserResponse.getValidationErrors(5).getErrorText());

        return "editUserDetails";
    }
}
