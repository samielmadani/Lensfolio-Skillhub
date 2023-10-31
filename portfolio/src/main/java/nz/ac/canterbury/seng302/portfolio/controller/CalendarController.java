package nz.ac.canterbury.seng302.portfolio.controller;

import io.grpc.StatusRuntimeException;
import nz.ac.canterbury.seng302.portfolio.exceptions.DomainValidationException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ForbiddenException;
import nz.ac.canterbury.seng302.portfolio.model.UserError;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.Sprint;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.SprintService;
import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

@Controller
public class CalendarController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    @Autowired
    private ProjectService projects;

    @Autowired
    private SprintService sprints;

    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);

    /**
     * Get mapping for the calendar page.
     *
     * @param principal Authentication token.
     * @param projectID ID of the project being displayed on the calendar.
     * @param model Thymeleaf DOM.
     * @return Calendar HTML page with all the added DOM elements.
     */
    @GetMapping("/calendar")
    public String calendar(@AuthenticationPrincipal AuthState principal,
                           @RequestParam(value="projectID") String projectID,
                           Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        //Edit is used to control who can edit the data in the calendar page
        String edit = "false";
        int userId = userService.getIdFromAuthState(principal);
        if (userService.isAdmin(principal)) edit = "true";

        UserResponse userReply;
        try {
            userReply = userClientGRPCService.receiveGetUserAccountById(userId);
        } catch (StatusRuntimeException e){
            model.addAttribute("registerMessage", "Error connecting to Identity Provider...");
            return "register";
        }

        model.addAttribute("username", userReply.getUsername());
        model.addAttribute("userId", userId);


        Project selectedProject = projects.getProjectById(Integer.parseInt(projectID));

        ArrayList<Sprint> sprintList = (ArrayList<Sprint>) sprints.getSprintsByProject(selectedProject);

        // Format the sprint array to work in javascript
        ArrayList<ArrayList<String>> javascriptSprints = new ArrayList<>();
        for (Sprint sprint : sprintList) {
            ArrayList<String> javascriptSprint = new ArrayList<>();
            javascriptSprint.add(sprint.getName());
            javascriptSprint.add(sprint.getStartDateIsoString());
            javascriptSprint.add(sprint.getEndDateIsoString());
            javascriptSprint.add(sprint.getLabel());
            javascriptSprint.add(sprint.getDescription());
            javascriptSprints.add(javascriptSprint);
        }

        //Formats the project array so it will work in javascript
        ArrayList<String> javascriptProject = new ArrayList<>();
        javascriptProject.add(selectedProject.getName());
        javascriptProject.add(selectedProject.getStartDateIsoString());
        Date endDate = DateUtil.addDaysToDate(selectedProject.getEndDate(), 1);
        javascriptProject.add(DateUtil.dateToISOString(endDate));
        javascriptProject.add(String.valueOf(selectedProject.getId()));

        //Adding attributes
        model.addAttribute("sprints", javascriptSprints);
        model.addAttribute("project", javascriptProject);
        model.addAttribute("currentProjectId", projectID);

        model.addAttribute("projectName", selectedProject.getName());
        model.addAttribute("formatProjectStartDate", DateUtil.dateToFormattedString(selectedProject.getStartDate()));
        model.addAttribute("formatProjectEndDate", DateUtil.dateToFormattedString(selectedProject.getEndDate()));
        model.addAttribute("projectDescription", selectedProject.getDescription());
        model.addAttribute("edit", edit);
        model.addAttribute("isAdmin", userService.isAdmin(principal));

        if (Boolean.TRUE.equals(SprintService.hasErrors())) {
            model.addAttribute("errors", SprintService.getCurrentErrors());
        }

        return "calendar";
    }

    /**
     * Api POST mapping for updating a sprint on the calendar.
     *
     * @param principal Authentication token.
     * @param projectID ID of the project being displayed on the calendar.
     * @param sprintLabel Label of the sprint being updated.
     * @param startDateString The new start date string.
     * @param endDateString The new end date string.
     * @param model Thymeleaf DOM.
     * @return The new end date of the updated sprint.
     */
    @ResponseBody
    @PostMapping("/api/updateSprintCalendar")
    public Date updateSprint (@AuthenticationPrincipal AuthState principal,
                                @RequestParam(value="projectID") String projectID,
                                @RequestParam(value="sprintLabel") String sprintLabel,
                                @RequestParam(value="startDate") String startDateString,
                                @RequestParam(value="endDate") String endDateString,
                                Model model) {
        //Prevent student from editing

        if (!userService.isAdmin(principal)) throw new ForbiddenException();

        SprintService.clearErrors();
        Project selectedProject = projects.getProjectById(Integer.parseInt(projectID));
        Sprint sprint = sprints.getSprintByLabel(sprintLabel, selectedProject);

        Date endDate;
        Date startDate;
        try {
            //Get the dates set in the calendar controller
            startDate = new Date(formatter.parse(startDateString).getTime());
            //we need to subtract one day because of the way fullcalendar handles dates
            endDate = new Date(formatter.parse(endDateString).getTime() - 1);
        } catch (ParseException e) {
            SprintService.addNewError(new UserError("", "Invalid format passed into /api/updateSprintCalendar"));
            throw new DomainValidationException("Invalid format passed into /api/updateSprintCalendar");
        }

        //update sprint dates in the database
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);
        sprints.save(sprint);

        //List containing new end date
        return endDate;

    }

    /**
     * Api GET mapping for getting the user permissions.
     *
     * @param principal Authentication token.
     * @return A boolean value of if the user has elevated permissions or not.
     */
    @ResponseBody
    @GetMapping("/api/getUserPermissions")
    public Boolean getPermissions(@AuthenticationPrincipal AuthState principal) {
        return userService.isAdmin(principal);
    }
}
