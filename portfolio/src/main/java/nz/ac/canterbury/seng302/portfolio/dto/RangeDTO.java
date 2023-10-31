package nz.ac.canterbury.seng302.portfolio.dto;

import java.util.Date;

public class RangeDTO {
    private Date start;
    private Date end;
    private String location;

    public RangeDTO () {}

    public RangeDTO (Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public RangeDTO (Date start, Date end, String location) {
        this.start = start;
        this.end = end;
        this.location = location;
    }

    public Date getStart () {return this.start;}
    public Date getEnd () {return this.end;}
    public void setStart (Date start) {this.start = start;}
    public void setEnd (Date end) {this.end = end;}
    public String getLocation () {return this.location;}
    public void setLocation (String location) {this.location = location;}

}
