package nz.ac.canterbury.seng302.portfolio.dto.advent;

import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MilestoneDTO {
    private int milestoneId;
    private int projectId;
    private String name;
    private Date startDate;

    public MilestoneDTO() {
    }

    public MilestoneDTO(Milestone milestone) {
        milestoneId = milestone.getMilestoneId();
        projectId = milestone.getProjectId();
        name = milestone.getName();
        startDate = milestone.getStartDate();
    }

    public int getMilestoneId() {
        return milestoneId;
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

    public String getStartDateISOString () {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(this.startDate);
    }

    public String getStartDateTimeString () {
        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy");
        return formatter.format(this.startDate);
    }
}
