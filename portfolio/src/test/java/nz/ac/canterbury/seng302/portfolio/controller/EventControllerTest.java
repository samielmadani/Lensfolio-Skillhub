package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import nz.ac.canterbury.seng302.portfolio.service.ProjectService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.ClaimDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(EventService.class), @MockBean(UserService.class), @MockBean(ProjectService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers= EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {EventController.class, EventService.class, UserService.class, ProjectService.class})
class EventControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    EventService eventService;

    @Autowired
    UserService userService;

    @Autowired
    ProjectService projectService;

    private final Event defaultParamsEvent = new Event (1);
    private final Event customParamsEventFull = new Event(1);
    private final Event customParamsEventPartial = new Event(1);


    /**
     * Helper function to create a custom AuthState used for testing with full authorisation
     * @param id ID of the user to create
     * @return valid AuthState to pass in as AuthenticationToken
     */
    private AuthState createAuthState (String id) {
        return AuthState.newBuilder()
                .setIsAuthenticated(true)
                .setNameClaimType("name")
                .setRoleClaimType("role")
                .addClaims(ClaimDTO.newBuilder().setType("role").setValue("STUDENT").build())
                .addClaims(ClaimDTO.newBuilder().setType("nameid").setValue(id).build())
                .build();
    }

    /**
     * Creates a Mocked SecurityContext override default behaviour of the EventController built-in spring security
     * This allows api requests to be mocked as if the user making the request is an admin
     */
    private void runTestAsAdmin() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        Mockito.when(userService.isAdmin(authState))
                .thenReturn(true);
    }

    /**
     * Creates a Mocked SecurityContext override default behaviour of the EventController built-in spring security
     * This allows api requests to be mocked as if the user making the request is not an admin
     */
    private void runTestAsStudent() {
        AuthState authState = createAuthState("1");
        SecurityContext mockedSecurityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(mockedSecurityContext.getAuthentication())
                .thenReturn(new PreAuthenticatedAuthenticationToken(authState, "")); //Assume security context is OK
        SecurityContextHolder.setContext(mockedSecurityContext); //Set the context for the upcoming test

        Mockito.when(userService.isAdmin(authState))
                .thenReturn(false);
    }

    /**
     * Generates custom values for different types of events that are required in testing
     */
    private void populateEventInformation() {
        customParamsEventFull.setName("Custom Event");
        Calendar startCalendar = new GregorianCalendar(2022, Calendar.MARCH, 20);
        customParamsEventFull.setStartDate(new Date(startCalendar.getTimeInMillis()));
        Calendar endCalendar = new GregorianCalendar(2022, Calendar.APRIL, 20);
        customParamsEventFull.setEndDate(new Date(endCalendar.getTimeInMillis()));
        customParamsEventFull.setStartTime("12:00");
        customParamsEventFull.setEndTime("12:30");

        customParamsEventPartial.setName("My New Event");
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this); //Required for mocking to work
        populateEventInformation();

    }

    /**
     * Makes an API request to get all the events for a project, assumes events already exist for the project
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void getEvents_validProjectId_containingEvents() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events");
        Mockito.when(eventService.getEventsForProject(1))
                .thenReturn(new ArrayList<>(List.of(defaultParamsEvent)));

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    /**
     * Makes an API request to get all the events for a project that doesn't exist
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void getEvents_invalidProjectId () throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events");
        Mockito.when(eventService.getEventsForProject(1))
                .thenReturn(null); //mock behaviour of EventService.java when the projectId doesn't exist

        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API request to get all the events for a project that has no events
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void getEvents_validProjectId_containingNoEvents () throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events");
        Mockito.when(eventService.getEventsForProject(1))
                .thenReturn(new ArrayList<>(List.of()));

        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getEventsJSON_validProjectId_containingNoEvents() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/eventsJSON");
        Mockito.when(eventService.getEventsForProject(1)).thenReturn(new ArrayList<>(List.of()));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void getEventsJSON_validProjectId_containingEvents() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/eventsJSON");
        Mockito.when(eventService.getEventsForProject(1)).thenReturn(new ArrayList<>(List.of(customParamsEventFull)));
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[{}]")); //expect only one returned
    }

    @Test
    void getEventsJSON_invalidProjectId() throws Exception {
        runTestAsStudent();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/eventsJSON");
        Mockito.when(eventService.getEventsForProject(1)).thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API get request to get the edit template for a valid event
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_validProjectID_validEventId () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events/1/edit");
        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(new Project());

        mvc.perform(request)
                .andExpect(status().isOk());
    }


    /**
     * Makes an API get request to get the edit template for an invalid event
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_validProjectId_eventDoesntExist () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events/1/edit");
        Mockito.when(eventService.getEventById(1))
                .thenReturn(null);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(new Project());
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }


    /**
     * Makes an API get request to get the edit template for an event in a project that doesn't exist
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_validAuthentication_invalidProjectId_validEventID () throws Exception {
        runTestAsAdmin();
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events/1/edit");
        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(projectService.getProjectById(1))
                .thenReturn(null);
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }


    /**
     * Makes an API get request to get the edit template for a valid event but with invalid authentication
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events/1/edit");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }


    /**
     * Makes an API get request to get the edit template for a valid event but with no authentication
     * @throws Exception custom HTTP exception thrown
     */
    @Test
    void getEditTemplate_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/project/1/events/1/edit");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }


    /**
     * Makes an API post request to create an event with valid data, but not information passed in for the event info
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_validAuthentication_validProjectId_emptyBody () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(eventService.save(any()))
                .thenReturn(defaultParamsEvent);


        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"); //no data passed in
        mvc.perform(request)
                .andExpect(status().isCreated());
    }

    /**
     * Makes an API post request to create an event but no authentication is provided
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().is4xxClientError());
    }

    /**
     * Makes an API post request to create an event with valid authentication but the user is not authorised to
     * make this request
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_invalidAuthentication () throws Exception {
        runTestAsStudent(); //Students aren't authorised to create events

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Makes an API post request to create an event with valid authentication but invalid projectId
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(eventService.save(any()))
                .thenReturn(null); //Mocks what happens if the project doesn't exist in the database

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Makes an API post request to create an event with valid authentication and a valid projectId with all possible
     * custom values for the event to create passed in
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_validAuthentication_validProjectId_fullBodyContent () throws Exception {
        runTestAsAdmin(); //post request requires the user to be an admin

        Mockito.when(eventService.save(any()))
                .thenReturn(customParamsEventFull);

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"Custom Event\", \"startDate\": \"2022-03-20T00:00+12:00\", \"endDate\": \"2022-04-20T00:00+12:00\", \"startTime\": \"12:00\", \"endTime\": \"12:30\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventService).save(eventCaptor.capture());
        //Tests the datetime values were set to the custom values passed in
        Assertions.assertEquals(customParamsEventFull.toString(), eventCaptor.getValue().toString());
    }

    /**
     * Makes an API post request to create an event with valid authentication and a valid projectId with some
     * custom values for the event to create passed in
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void createEvent_validAuthentication_validProjectId_partialBodyContent () throws Exception {
        runTestAsAdmin(); //post request requires user to be admin

        Mockito.when(eventService.save(any()))
                .thenReturn(customParamsEventPartial);

        RequestBuilder request = MockMvcRequestBuilders.post("/api/project/1/events")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"My New Event\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventService).save(eventCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsEventPartial.toString(), eventCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing event testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_validAuthentication_validProjectId_eventExists_emptyBody () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.save(any()))
                .thenReturn(defaultParamsEvent);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventService).save(eventCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(defaultParamsEvent.toString(), eventCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing event testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_validAuthentication_validProjectId_eventExists_fullBodyContent () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.save(any()))
                .thenReturn(customParamsEventFull);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"Custom Event\", \"startDate\": \"2022-03-20T00:00+12:00\", \"endDate\": \"2022-04-20T00:00+12:00\", \"startTime\": \"12:00\", \"endTime\": \"12:30\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventService).save(eventCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsEventFull.toString(), eventCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating an existing event testing expected normal behaviour of the endpoint
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_validAuthentication_validProjectId_eventExists_partialBodyContent () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.save(any()))
                .thenReturn(customParamsEventPartial);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1, \"name\": \"My New Event\"}");
        mvc.perform(request)
                .andExpect(status().isCreated());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventService).save(eventCaptor.capture());
        //Test that nothing except what was passed in from the request body was changed
        Assertions.assertEquals(customParamsEventPartial.toString(), eventCaptor.getValue().toString());
    }

    /**
     * Tests an API put request for updating a non-existent event
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_validAuthentication_validProjectId_eventDoesntExist () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests an API put request for updating an event for a non-existent projectId
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.save(any()))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests an API put request for updating an event with no authentication provided
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests an API put request for updating an event with invalid authentication provided
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void updateEvent_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.put("/api/project/1/events/1")
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": 1}");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    /**
     * Tests the API delete request for deleting an event with valid information
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void deleteEvent_validAuthentication_validProjectId_validEventId () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.delete(any()))
                .thenReturn(true);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/events/1");
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    /**
     * Tests the API delete request for a non-existent event
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void deleteEvent_validAuthentication_validProjectId_invalidEventId () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(null);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/events/1");
        mvc.perform(request)
                .andExpect(status().isNotFound());
    }

    /**
     * Tests the API delete request for a non-existent project
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void deleteEvent_validAuthentication_invalidProjectId () throws Exception {
        runTestAsAdmin();

        Mockito.when(eventService.getEventById(1))
                .thenReturn(defaultParamsEvent);
        Mockito.when(eventService.delete(any()))
                .thenReturn(false);

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/events/1");
        mvc.perform(request)
                .andExpect(status().isInternalServerError());
    }

    /**
     * Tests the API delete request with no authentication passed in
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void deleteEvent_noAuthentication () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/events/1");
        mvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests the API delete request with invalid authentication passed in
     * @throws Exception custom HTTP exception thrown, details can be found in EventsExceptionAdvice.java
     */
    @Test
    void deleteEvent_invalidAuthentication () throws Exception {
        runTestAsStudent();

        RequestBuilder request = MockMvcRequestBuilders.delete("/api/project/1/events/1");
        mvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
}
