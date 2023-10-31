package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.model.repositories.MilestoneRepository;
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
class MilestoneServiceTest {
    @InjectMocks
    private MilestoneService milestoneService;
    @Mock
    private MilestoneRepository milestoneRepository;

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
    void getMilestoneById_milestoneExists () {
        Mockito.when(milestoneRepository.findById(1)).thenReturn(new Milestone(1));
        Assertions.assertNotNull(milestoneService.getMilestoneById(1));
    }

    @Test
    void getMilestoneById_milestoneDoesntExist () {
        Mockito.when(milestoneRepository.findById(0)).thenReturn(null);
        Assertions.assertNull(milestoneService.getMilestoneById(0));
    }

    @Test
    void getMilestonesForProject_projectExists_noMilestones () {
        Mockito.when(milestoneRepository.findByProjectId(1)).thenReturn(new ArrayList<>());
        assertEquals(0, milestoneService.getMilestonesForProject(1).size());
    }

    @Test
    void getMilestonesForProject_projectExists_containingMilestones () {
        ArrayList<Milestone> milestones = new ArrayList<>();
        milestones.add(new Milestone(1));
        milestones.add(new Milestone(1));
        milestones.add(new Milestone(1));

        Mockito.when(milestoneRepository.findByProjectId(1))
                .thenReturn(milestones);

        assertEquals(3, milestoneService.getMilestonesForProject(1).size());
    }

    @Test
    void saveMilestone_validMilestoneData () {
        Mockito.when(milestoneRepository.save(Mockito.any(Milestone.class)))
                .thenReturn(new Milestone(1));
        Assertions.assertNotNull(milestoneService.save(new Milestone(1)));
    }

    @Test
    void getMilestonesInRange_noMilestones () {
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(milestoneRepository.findByProjectId(1))
                .thenReturn(new ArrayList<>());
        assertEquals(0, milestoneService.getMilestonesInRange(1, startDate, endDate).size());
    }

    @Test
    void getMilestonesInRange_containingMilestones () {
        ArrayList<Milestone> milestonesInDB = new ArrayList<>();
        milestonesInDB.add(new Milestone(1));

        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(milestoneRepository.findByProjectId(1))
                .thenReturn(milestonesInDB);
        assertEquals(1, milestoneService.getMilestonesInRange(1, startDate, endDate).size());
    }

    @Test
    void getMilestonesInRange_milestoneOutOfRange () {
        ArrayList<Milestone> milestonesInDB = new ArrayList<>();
        milestonesInDB.add(new Milestone(1));
        cal.add(Calendar.WEEK_OF_YEAR, 5);
        Date startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 10);
        Date endDate = new Date(cal.getTimeInMillis());

        Mockito.when(milestoneRepository.findByProjectId(1))
                .thenReturn(milestonesInDB);
        assertEquals(0, milestoneService.getMilestonesInRange(1, startDate, endDate).size());
    }
}
