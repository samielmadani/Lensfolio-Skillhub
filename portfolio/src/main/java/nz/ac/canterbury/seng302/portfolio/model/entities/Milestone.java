package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.dto.advent.MilestoneDTO;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Entity(name = "Milestone")
public class Milestone implements Comparable<Milestone>{

    @Id
    @Column(name="milestone_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int milestoneId;

    @Column(nullable = false)
    private int projectId;

    private String name;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date startDate;

    public Milestone(int projectId) {
        this.projectId = projectId;
        this.name = "Default Milestone";

        //Creates new instance of calendar that stores the users local time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        this.startDate = new Date(cal.getTimeInMillis());
    }

    protected Milestone() {}

    public Milestone updateUsingDTO (MilestoneDTO milestoneDTO) {
        if (milestoneDTO.getName() != null) setName(milestoneDTO.getName());
        if (milestoneDTO.getStartDate() != null) setStartDate(milestoneDTO.getStartDate());
        return this;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public String getEditId () {return "Milestone" + milestoneId;}

    public int getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getStartDateIsoString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String toString () {
        String startDateString = DateUtil.dateToFormattedString(startDate);

        return startDateString;
    }

    @Override
    public int compareTo(Milestone o) {
        //Compare to other milestones start date
        if (this.startDate.before(o.startDate)) {
            return -1;
        }

        if (this.startDate.after(o.startDate)) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Milestone)) return false;
        return name.equals(((Milestone) o).getName())
                && projectId == ((Milestone) o).getProjectId()
                && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return milestoneId * projectId * name.hashCode();
    }
}
