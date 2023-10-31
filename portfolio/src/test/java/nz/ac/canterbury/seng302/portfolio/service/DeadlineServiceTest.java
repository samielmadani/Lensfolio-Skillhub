package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.repositories.DeadlineRepository;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class DeadlineServiceTest {
    @InjectMocks
    private DeadlineService deadlineService;
    @Mock
    private DeadlineRepository deadlineRepository;

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
    void getDeadlineById_deadlineExists () {
        Mockito.when(deadlineRepository.findById(1)).thenReturn(new Deadline(1));
        Assertions.assertNotNull(deadlineService.getDeadlineById(1));
    }

    @Test
    void getDeadlineById_deadlineDoesntExist () {
        Mockito.when(deadlineRepository.findById(0)).thenReturn(null);
        Assertions.assertNull(deadlineService.getDeadlineById(0));
    }

    @Test
    void getDeadlinesForProject_projectExists_noDeadlines () {
        Mockito.when(deadlineRepository.findByProjectId(1)).thenReturn(new ArrayList<>());
        assertEquals(0, deadlineService.getDeadlinesForProject(1).size());
    }

    @Test
    void getDeadlinesForProject_projectExists_containingDeadlines () {
        ArrayList<Deadline> deadlines = new ArrayList<>();
        deadlines.add(new Deadline(1));
        deadlines.add(new Deadline(1));
        deadlines.add(new Deadline(1));

        Mockito.when(deadlineRepository.findByProjectId(1))
                .thenReturn(deadlines);

        assertEquals(3, deadlineService.getDeadlinesForProject(1).size());
    }

    @Test
    void saveDeadline_validDeadlineData () {
        Mockito.when(deadlineRepository.save(Mockito.any(Deadline.class)))
                .thenReturn(new Deadline(1));
        Assertions.assertNotNull(deadlineService.save(new Deadline(1)));
    }

    @Test
    void getDeadlinesInRange_noDeadlines () {
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(deadlineRepository.findByProjectId(1))
                .thenReturn(new ArrayList<>());
        assertEquals(0, deadlineService.getDeadlinesInRange(1, startDate, endDate).size());
    }

    @Test
    void getDeadlinesInRange_containingDeadlines () {
        ArrayList<Deadline> deadlinesInDB = new ArrayList<>();
        deadlinesInDB.add(new Deadline(1));

        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(deadlineRepository.findByProjectId(1))
                .thenReturn(deadlinesInDB);
        assertEquals(1, deadlineService.getDeadlinesInRange(1, startDate, endDate).size());
    }

    @Test
    void getDeadlinesInRange_deadlineOutOfRange () {
        ArrayList<Deadline> deadlinesInDB = new ArrayList<>();
        deadlinesInDB.add(new Deadline(1));
        cal.add(Calendar.WEEK_OF_YEAR, 5);
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(deadlineRepository.findByProjectId(1))
                .thenReturn(deadlinesInDB);
        assertEquals(0, deadlineService.getDeadlinesInRange(1, startDate, endDate).size());
    }
}
