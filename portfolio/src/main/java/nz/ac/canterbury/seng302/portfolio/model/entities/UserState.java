package nz.ac.canterbury.seng302.portfolio.model.entities;

import javax.persistence.*;

/**
 * Entity class (database table) storing user state information. Any state that must be persistent through
 * logout can be kept here.
 */
@Entity
public class UserState {
    /**
     * Identification of the userState. For database purposes
     */
    @Id
    @Column(name = "state_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long stateId;

    /**
     * ID of the user who owns the state
     */
    @Column(unique = true, nullable = false)
    protected int userId;

    /**
     * For users table, keep sort by
     */
    protected String sortBy;

    /**
     * For users table, keep ascending/descending. True if ascending
     */
    protected boolean ascending;

    /**
     * Current page in table
     */
    protected int page;

    /**
     * Empty constructor for repository access
     */
    protected UserState() {}

    /**
     * Create a new UserState
     * @param userId - UserId for owner of state
     */
    public UserState(int userId) {
        this.userId = userId;
        sortBy = "Username";
        ascending = true;
        page = 1;
    }

    @Override
    public String toString() {
        return "UserState{" +
                "stateId=" + stateId +
                ", userId=" + userId +
                ", sortBy='" + sortBy + '\'' +
                ", ascending=" + ascending +
                ", page=" + page +
                '}';
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public String getAscendingString() {
        return ascending? "ASC" : "DESC";
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
