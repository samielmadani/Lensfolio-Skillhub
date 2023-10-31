package nz.ac.canterbury.seng302.portfolio.dto.evidence;

import java.util.*;

public class EvidenceDTO {
    private String name;
    private String date;
    private String description;
    private List<String> webLinks;
    private List<String> categories;
    private List<Integer> linkedUsers;
    private Set<String> skills;

    private List<CommitDTO> commits;

    public EvidenceDTO(String name, String date, String description) {
        this.name = name;
        this.date = date;
        this.description = description;
    }

    public EvidenceDTO(String name, String date, String description, List<String> categories) {
        this.name = name;
        this.date = date;
        this.description = description;
        this.categories = categories;
    }

    public EvidenceDTO(String name, String date, String description, List<String> webLinks, Set<String> skills, List<CommitDTO> commits) {
        this.name = name;
        this.date = date;
        this.description = description;
        this.webLinks = webLinks;
        this.skills = skills;
        this.commits = commits;
    }

    public EvidenceDTO () {}

    public List<CommitDTO> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitDTO> commits) {
        this.commits = commits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWebLinks (List<String> webLinks) { this.webLinks = webLinks; }

    public List<String> getWebLinks () { return this.webLinks; }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    public List<String> getCategories () { return this.categories; }

    public void setCategories (List<String> categories) { this.categories = categories; }

    public void setLinkedUsers (List<Integer> linkedUsers) {this.linkedUsers = linkedUsers;}

    public List<Integer> getLinkedUsers() {return this.linkedUsers;}
}
