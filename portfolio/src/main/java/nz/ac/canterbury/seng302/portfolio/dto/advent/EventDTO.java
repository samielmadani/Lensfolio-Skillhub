package nz.ac.canterbury.seng302.portfolio.dto.advent;

import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EventDTO {
    private int eventId;
    private int projectId;
    private String name;
    private Date startDate;
    private Date endDate;
    private String startTime;
    private String endTime;

    public EventDTO() {
    }

    public EventDTO(Event event) {
        eventId = event.getEventId();
        projectId = event.getProjectId();
        name = event.getName();
        startDate = event.getStartDate();
        endDate = event.getEndDate();
        startTime = event.getStartTime();
        endTime = event.getEndTime();
    }

    public int getEventId() {
        return eventId;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public Date getStartDate() {
        if (startDate == null) return null;
        return new Date(startDate.getTime());
    }

    public Date getEndDate() {
        if (endDate == null) return null;
        return new Date(endDate.getTime());
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStartDateISOString () {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public String getEndDateISOString () {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.endDate);
    }

    public String getStartDateTimeString () {
        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy");
        return formatter.format(this.startDate) + " " + DateUtil.convertTo12HourTime(this.startTime);
    }

    public String getEndDateTimeString () {
        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy");
        return formatter.format(this.endDate) + " " + DateUtil.convertTo12HourTime(this.endTime);
    }
}
