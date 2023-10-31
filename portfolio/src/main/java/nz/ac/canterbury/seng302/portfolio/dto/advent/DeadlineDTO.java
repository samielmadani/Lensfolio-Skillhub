package nz.ac.canterbury.seng302.portfolio.dto.advent;

import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DeadlineDTO {
    private int deadlineId;
    private int projectId;
    private String name;
    private Date startDate;
    private String startTime;

    public DeadlineDTO() {
    }

    public DeadlineDTO(Deadline deadline) {
        deadlineId = deadline.getDeadlineId();
        projectId = deadline.getProjectId();
        name = deadline.getName();
        startDate = deadline.getStartDate();
        startTime = deadline.getStartTime();
    }

    public int getDeadlineId() {
        return deadlineId;
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

    public String getStartTime() {
        return startTime;
    }

    public String getStartDateISOString () {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public String getStartDateTimeString () {
        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy");
        return formatter.format(this.startDate) + " " + DateUtil.convertTo12HourTime(this.startTime);
    }
}
