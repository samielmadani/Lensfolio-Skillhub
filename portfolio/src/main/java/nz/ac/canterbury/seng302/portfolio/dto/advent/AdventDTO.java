package nz.ac.canterbury.seng302.portfolio.dto.advent;

import nz.ac.canterbury.seng302.portfolio.model.entities.Deadline;
import nz.ac.canterbury.seng302.portfolio.model.entities.Event;
import nz.ac.canterbury.seng302.portfolio.model.entities.Milestone;
import nz.ac.canterbury.seng302.portfolio.util.DateUtil;

import java.util.Date;

public class AdventDTO implements Comparable<AdventDTO>{
    private int adventId;
    private Date date;
    private String name;
    private String type;
    private String startDate;
    private String endDate;

    public AdventDTO () {}

    public AdventDTO (Deadline deadline) {
        adventId = deadline.getDeadlineId();
        date = DateUtil.combineDateAndTime(deadline.getStartDate(), deadline.getStartTime());
        name = deadline.getName();
        startDate = DateUtil.dateToMonthString(deadline.getStartDate());
        type = "deadline";
    }

    public AdventDTO (Event event) {
        adventId = event.getEventId();
        date = DateUtil.combineDateAndTime(event.getStartDate(), event.getStartTime());
        startDate = DateUtil.dateToMonthString(event.getStartDate());
        endDate = DateUtil.dateToMonthString(event.getEndDate());
        name = event.getName();
        type = "event";
    }

    public AdventDTO (Milestone milestone) {
        adventId = milestone.getMilestoneId ();
        date = milestone.getStartDate();
        name = milestone.getName();
        startDate = DateUtil.dateToMonthString(milestone.getStartDate());
        endDate = "";
        type = "milestone";
    }

    public int getAdventId() {
        return adventId;
    }

    public void setAdventId(int adventId) {
        this.adventId = adventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate () {return this.date;}

    public void setDate (Date date) {this.date = date;}

    public String getStartDate () {return this.startDate;}

    public void setStartDate (String startDate) {this.startDate = startDate;}

    public String getEndDate () {return this.endDate;}

    public void setEndDate (String endDate) {this.endDate = endDate;}



    @Override
    public int compareTo (AdventDTO a) {
        if (date.after(a.getDate())) return 1;
        if (date.before(a.getDate())) return -1;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof AdventDTO)) return false;
        return ((AdventDTO) obj).getAdventId() == adventId;
    }
}
