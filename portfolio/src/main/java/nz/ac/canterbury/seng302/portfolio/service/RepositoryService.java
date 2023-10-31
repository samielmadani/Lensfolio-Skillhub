package nz.ac.canterbury.seng302.portfolio.service;

import nz.ac.canterbury.seng302.portfolio.dto.evidence.BranchDTO;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.CommitDTO;
import nz.ac.canterbury.seng302.portfolio.dto.evidence.RepositoryDTO;
import nz.ac.canterbury.seng302.portfolio.model.entities.Repo;
import nz.ac.canterbury.seng302.portfolio.model.repositories.RepoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;

@Service
public class RepositoryService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryService.class);
    @Autowired
    private RepoRepository repos;

    /**
     * Adds all branches from the repository to our repository model
     * @param repo Repository to add branches from
     * @return updated repository
     */
    public RepositoryDTO addBranchesToRepository (RepositoryDTO repo) {
        logger.info(format("Getting all branches for repository %s (%s)", repo.getRepoAlias(),  repo.getRepoAPIKey()));
        //HTTP GET request to GitLab to get all branch information
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .header("PRIVATE-TOKEN", repo.getRepoAPIKey())
                .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repo.getProjectId()+"/repository/branches"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.info(format("Tried to get all branches for repository %s but the APIKey or ProjectId wasn't valid", repo.getRepoAPIKey()));
                return repo;
            }
            String responseBody = response.body(); //GitLab branch response (string format)

            //Because we aren't using any JSON parsing libraries (like jackson) we have to parse the JSON from a
            //string into our simplified branch format (BranchDTO). This has the added benefit of only storing the
            //relevant information which is better for performance.
            //TEST USING JACKSON: time to parse: 0.2ms
            //OUR SOLUTION: time to parse: 0.14ms 🥳🥳🥳
            List<BranchDTO> branches = new ArrayList<>();
            int index = 0;
            while (index >= 0) {
                int newIndex = responseBody.indexOf("\"name\":\"", index); //Get name of branch
                if (newIndex < index) break;
                index = newIndex + 8;
                int branchNameStartLocation = index;
                int branchNameEndLocation = responseBody.indexOf("\"", index);
                String branchName = responseBody.substring(branchNameStartLocation, branchNameEndLocation);
                BranchDTO branch = new BranchDTO();
                branch.setName(branchName);
                branches.add(branch);
            }
            logger.info(format("Got %s branches for repository %s (%s)", branches.size(), repo.getRepoAlias(), repo.getRepoAPIKey()));
            repo.setBranches(branches);
        } catch (IOException | InterruptedException e) {
            logger.error(format("There was an error with the HTTP request to get branches for repository %s (%s)", repo.getRepoAlias(), repo.getRepoAPIKey()));
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
            return repo;
        }
        return repo;
    }

    public boolean repoExistsForGroup (int groupId) {
        return repos.existsById(groupId);
    }

    /**
     * Gets the repository linked to the group passed in. If one doesn't already exist, it creates a new blank one
     * @param groupId ID of the group to get the repo for
     * @return Repo entity for group
     */
    public Repo getRepoForGroup (int groupId) {
        if (repoExistsForGroup(groupId)) {
            return repos.findById(groupId);
        } else {
            logger.info(format("No repo found, creating new repo for group %s", groupId));
            Repo repo = new Repo(groupId);
            return repos.save(repo);
        }
    }

    /**
     * Safely constructs a RepositoryDTO from a Repo entity, including getting all the branches in the repository
     * if the API Key and project ID are valid
     * @param repo Repository to base the RepositoryDTO based off
     * @return RepositoryDTO representation of the repo
     */
    public RepositoryDTO constructRepoDTO (Repo repo) {
        RepositoryDTO repoDTO = new RepositoryDTO(repo.getRepoAPIKey(), repo.getProjectId(), repo.getRepoAlias(), null);
        if (repoDTO.getRepoAPIKey() == null || repo.getProjectId() == -1) return repoDTO;
        return addBranchesToRepository(repoDTO);
    }

    /**
     * Gets the required Linking message to show on the HTML based off of the state of the repository passed in.
     * This function will test the validity of the APIKey and ProjectID and return the corresponding message
     * @param repoDTO repositoryDTO to get status of
     * @return Linked message to show on the HTML
     */
    public String getRepoLinkMessage (RepositoryDTO repoDTO) {
        if (repoDTO.getBranches() == null) {
            if (repoDTO.getRepoAPIKey() == null) return "Not Linked, no API key";
            if (repoDTO.getProjectId() == -1) return "Not Linked, no Project ID";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("accept", "application/json")
                    .header("PRIVATE-TOKEN", repoDTO.getRepoAPIKey())
                    .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects"))
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return "Not Linked, invalid project ID";
                } else {
                    return "Not Linked, invalid API Key";
                }
            } catch (IOException | InterruptedException e) {
                logger.info(format("Couldn't complete GET request for repo %s", repoDTO.getRepoAlias()));
                Thread.currentThread().interrupt();
                return "Not Linked, Internal Server Error";
            }
        } else {
            return "Linked, no errors";
        }
    }

    /**
     * Gets commits for a repository
     * @param repo Repository to get commits for
     * @return list of commits found
     */
    public List<CommitDTO> getCommits (Repo repo) {
        //HTTP GET request to GitLab to get all commit information
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .header("PRIVATE-TOKEN", repo.getRepoAPIKey())
                .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repo.getProjectId()+"/repository/commits?per_page=100"))
                .build();
        return makeCommitRequest(request, repo);
    }

    public List<CommitDTO> getAllCommits (Repo repo) {
        int page = 1;
        List<CommitDTO> allCommits = new ArrayList<>();
        boolean commitsFound = true;
        while (commitsFound) {
            //HTTP GET request to GitLab to get all commit information
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("accept", "application/json")
                    .header("PRIVATE-TOKEN", repo.getRepoAPIKey())
                    .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repo.getProjectId()+"/repository/commits?per_page=100&page=" + page))
                    .build();
            List<CommitDTO> paginatedCommits = makeCommitRequest(request, repo);
            if (paginatedCommits.isEmpty()) {
                commitsFound = false;
                break;
            }
            page += 1;
            allCommits.addAll( paginatedCommits );
        }
        return allCommits;
    }

    /**
     * Gets a list of commits from a repository in a date range
     * @param repo Repository to get commits for
     * @param startDate start date of range to get commits for
     * @param endDate end date of range to get commits for
     * @return list of commits found in the specified date range
     */
    public List<CommitDTO> getCommitsByDateRange (Repo repo, Date startDate, Date endDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //HTTP GET request to GitLab to get all commit information
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .header("PRIVATE-TOKEN", repo.getRepoAPIKey())
                .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repo.getProjectId()+"/repository/commits?per_page=100&since=" + dateFormat.format(startDate) + "&until=" + dateFormat.format(endDate)))
                .build();
        return makeCommitRequest(request, repo);
    }

    public List<CommitDTO> getCommitById (Repo repo, String shortId) {
        List<CommitDTO> allCommits = getAllCommits(repo);
        List<CommitDTO> filteredCommits = new ArrayList<>();
        for (CommitDTO commit : allCommits) {
            if (commit.getCommitId().equals(shortId)) filteredCommits.add(commit);
        }
        return filteredCommits;
    }

    /**
     * Get all commits in a date range and made by a user
     * @param repo Repository to get commits for
     * @param startDate start date of range to get commits for
     * @param endDate end date of range to get commits for
     * @param user name of user as stored in GitLab
     * @return list of commits that are in the date range and made by the user
     */
    public List<CommitDTO> getCommitsByDateRangeAndUser (Repo repo, Date startDate, Date endDate, String user) {
        List<CommitDTO> commitsByDateRange = getCommitsByDateRange(repo, startDate, endDate);
        List<CommitDTO> commitsByUser = getCommitsByUser (repo, user);
        commitsByUser.retainAll(commitsByDateRange);
        return commitsByUser;
    }

    /**
     * Gets all the commits made by a user
     * @param repo Repository to get commits for
     * @param user name of user as stored in GitLab
     * @return list of commits found from that user
     */
    public List<CommitDTO> getCommitsByUser (Repo repo, String user) {
        List<CommitDTO> commits = getAllCommits(repo);
        List<CommitDTO> userCommits = new ArrayList<>();
        for (CommitDTO commit : commits) {
            if (commit.getCommitAuthor().equals(user)) userCommits.add(commit);
        }
        return userCommits;
    }

    /**
     * Makes the commit request to the api and formats the returned body string into usable CommitDTO objects
     * @param request Request to make to the gitlab api
     * @param repo repository to make the request for
     * @return list of all the commits found
     */
    private List<CommitDTO> makeCommitRequest (HttpRequest request, Repo repo) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Couldn't get commits for repository {}", repo.getRepoAlias());
                return new ArrayList<>();
            }
            logger.info("Got response back");
            String responseBody = response.body();
            logger.info("Got response body format");
            return responseBodyToCommits (responseBody);
        } catch (IOException | InterruptedException | ParseException e) {
            logger.error("There was an HTTP error with the request to get commits for repository {}", repo.getRepoAlias());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Converts the string representation of the commit api request into a usable list of CommitDTO objects
     * @param responseBody String of the response body from a gitlab commit api request
     * @return list of all the commits passed through in the body
     * @throws ParseException if there is an error parsing the date format
     */
    private List<CommitDTO> responseBodyToCommits (String responseBody) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        ArrayList<CommitDTO> commits = new ArrayList<>();
        int index = 0;
        while (index >= 0) {
            int newIndex = responseBody.indexOf("\"short_id\":\"", index);
            if (newIndex < index) break;
            index = newIndex + 12;
            int commitShortIDStartLocation = index;
            int commitShortIDEndLocation = responseBody.indexOf ("\"", index);
            String commitShortID = responseBody.substring(commitShortIDStartLocation, commitShortIDEndLocation);
            logger.info("Found commit short ID {}", commitShortID);
            index = commitShortIDEndLocation + 1;
            int commitTitleStartLocation = responseBody.indexOf("\"title\":\"", index) + 9;
            int commitTitleEndLocation = responseBody.indexOf("\",", commitTitleStartLocation);
            String commitTitle = responseBody.substring(commitTitleStartLocation, commitTitleEndLocation);
            logger.info("Found commit title {}", commitTitle);
            index = commitTitleEndLocation + 2;
            int authorNameStartLocation = responseBody.indexOf("\"author_name\":\"", index) + 15;
            int authorNameEndLocation = responseBody.indexOf("\",", authorNameStartLocation);
            String authorName = responseBody.substring(authorNameStartLocation, authorNameEndLocation);
            logger.info("Found commit author {}", authorName);
            index = authorNameEndLocation + 2;
            int commitDateStartLocation = responseBody.indexOf("\"committed_date\":\"", index) + 18;
            int commitDateEndLocation = responseBody.indexOf ("\",", commitDateStartLocation);
            String committedDate = responseBody.substring(commitDateStartLocation, commitDateEndLocation).substring(0, 10);
            logger.info("Got committed date {}", committedDate);
            commits.add(new CommitDTO(commitShortID, authorName, dateFormat.parse(committedDate), commitTitle));
            logger.info("Added new commit");
        }
        return commits;
    }

    /**
     * Gets all the users that are in a gitlab repository
     * @param repo Repository to get users for
     * @return a list of the names of the users in the repository as stored in gitlab
     */
    public List<String> getUsersInRepository (Repo repo) {
        //HTTP GET request to GitLab to get all branch information
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("accept", "application/json")
                .header("PRIVATE-TOKEN", repo.getRepoAPIKey())
                .uri(URI.create("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repo.getProjectId()+"/members"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.info(format("Tried to get all members for repository %s but the APIKey or ProjectId wasn't valid", repo.getRepoAPIKey()));
                return new ArrayList<>();
            }
            List<String> usersInProject = new ArrayList<>();
            String responseBody = response.body(); //GitLab branch response (string format)
            int index = 0;
            while (index >= 0) {
                int newIndex = responseBody.indexOf ("\"name\":\"", index);
                if (newIndex < index) break;
                index = newIndex + 8;
                int userStartLocation = index;
                int userEndLocation = responseBody.indexOf("\",", index);
                usersInProject.add(responseBody.substring(userStartLocation, userEndLocation));
                index = userEndLocation + 2;
            }
            usersInProject.remove("SCRUMBOARD");
            return usersInProject;
        } catch (IOException | InterruptedException e) {
            logger.error(format("There was an error with the HTTP request to get branches for repository %s (%s)", repo.getRepoAlias(), repo.getRepoAPIKey()));
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    public Repo save (Repo repo) {
        return repos.save(repo);
    }
}
