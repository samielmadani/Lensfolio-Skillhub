package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.structs.Pronouns;
import nz.ac.canterbury.seng302.shared.identityprovider.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Controller
public class EditPasswordController {

    @Autowired
    private UserClientGRPCService userClientGRPCService;


    @GetMapping("/editPassword")
    public String editDetails(
            @AuthenticationPrincipal AuthState principal,
            Pronouns pronoun,
            Model model
    ) {
        Integer id = Integer.valueOf(principal.getClaimsList().stream()
        .filter(claim -> claim.getType().equals("nameid"))
        .findFirst()
        .map(ClaimDTO::getValue)
        .orElse("-100"));

        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(id);
        } catch (StatusRuntimeException e){
            model.addAttribute("editMessage", "Error connecting to Identity Provider...");
            return "register";
        }

        model.addAttribute("isSuccess", "");
        model.addAttribute("fullName", userReply.getFirstName() + " " + userReply.getLastName());
        model.addAttribute("username", userReply.getUsername());

        UserDetailsController.addUserRolesToModel(model, userReply);
        LocalDate accountDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(accountDate, LocalDate.now());
        String formattedDate = accountDate + " (" + months + " months)";
        model.addAttribute("registrationDate", formattedDate);

        return "editPassword";
    }

    @PostMapping("/editPassword")
    public String addEditChanges(
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(name="currentPassword", required=false, defaultValue="") String currentPassword,
            @RequestParam(name="newPassword", required=false, defaultValue="") String newPassword,
            Model model
    ) {
        
        ChangePasswordResponse registerReply;

        Integer id = Integer.valueOf(principal.getClaimsList().stream()
                .filter(claim -> claim.getType().equals("nameid"))
                .findFirst()
                .map(ClaimDTO::getValue)
                .orElse("-100"));

        try {
            registerReply = userClientGRPCService.receiveChangePasswordRequest(id,
                currentPassword,
                newPassword);
        } catch (StatusRuntimeException e){
            model.addAttribute("editMessage", "Error connecting to Identity Provider...");
            return "register";
        }

        if (registerReply.getIsSuccess()) {
            model.addAttribute("isSuccess", "Change Successful!");
        } else {
            model.addAttribute("isSuccess", "Change Not Succesful!");

        }

        UserResponse newUserReply = userClientGRPCService.receiveGetUserAccountById(id);
        UserDetailsController.addUserRolesToModel(model, newUserReply);
        LocalDate accountDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(accountDate, LocalDate.now());
        String formattedDate = accountDate + " (" + months + " months)";
        model.addAttribute("registrationDate", formattedDate);

        model.addAttribute("username", newUserReply.getUsername());
        model.addAttribute("fullName", newUserReply.getFirstName() + " " + newUserReply.getLastName());

        model.addAttribute("newPasswordError", registerReply.getValidationErrors(0).getErrorText());
        model.addAttribute("oldPasswordError", registerReply.getValidationErrors(1).getErrorText());
        return "editPassword";
    }




}