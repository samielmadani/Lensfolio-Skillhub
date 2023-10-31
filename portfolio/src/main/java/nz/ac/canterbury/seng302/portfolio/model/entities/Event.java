package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.dto.advent.EventDTO;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Entity(name = "Event")
public class Event implements Comparable<Event>{

    @Id
    @Column(name="event_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int eventId;

    @Column(nullable = false)
    private int projectId;

    private String name;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date endDate;

    private String startTime;

    private String endTime;

    public Event(int projectId) {
        this.projectId = projectId;
        this.name = "Default Event";

        //Creates new instance of calendar that stores the users local time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        this.startDate = new Date(cal.getTimeInMillis());
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        this.endDate = new Date(cal.getTimeInMillis());
        this.startTime = "00:00";
        this.endTime = "00:00";
    }

    protected Event() {}

    public Event updateUsingDTO (EventDTO eventDTO) {
        if (eventDTO.getName() != null) setName(eventDTO.getName());
        if (eventDTO.getStartDate() != null) setStartDate(eventDTO.getStartDate());
        if (eventDTO.getEndDate() != null) setEndDate(eventDTO.getEndDate());
        if (eventDTO.getStartTime() != null) setStartTime(eventDTO.getStartTime());
        if (eventDTO.getEndTime() != null) setEndTime(eventDTO.getEndTime());
        return this;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEditId () {return "event" + eventId;}

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

    public String getEndTime() {return endTime;}

    public void setStartTime (String startTime) {this.startTime = startTime;}

    public void setEndTime (String endTime) {this.endTime = endTime;}

    public String getStartDateIsoString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public String getEndDateIsoString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.endDate);
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String toString () {
        String startDateString = DateUtil.dateToFormattedString(startDate) + " (" + DateUtil.convertTo12HourTime(startTime) + ")";
        String endDateString = DateUtil.dateToFormattedString(endDate) + " (" + DateUtil.convertTo12HourTime(endTime) + ")";

        return startDateString + " - " + endDateString;
    }

    @Override
    public int compareTo(Event o) {
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
        if (!(o instanceof Event)) return false;
        return name.equals(((Event) o).getName())
                && projectId == ((Event) o).getProjectId()
                && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return eventId * projectId * name.hashCode();
    }
}
