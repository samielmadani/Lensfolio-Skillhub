package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import org.springframework.beans.factory.annotation.Autowired;
import nz.ac.canterbury.seng302.portfolio.service.UserService;

    @Controller
    public class HomeController {

        /**
         * Controller for home page.
         * @param principal Authentication token
         * @param model Page model
         * @return HTML to display
         */
        @Autowired
        private UserService userService;

        @Autowired
        private UserClientGRPCService userClientGRPCService;

        @GetMapping("/")
        public String index(@AuthenticationPrincipal AuthState principal, Model model) {
            // Redirects the index page to homepage
            return "redirect:/login";
        }

        @GetMapping("/homepage")
        public String home(@AuthenticationPrincipal AuthState principal, Model model) {
            if (principal == null) {
                return "redirect:/login";
            }

            UserResponse userReply;
            try {
                userReply = userClientGRPCService.receiveGetUserAccountById(userService.getIdFromAuthState(principal));
            } catch (StatusRuntimeException e){
                model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
                return "register";
            }

            model.addAttribute("username", userReply.getUsername());
            model.addAttribute("userId", userService.getIdFromAuthState(principal));

            return "homepage";
        }
    }