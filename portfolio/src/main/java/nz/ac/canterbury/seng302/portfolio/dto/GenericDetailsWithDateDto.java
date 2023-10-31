package nz.ac.canterbury.seng302.portfolio.dto;

public class GenericDetailsWithDateDto {
    public GenericDetailsWithDateDto(String name, String description, String startDateString, String endDateString) {
        this.name = name;
        this.description = description;
        this.startDateString = startDateString;
        this.endDateString = endDateString;
    }

    private String name;

    private String description;

    private String startDateString;

    private String endDateString;

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

    public String getStartDateString() {
        return startDateString;
    }

    public void setStartDateString(String startDateString) {
        this.startDateString = startDateString;
    }

    public String getEndDateString() {
        return endDateString;
    }

    public void setEndDateString(String endDateString) {
        this.endDateString = endDateString;
    }
}
