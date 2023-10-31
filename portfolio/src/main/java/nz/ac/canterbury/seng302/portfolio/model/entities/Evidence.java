package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.*;
import java.util.*;

import static java.lang.String.format;
import static java.lang.String.join;

@Entity
public class Evidence {
    @Id
    @Column(name = "evidence_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String name;
    private Date date;
    private String description;
    private int userId;
    private int projectId;
    private String usersName;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "evidence_id")
    private List<EvidenceSkill> skills = new ArrayList<>();

    @ElementCollection
    private List<String> categories = new ArrayList<>();

    @ElementCollection
    private List<EvidenceUser> linkedUsers = new ArrayList<>();

    @ElementCollection
    private List<String> webLinks = new ArrayList<>();

    @ElementCollection
    private List<RepositoryCommit> commits = new ArrayList<>();

    protected Evidence() {}
    
    public Evidence(String name, Date date, String description, int user, String usersName) {
        this.name = name;
        this.date = date;
        this.description = description;
        this.userId = user;
        this.usersName = usersName;
    }

    public Evidence(String name, Date date, String description, int user, String usersName, int projectId) {
        this.name = name;
        this.date = date;
        this.description = description;
        this.userId = user;
        this.usersName = usersName;
        this.projectId = projectId;
    }

    public Evidence (String name, Date date, String description, int user, List<String> webLinks, String usersName) {
        this.name = name;
        this.date = date;
        this.description = description;
        this.userId = user;
        this.webLinks = webLinks;
        this.usersName = usersName;
    }

    @Override
    public String toString() {
        return format("Evidence{id=%s, name='%s', date=%s, description='%s', user=%s, webLinks=%s}",
                        id, name, date, description, userId, join(", ", webLinks));
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getUserId () {
        return userId;
    }
    
    public void setUserId (int user) {
        this.userId = user;
    }

    public void addWebLink (String webLink) { webLinks.add(webLink); }

    public List<String> getWebLinks () { return this.webLinks; }

    public void setWebLinks (List<String> webLinks) { this.webLinks = webLinks; }

    public List<EvidenceSkill> getSkills() { return this.skills; }

    public void addSkill(EvidenceSkill skill) {
        this.skills.add(skill);
    }

    public String getUsersName () { return this.usersName; }

    public void setUsersName (String usersName) { this.usersName = usersName; }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public int getProjectId () { return projectId; }

    public void setProjectId (int projectId) { this.projectId = projectId;}

    public void addCommit (RepositoryCommit commit) {this.commits.add(commit);}

    public List<RepositoryCommit> getCommits () {return this.commits;}

    public void removeCommit (RepositoryCommit commit) {this.commits.remove(commit);}

    public void setCommits (List<RepositoryCommit> commits) {this.commits = commits;}

    public List<EvidenceUser> getLinkedUsers() {return this.linkedUsers;}

    public void setLinkedUsers (List<EvidenceUser> linkedUsers) {this.linkedUsers = linkedUsers;}
}
