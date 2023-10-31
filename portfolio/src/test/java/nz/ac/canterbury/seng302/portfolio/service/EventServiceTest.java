package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.repositories.EventRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @InjectMocks
    private EventService eventService;
    @Mock
    private EventRepository eventRepository;

    Calendar cal;

    @BeforeEach
    public void reset () {
        cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    @Test
    void getEventById_eventExists () {
        Mockito.when(eventRepository.findById(1)).thenReturn(new Event(1));
        Assertions.assertNotNull(eventService.getEventById(1));
    }

    @Test
    void getEventById_eventDoesntExist () {
        Mockito.when(eventRepository.findById(0)).thenReturn(null);
        Assertions.assertNull(eventService.getEventById(0));
    }

    @Test
    void getEventsForProject_projectExists_noEvents () {
        Mockito.when(eventRepository.findByProjectId(1)).thenReturn(new ArrayList<>());
        assertEquals(0, eventService.getEventsForProject(1).size());
    }

    @Test
    void getEventsForProject_projectExists_containingEvents () {
        ArrayList<Event> events = new ArrayList<>();
        events.add(new Event(1));
        events.add(new Event(1));
        events.add(new Event(1));

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(events);

        assertEquals(3, eventService.getEventsForProject(1).size());
    }

    @Test
    void saveEvent_validEventData () {
        Mockito.when(eventRepository.save(Mockito.any(Event.class)))
                .thenReturn(new Event(1));
        Assertions.assertNotNull(eventService.save(new Event(1)));
    }

    @Test
    void saveEvent_invalidEventData () {
        Event invalidEvent = new Event(1);
        Date eventStartDate = invalidEvent.getStartDate();
        invalidEvent.setStartDate(invalidEvent.getEndDate());
        invalidEvent.setEndDate(eventStartDate);
        Assertions.assertNull(eventService.save(invalidEvent));
    }

    @Test
    void getEventsInRange_noEvents () {
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(new ArrayList<>());
        assertEquals(0, eventService.getEventsInRange(1, startDate, endDate).size());
    }

    @Test
    void getEventsInRange_containingEvents () {
        ArrayList<Event> eventsInDB = new ArrayList<>();
        eventsInDB.add(new Event(1));

        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(eventsInDB);
        assertEquals(1, eventService.getEventsInRange(1, startDate, endDate).size());
    }

    @Test
    void getEventsInRange_eventOutOfRange () {
        ArrayList<Event> eventsInDB = new ArrayList<>();
        eventsInDB.add(new Event(1));
        cal.add(Calendar.WEEK_OF_YEAR, 5);
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(eventsInDB);
        assertEquals(0, eventService.getEventsInRange(1, startDate, endDate).size());
    }

    @Test
    void getEventsInRange_eventStartOutOfRange () {
        ArrayList<Event> eventsInDB = new ArrayList<>();
        eventsInDB.add(new Event(1));
        cal.add(Calendar.DAY_OF_MONTH, 5);
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(eventsInDB);
        assertEquals(1, eventService.getEventsInRange(1, startDate, endDate).size());
    }

    @Test
    void getEventsInRange_eventEndOutOfRange () {
        ArrayList<Event> eventsInDB = new ArrayList<>();
        eventsInDB.add(new Event(1));
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 52);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(eventRepository.findByProjectId(1))
                .thenReturn(eventsInDB);
        assertEquals(1, eventService.getEventsInRange(1, startDate, endDate).size());
    }
}
