package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.authentication.CookieUtil;
import nz.ac.canterbury.seng302.portfolio.service.*;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class ProjectsController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectUserService projectUsers;

    private final Logger logger = LoggerFactory.getLogger(ProjectsController.class);

    @GetMapping("/projects")
    public String projects (
            @AuthenticationPrincipal AuthState principal,
            Model model)
    {
        //Get all projects that exist for the user
        //Display them on a page to let the user select what they want to view
        //If they are a teacher then allow them to create project

        int userId = userService.getIdFromAuthState(principal);
        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(userId);
        } catch (StatusRuntimeException e){
            model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
            return "register";
        }

        // Generate the default project if it doesn't exist
        Project defaultProject = projectService.generateDefaultProject();
        if (!projectUsers.isUserInProject(defaultProject.getId(), userId)) {
            projectUsers.addUserToProject(defaultProject.getId(), userId);
        }
        List<Project> projects = projectUsers.getProjectsForUser(userId);

        model.addAttribute("projects", projects);
        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("isAdmin", userService.isAdmin(principal));
        return "projects";
    }

    @GetMapping("api/createProject")
    public String createProject (@AuthenticationPrincipal AuthState principal){
        int userId = userService.getIdFromAuthState(principal);
        if (!userService.isAdmin(principal)) {
            logger.error("User must be an admin to have access.");
            return "redirect:/projects";
        }

        Project project = new Project();
        projectService.save(project);
        projectUsers.addUserToProject(project.getId(), userId);

        List<Project> usersProjects = projectUsers.getProjectsForUser(userId);

        // Count all the projects the user has access to with the same name
        int count = 0;
        for (Project usersProject : usersProjects) {
            if (usersProject.getName().startsWith(project.getName())) {
                count++;
            }
        }

        // If there is multiple with the same name, add a (x) to the end
        if (count > 1) {
            project.setName(project.getName() + " (" + (count - 1) + ")");
            projectService.save(project);
        }

        return "redirect:/projects";
    }

    @GetMapping ("api/setProject/{projectId}")
    public void setCurrentProjectCookie (@PathVariable int projectId, HttpServletResponse response, HttpServletRequest request) {
        var domain = request.getHeader("host");
        CookieUtil.create(response, "current-project", String.valueOf(projectId), false, 24 * 60 * 60, domain.startsWith("localhost") ? null : domain);
    }
}
