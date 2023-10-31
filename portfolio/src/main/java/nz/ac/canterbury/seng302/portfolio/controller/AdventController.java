package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.advent.AdventDTO;
import nz.ac.canterbury.seng302.portfolio.exceptions.DomainValidationException;
import nz.ac.canterbury.seng302.portfolio.exceptions.InternalServerErrorException;
import nz.ac.canterbury.seng302.portfolio.exceptions.ProjectNotFoundException;
import nz.ac.canterbury.seng302.portfolio.service.AdventService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Date;
import java.util.List;

@Controller
public class AdventController {

    @Autowired
    private ProjectService projects;
    @Autowired
    private AdventService advents;


    private final Logger logger = LoggerFactory.getLogger(AdventController.class);


    /**
     * Gets all advents in the range of the start-end date passed in for a certain project. This will return a sorted
     * list represented by an HTML fragment of all the inline advents in the range for the project
     * @param projectId ID of the project to get all the advents in range for
     * @param startString String representation of the start date in the range
     * @param endString String representation of the end date in the range
     * @param model HTML DOM model to add attributes to returning HTML fragment
     * @return HTML fragment of all inline advents in the range for the project
     */
    @GetMapping("api/project/{projectId}/adventsRange")
    @ResponseStatus(HttpStatus.OK)
    public String getAdventsInRange (@PathVariable int projectId, @RequestParam("start") String startString, @RequestParam("end") String endString, Model model) {
        //Sanitize inputs
        startString = startString.replaceAll("[\n\r\t]", "_");
        endString = endString.replaceAll("[\n\r\t]", "_");
        logger.info("Getting all advents in range {} - {} for project {}", startString, endString, projectId);

        if (!projects.hasProject(projectId)) {
            logger.info("Tried to get all advents in range for project {} but the project couldn't be found", projectId);
            throw new ProjectNotFoundException(projectId);
        }

        Date start = new Date();
        start.setTime(Long.parseLong(startString));
        Date end = new Date();
        end.setTime(Long.parseLong(endString));

        if (start.after(end)) {
            logger.info("Tried to get all the advents in a range, but the range start was after the end!");
            throw new DomainValidationException("Range start occurs after Range end :(");
        }
        List<AdventDTO> allAdvents = advents.getAdventInRange(projectId, start, end);
        if (allAdvents == null) {
            logger.info("Tried to get all the advents in project {} for the range {} - {} but something went wrong.", projectId, start, end);
            throw new InternalServerErrorException();
        }
        logger.info("Got {} advents for project {} in the range {} - {}", allAdvents.size(), projectId, start, end);
        model.addAttribute("advents", allAdvents);
        return "advents/adventLineDisplayCreator";
    }
}
