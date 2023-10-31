package nz.ac.canterbury.seng302.portfolio.dto;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;

import java.util.Date;

public class ProjectDTO {
    private int id;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;

    public ProjectDTO () {}

    public ProjectDTO (Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.startDate = project.getStartDate();
        this.endDate = project.getEndDate();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
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
}
