package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;
import nz.ac.canterbury.seng302.portfolio.model.entities.ProjectUser;
import nz.ac.canterbury.seng302.portfolio.model.repositories.ProjectUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectUserService {
    @Autowired
    private ProjectUserRepository projectUsers;
    @Autowired
    private ProjectService projects;

    /**
     * Get all the projects related to a userId
     */
    public ArrayList<Project> getProjectsForUser (int userId) {
        List<ProjectUser> projectIds = projectUsers.findByUserId(userId);
        ArrayList<Project> prjs = new ArrayList<>();
        for (ProjectUser projectUser : projectIds) {
            prjs.add(projects.getProjectById(projectUser.getProjectId()));
        }
        if (!prjs.contains(projects.generateDefaultProject())) prjs.add(projects.generateDefaultProject());
        return prjs;
    }

    /**
     * Get all userIds that are included in a project
     */
    public ArrayList<Integer> getUserIdsForProject (int projectId) {
        List<ProjectUser> userIds = projectUsers.findByProjectId(projectId);
        ArrayList<Integer> users = new ArrayList<>();
        for (ProjectUser projectUser : userIds) {
            users.add(projectUser.getUserId());
        }
        return users;
    }

    /**
     * Save or create a projectUser relation
     * @return Updated ProjectUser
     */
    public ProjectUser save(ProjectUser projectUser) {
        if (projectUser == null) {
            return null;
        }
        return projectUsers.save(projectUser);
    }

    /**
     * Check if a user is in a project, by ID
     */
    public boolean isUserInProject (int projectId, int userId) {
        return !projectUsers.findByProjectIdAndUserId(projectId, userId).isEmpty();
    }

    /**
     * Add a user to a project by their Ids
     */
    public void addUserToProject (int projectId, int userId) {
        save(new ProjectUser(projectId, userId));
    }

    public void removeUserFromProject (int projectId, int userId) {
        projectUsers.delete(new ProjectUser(projectId, userId));
        projectUsers.deleteByProjectIdAndUserId(projectId, userId);
    }

}
