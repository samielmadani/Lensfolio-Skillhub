package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.dto.advent.DeadlineDTO;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Entity(name = "Deadline")
public class Deadline implements Comparable<Deadline>{

    @Id
    @Column(name="deadline_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int deadlineId;

    @Column(nullable = false)
    private int projectId;

    private String name;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date startDate;

    private String startTime;

    public Deadline(int projectId) {
        this.projectId = projectId;
        this.name = "Default Deadline";

        //Creates new instance of calendar that stores the users local time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        this.startDate = new Date(cal.getTimeInMillis());
        this.startTime = "00:00";
    }

    protected Deadline() {}

    public Deadline updateUsingDTO (DeadlineDTO deadlineDTO) {
        if (deadlineDTO.getName() != null) setName(deadlineDTO.getName());
        if (deadlineDTO.getStartDate() != null) setStartDate(deadlineDTO.getStartDate());
        if (deadlineDTO.getStartTime() != null) setStartTime(deadlineDTO.getStartTime());
        return this;
    }

    public int getDeadlineId() {
        return deadlineId;
    }

    public String getEditId () {return "event" + deadlineId;}

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

    public String getStartTime() {return startTime;}

    public void setStartTime (String startTime) {this.startTime = startTime;}

    public String getStartDateIsoString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String toString () {
        return DateUtil.dateToFormattedString(startDate) + " (" + DateUtil.convertTo12HourTime(startTime) + ")";
    }

    @Override
    public int compareTo(Deadline o) {
        //Compare to other events start date
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
        if (!(o instanceof Deadline)) return false;
        return name.equals(((Deadline) o).getName())
                && projectId == ((Deadline) o).getProjectId()
                && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return deadlineId * projectId * name.hashCode();
    }
}
