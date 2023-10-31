package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.advent.AdventDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.service.AdventService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(AdventService.class), @MockBean(ProjectService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers= AdventController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {AdventController.class, AdventService.class, ProjectService.class})
class AdventControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProjectService projects;
    @Autowired
    private AdventService advents;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this); //Required for mocking to work
    }

    public List<AdventDTO> populatedAdventList () {
        ArrayList<AdventDTO> allAdvents = new ArrayList<>();
        allAdvents.add(new AdventDTO(new Event(1)));
        allAdvents.add(new AdventDTO(new Event(1)));
        allAdvents.add(new AdventDTO(new Deadline(1)));
        allAdvents.add(new AdventDTO(new Milestone(1)));
        return allAdvents;
    }

    @Test
    void getAdventsInRange_validProjectId_validDates () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/adventsRange?start=1645268400000&end=1654776000000");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        Mockito.when(advents.getAdventInRange(Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class)))
                .thenReturn(populatedAdventList());
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getAdventsInRange_validProjectId_validDates_noAdventsFound () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/adventsRange?start=1645268400000&end=1654776000000");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        Mockito.when(advents.getAdventInRange(Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class)))
                .thenReturn(new ArrayList<>());
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getAdventsInRange_validProjectId_validDates_serviceError () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/adventsRange?start=1645268400000&end=1654776000000");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        Mockito.when(advents.getAdventInRange(Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class)))
                .thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }


    @Test
    void getAdventsInRange_validProjectId_invalidDates () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/adventsRange?start=1645268400000&end=1623240000000");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAdventsInRange_validProjectId_noDates () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/adventsRange?start=1645268400000&end=1623240000000");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAdventsInRange_invalidProjectId () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/2/adventsRange?start=2022-02-20&end=2022-06-10");
        Mockito.when(projects.hasProject(1)).thenReturn(true);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }
}
