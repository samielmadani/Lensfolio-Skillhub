package nz.ac.canterbury.seng302.portfolio.model.entities;

import nz.ac.canterbury.seng302.portfolio.service.validators.ProjectValidator;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity(name = "Project") // this is an entity, assumed to be in a table called Project
public class Project implements Serializable {
    @Id
    @Column(name = "project_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int projectId;

    private String name;
    private String description;

    // Boolean value to identify the default project. Only one project should ever have this set to true (managed in the services)
    private Boolean isDefaultProject;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date startDate;

    @DateTimeFormat(pattern = "yyy-MM-dd")
    private Date endDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private List<Sprint> sprints;

    public Project() {
        //Creates new instance of calendar that stores the users local time
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUtil.stripTimeFromDate(cal.getTime()));

        //Ensures default format is "Project <current year>", e.g. "Project 2022"
        name = "Project " + cal.get(Calendar.YEAR);
        description = name;
        startDate = cal.getTime(); //Creates a new SQL date at the current date
        cal.add(Calendar.MONTH, 8); //Moves the user calendar object forward by 8 months, as required by default
        endDate = cal.getTime(); //Creates a new SQL date at the new calendar date
        ProjectValidator.validateDates(this);
    }

    public Project(String projectName, String projectDescription, Date projectStartDate, Date projectEndDate) {
        this.name = projectName;
        this.description = projectDescription;
        this.startDate = projectStartDate;
        this.endDate = projectEndDate;
    }

    public Project(String projectName, String projectDescription, String projectStartDate, String projectEndDate) {
        this.name = projectName;
        this.description = projectDescription;
        this.startDate = DateUtil.stringToDate(projectStartDate);
        this.endDate = DateUtil.stringToDate(projectEndDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return projectId == project.projectId && Objects.equals(name, project.name) && Objects.equals(description, project.description) && Objects.equals(isDefaultProject, project.isDefaultProject) && Objects.equals(startDate, project.startDate) && Objects.equals(endDate, project.endDate) && Objects.equals(sprints, project.sprints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, name, description, isDefaultProject, startDate, endDate, sprints);
    }

    @Override
    public String toString() {
        return String.format(
                "Project[id=%d, projectName='%s', projectStartDate='%s', projectEndDate='%s', projectDescription='%s', isDefaultProject='%s']",
                projectId, name, startDate, endDate, description, isDefaultProject);
    }

    /* Getters/Setters */

    public int getId(){
        return  projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Set this project as the sole default
     */
    public void setAsDefaultProject() {
        this.isDefaultProject = true;
    }

    public boolean getIsDefaultProject() {
        return this.isDefaultProject;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getStartDateString() {
        return DateUtil.dateToMonthString(this.startDate);
    }

    public String getStartDateIsoString() {
        return DateUtil.dateToISOString(this.startDate);
    }

    public String getFormattedStartDate () {return DateUtil.dateToFormattedString(startDate);}

    public void setStartDate(Date newStartDate) {
        this.startDate = newStartDate;
    }

    public void setStartDateString(String date) {
        this.startDate = DateUtil.stringToDate(date);
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getEndDateString() {
        return DateUtil.dateToMonthString(this.endDate);
    }

    public String getEndDateIsoString() {
        return DateUtil.dateToISOString(this.endDate);
    }

    public String getFormattedEndDate () {return DateUtil.dateToFormattedString(endDate);}

    public void setEndDate(Date newEndDate) {
        this.endDate = newEndDate;
    }

    public void setEndDateString(String date) {
        this.endDate = DateUtil.stringToDate(date);
    }

    public List<Sprint> getSprints() {
        return sprints;
    }
}
