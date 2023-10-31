package nz.ac.canterbury.seng302.portfolio.service.validators;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class EventValidatorTests {
    @BeforeEach
    public void resetValidator () {
        EventService.clearErrors();
    }

    @Test
    public void validateEvent_validDates_validName () {
        Event event = new Event(1);
        EventValidator.validateEvent(event);
        assertFalse(EventService.hasErrors());
    }

    @Test
    public void validateEvent_validDates_invalidName () {
        Event event = new Event (1);
        event.setName("");
        EventValidator.validateEvent(event);
        assertTrue(EventService.hasErrors());
    }

    @Test
    public void validateEvent_invalidDates_validName () {
        Event event = new Event (1);
        Date eventStartDate = event.getStartDate();
        event.setStartDate(event.getEndDate());
        event.setEndDate(eventStartDate);
        EventValidator.validateEvent(event);
        assertTrue(EventService.hasErrors());
        assertEquals(1, EventService.getCurrentErrors().size());
    }

    @Test
    public void validateEvent_invalidDates_invalidName () {
        Event event = new Event (1);
        Date eventStartDate = event.getStartDate();
        event.setStartDate(event.getEndDate());
        event.setEndDate(eventStartDate);
        event.setName("");
        EventValidator.validateEvent(event);
        assertTrue(EventService.hasErrors());
        assertEquals(2, EventService.getCurrentErrors().size());
    }
}
