/**
 * Handles showing/hiding of commit information for a branch. Toggles the animations for the expanded icon, and
 * the animation for displaying/hiding commits.
 * @param branchLink HTML element of branch button to view all commit information for a branch
 * @returns {Promise<void>} null
 */
async function toggleCommitsForBranch (branchLink) {
    //Toggle animation for dropdown icon
    document.getElementById("dropDownIcon" + branchLink.id).classList.toggle("expanded")
    //Gets the card body to add the commit information to
    let commitPane = document.getElementById("commitsFor" + branchLink.id)
    //If there is not already commit information, get the commit information for the branch
    if (commitPane.innerHTML === "") {
        await getCommitFromBranch(branchLink.id, 10)
    }
    //Animation
    if (commitPane.style.maxHeight) {
        commitPane.style.setProperty("padding", "0", "important")
        commitPane.style.maxHeight = null
    } else {
        commitPane.style.setProperty("padding", "1rem 1rem", "important")
        commitPane.style.maxHeight = commitPane.scrollHeight + "px"
    }
}

/**
 * Gets commits from a branch specified by the APIKey in the group settings. Gets all Commits from GitLab API and passes
 * them to our API to get HTML thymeleaf fragments to add to the page
 * @param branchName Name of the branch to get the commits for
 * @param numOfCommits number of commits to get (maximum 100)
 * @returns {Promise<void>} null
 */
async function getCommitFromBranch(branchName, numOfCommits) {
    //Get commits from GitLab API
    let response = await fetch("https://eng-git.canterbury.ac.nz/api/v4/projects/"+repository.projectId+"/repository/commits?ref_name=" + branchName + "&per_page=" + numOfCommits,
        {method: 'GET', headers: {"accept": "application/json", "PRIVATE-TOKEN": repository.repoAPIKey}});
    let response_body = await response.json();
    let arrayList = [];
    //convert GitLab API response into simplified JSON to pass to our API controller
    response_body.forEach(obj => {
        arrayList.push({commitId: obj.short_id,
                        commitAuthor: obj.author_name,
                        commitDate: obj.committed_date.substring(0, 10),
                        commitName: obj.title})
    })
    //our API GET request to fetch thymeleaf fragments
    response = await fetch("api/repository/commitsFragment?branchName=" + branchName, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(arrayList)})
    //Add returned fragments to the page
    document.getElementById("commitsFor" + branchName).innerHTML = await response.text();
    //Decides if the show more button should be toggled or not
    if (numOfCommits === 100) document.getElementById("branchCheckbox" + branchName).checked = true
}

/**
 * Handles when the checkbox for showing more commit value changes
 * @param obj checkbox HTML element
 */
async function checkboxChanged (obj) {
    if (obj.checked) {
        await getCommitFromBranch(obj.dataset.branch, 100)
    } else {
        await getCommitFromBranch(obj.dataset.branch, 10)
    }
    //Resize pane with new commit height
    let commitPane = document.getElementById("commitsFor" + obj.dataset.branch)
    commitPane.style.maxHeight = commitPane.scrollHeight + "px"
}

/**
 * Links repository API key and projectID based on the information on the groupSettings page
 * @param groupId ID of the group to link repository with
 * @returns {Promise<void>} null
 */
async function linkRepository (groupId) {
    let repoAPIKeyValue = document.getElementById("linkRepoKey").value
    let repoProjectIdValue = document.getElementById("repoProjectIdInput").value
    let body = {repoAPIKey: repoAPIKeyValue, projectId: repoProjectIdValue, repoAlias: null, branches: null}
    await fetch("api/group/"+groupId+"/linkRepository",
        {method: "PUT",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)})
    window.location.href = "groupSettings?groupID=" + groupId //reload the page to apply the changes
}

/**
 * Update repository alias for group
 * @param groupId ID of the group to update repo alias for
 * @returns {Promise<void>} null
 */
async function updateRepoAlias (groupId) {
    let repoAliasInput = document.getElementById("repoAliasInput")
    if (repoAliasInput.value.replace(/\s/g, '') === '') {
        if (repoAliasInput.classList.contains("is-valid")) repoAliasInput.classList.remove("is-valid")
        repoAliasInput.classList.add("is-invalid")
    } else {
        if (repoAliasInput.classList.contains("is-invalid")) repoAliasInput.classList.remove("is-invalid")
        repoAliasInput.classList.add("is-valid")
        let response = await fetch("api/group/" + groupId + "/updateRepoAlias?alias=" + repoAliasInput.value, {method: "PUT"})
        if (response.status !== 200) {
            showErrorToast()
        }
    }
}

/**
 * Update the remaining characters for the edit group names inputs
 */
const updateCounts = () => {
    const short = document.getElementById("groupShortNameInput");
    const long = document.getElementById("groupLongNameInput");

    document.getElementById("shortCount").innerText = (15 - short.value.length).toString(10) + " Characters Remaining";
    document.getElementById("longCount").innerText = (50 - long.value.length).toString(10) + " Characters Remaining";
}

/**
 * Updates the group short/long name
 * @param groupId ID of the group to update for
 * @returns {Promise<void>} null
 */
async function updateGroupNames (groupId) {
    const groupLongName = document.getElementById("groupLongNameInput")
    const groupShortName = document.getElementById("groupShortNameInput")
    let error = false
    //ensure long name is valid
    if (groupLongName.value.replace(/\s/g, '') === '') {
        //bootstrap validation
        if (groupLongName.classList.contains("is-valid")) groupLongName.classList.remove("is-valid")
        groupLongName.classList.add("is-invalid")
        error = true
    } else {
        if (groupLongName.classList.contains("is-invalid")) groupLongName.classList.remove("is-invalid")
        groupLongName.classList.add("is-valid")
    }
    //ensure short name is valid
    if (groupShortName.value.replace(/\s/g, '') === '') {
        if (groupShortName.classList.contains("is-valid")) groupShortName.classList.remove("is-valid")
        groupShortName.classList.add("is-invalid")
        error = true
    } else {
        if (groupShortName.classList.contains("is-invalid")) groupShortName.classList.remove("is-invalid")
        groupShortName.classList.add("is-valid")
    }

    if (!error) {
        //make api request to change details
        const URL = "api/groups/" + groupId + "?longName=" + groupLongName.value + "&shortName=" + groupShortName.value
        let response = await fetch(URL, {method: "PUT"});
        if (response.status !== 200) {
            showErrorToast()
        }
    }
}


async function linkProject (projectId, groupId) {
    const response = await fetch("api/project/" + projectId + "/linkGroup/" + groupId, {method: "PUT"})

    if (response.status !== 201) {
        showErrorToast()
    } else {
        const response2 = await fetch("api/project/" + projectId + "/linkedProject/" + groupId, {method: "GET"})
        let data = await response2.text();

        document.getElementById("linkedProjects").innerHTML += data;
        document.getElementById("projectDropdown-" + projectId).remove()
        document.getElementById("hiddenLabel").hidden = true


    }



}

async function unlinkProject(projectId, groupId) {
    await fetch("api/project/" + projectId + "/unlinkGroup/" + groupId, {method: "DELETE"})
    const projectName = document.getElementById("project-" + projectId).dataset.projectname;
    document.getElementById("project-" + projectId).remove()

    if (document.getElementById("projectLink-" + projectId)) {
        document.getElementById("projectLink-" + projectId).remove()
    }

    document.getElementById("dropdown-" + groupId).innerHTML += "<li> <a class='dropdown-item' href='#' id='projectDropdown-" + projectId + "' onclick='linkProject(" + projectId + ", " + groupId + ")'>" + projectName + "</a> </li>"


    if (document.getElementsByName("projectName").length === 0) {
        document.getElementById("hiddenLabel").hidden = false;
    }

}