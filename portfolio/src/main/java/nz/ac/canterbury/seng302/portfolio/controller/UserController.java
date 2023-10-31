package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.authentication.CookieUtil;
import nz.ac.canterbury.seng302.portfolio.service.AuthenticateClientService;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.structs.Pronouns;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthenticateResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    @Autowired
    private AuthenticateClientService authenticateClientService;

    @GetMapping("/register")
    public String register(
            @AuthenticationPrincipal AuthState principal,
            Model model
    ) {
        

        model.addAttribute("isSuccess", "");


        model.addAttribute("registerMessage", "");
        model.addAttribute("usernameError", "");
        model.addAttribute("passwordError", "");
        model.addAttribute("firstNameError", "");
        model.addAttribute("middleNameError", "");
        model.addAttribute("lastNameError", "");
        model.addAttribute("nickNameError", "");
        model.addAttribute("bioError", "");
        model.addAttribute("emailError", "");
        
        model.addAttribute("pronouns", new Pronouns());
        model.addAttribute("personalPronouns", List.of("She/Her","He/Him","They/Them"));

        return "register";
    }

    @PostMapping("/register")
    public String addRegister(
            @AuthenticationPrincipal AuthState principal,
            @RequestParam(name="username", required=false, defaultValue="") String username,
            @RequestParam(name="password", required=false, defaultValue="") String password,
            @RequestParam(name="firstName", required=false, defaultValue="") String firstName,
            @RequestParam(name="middleName", required=false, defaultValue="") String middleName,
            @RequestParam(name="lastName", required=false, defaultValue="") String lastName,
            @RequestParam(name="nickname", required=false, defaultValue="") String nickname,
            @RequestParam(name="bio", required=false, defaultValue="") String bio,
            @RequestParam(name="pronoun", required=false, defaultValue="") String personalPronouns,
            @RequestParam(name="email", required=false, defaultValue="") String email,
            Pronouns pronoun,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        
        UserRegisterResponse registerReply;
        try {
            registerReply = userClientGRPCService.receiveUserRegistration(username,
                password,
                firstName,
                middleName,
                lastName,
                nickname,
                bio,
                personalPronouns,
                email);
        } catch (StatusRuntimeException e){
            model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
            return "register";
        }



        model.addAttribute("username", username);
        model.addAttribute("password", password);
        model.addAttribute("firstName", firstName);
        model.addAttribute("middleName", middleName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("nickname", nickname);
        model.addAttribute("bio", bio);
        model.addAttribute("email", email);

        model.addAttribute("pronouns", pronoun);
        model.addAttribute("personalPronouns", List.of("She/Her","He/Him","They/Them"));

        model.addAttribute("registerMessage", registerReply.getMessage());
        model.addAttribute("usernameError", registerReply.getValidationErrors(0).getErrorText());
        model.addAttribute("passwordError", registerReply.getValidationErrors(1).getErrorText());
        model.addAttribute("firstNameError", registerReply.getValidationErrors(2).getErrorText());
        model.addAttribute("middleNameError", registerReply.getValidationErrors(3).getErrorText());
        model.addAttribute("lastNameError", registerReply.getValidationErrors(4).getErrorText());
        model.addAttribute("nicknameError", registerReply.getValidationErrors(5).getErrorText());
        model.addAttribute("bioError", registerReply.getValidationErrors(6).getErrorText());
        model.addAttribute("emailError", registerReply.getValidationErrors(7).getErrorText());

        if (registerReply.getIsSuccess()) {
            model.addAttribute("isSuccess", "Signed up successfully!");
        } else {
            model.addAttribute("isSuccess", "Failed!");
            return "register";
        }

        AuthenticateResponse loginReply;

        try {
            loginReply = authenticateClientService.authenticate(username, password);
        } catch (StatusRuntimeException e){
            model.addAttribute("loginMessage", "Error connecting to Identity Provider...");
            return "login";
        }

        if (loginReply.getSuccess()) {
            var domain = request.getHeader("host");
            CookieUtil.create(
                    response,
                    "lens-session-token",
                    loginReply.getToken(),
                    true,
                    5 * 60 * 60, // Expires in 5 hours
                    domain.startsWith("localhost") ? null : domain
            );
            model.addAttribute("Username", username);
            return "redirect:/user_details";
        }


        return "redirect:/user_details";
    }
}