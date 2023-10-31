let currentUser = ""
let currentGroup = -1
let commitRangeStart = null
let commitRangeEnd = null
let selectedCommits = []
let commitIdSearch = ""

async function updateUserFilter (event) {
    currentUser = event.dataset.user
    await updateGroupCommits()
}

async function updateGroupCommits () {
    let URI
    if (commitIdSearch !== "") {
        URI = "api/groups/" + currentGroup + "/commits?id=" + commitIdSearch
    } else {
        if (currentUser !== "") {
            if (commitRangeStart !== null && commitRangeEnd !== null) {
                URI = "api/groups/" + currentGroup + "/commits?user=" + currentUser + "&startDate=" +commitRangeStart + "&endDate="+commitRangeEnd
            } else {
                URI = "api/groups/" + currentGroup + "/commits?user=" + currentUser
            }
        } else {
            if (commitRangeStart !== null && commitRangeEnd !== null) {
                URI = "api/groups/" + currentGroup + "/commits?startDate=" +commitRangeStart + "&endDate="+commitRangeEnd
            } else {
                URI = "api/groups/" + currentGroup + "/commits"
            }

        }
    }

    document.getElementById("linkedCommitsList").innerHTML = ""
    document.getElementById("commitLoadingIcon").hidden = false;

    let response = await fetch(URI)
    if (response.status === 200) {
        document.getElementById("linkedCommitsList").innerHTML = await response.text()
        document.getElementById("commitLoadingIcon").hidden = true;
        updateCheckBoxes()
    }
}

function resetCommits() {
    selectedCommits = []
    document.getElementById("commitTagsArea").innerHTML = ""
    document.getElementById("linkCommitContainer").hidden = true;
}

function updateCheckBoxes() {
    console.log("Checking commits")
    // Check all of the commits that need to be checked
    for (let selectedCommit of selectedCommits) {
        console.log(selectedCommit)
        const checkbox = document.getElementById("commit-" + selectedCommit.commitId + "-checkbox")
        if (checkbox) {
            checkbox.checked = true;
        }
    }
}

async function changeGroup (groupId) {
    currentGroup = groupId
    currentUser = ""
    commitRangeEnd = null
    commitRangeStart = null
    commitIdSearch = ""
    document.getElementById("startDateRange").value = null
    document.getElementById("endDateRange").value = null
    document.getElementById("newLinkedCommitId").value = ""
    await updateGroupCommits ()
    //get new users in the group
    const response = await fetch("api/groups/"+groupId+"/usersInRepo")
    if (response.status === 200) {
        document.getElementById("userDropdownArea").innerHTML = await response.text()
        updateCheckBoxes()
    }
}

function toggleCommit(commit) {
    const commitId = commit.commitId || commit.dataset.cid;
    console.log(commitId)
    const checkbox = document.getElementById("commit-" + commitId + "-checkbox")

    if (document.getElementById(commitId + "-chip")) {
        // Remove chip
        document.getElementById(commitId + "-chip").remove()

        // Remove from list
        selectedCommits = selectedCommits.filter(commitItem => { return commitItem.commitId !== commitId })

        // Uncheck box
        if (checkbox) checkbox.checked = false;
    } else {
        // Add chip
        const container = document.getElementById("commitTagsArea")
        container.innerHTML += "<div id='" + commitId + "-chip' class='chip' onclick='toggleCommit(this)' data-cid='" + commitId + "'>" + commitId + "</div>"

        // Create a dto
        const commitDto = {
            'commitId': commitId,
            'commitAuthor': commit.commitAuthor || commit.dataset.cuser,
            'commitDate': new Date( commit.commitDate || commit.dataset.cdate),
            'commitName': commit.commitName || commit.dataset.cname
        }

        // Add to list
        selectedCommits.push(commitDto)

        // Check box
        if (checkbox) checkbox.checked = true;
    }

    console.log(selectedCommits)
    enableButtons()
}

function startDateModified () {
    if (validateCommitDates()) {
        commitRangeStart = document.getElementById("startDateRange").value
        updateGroupCommits()
    }
}

function endDateModified () {
    if (validateCommitDates()) {
        commitRangeEnd = document.getElementById("endDateRange").value
        updateGroupCommits()
    }
}

function clearDates () {
    document.getElementById("startDateRange").value = null
    document.getElementById("endDateRange").value = null
    commitRangeStart = null
    commitRangeEnd = null
    if (document.getElementById("startDateRange").classList.contains("is-invalid")) document.getElementById("startDateRange").classList.remove("is-invalid")
    if (document.getElementById("endDateRange").classList.contains("is-invalid")) document.getElementById("endDateRange").classList.remove("is-invalid")
    updateGroupCommits()
}

function validateCommitDates() {
    const startDateEl = document.getElementById("startDateRange")
    const endDateEl = document.getElementById("endDateRange")
    let commitRangeStartDate = new Date(startDateEl.value)
    let commitRangeEndDate = new Date(endDateEl.value)
    if( commitRangeStartDate <= commitRangeEndDate ) {
        if (startDateEl.classList.contains("is-invalid")) startDateEl.classList.remove("is-invalid")
        if (endDateEl.classList.contains("is-invalid")) endDateEl.classList.remove("is-invalid")
        return true
    } else {
        if (!startDateEl.classList.contains("is-invalid")) startDateEl.classList.add("is-invalid")
        if (!endDateEl.classList.contains("is-invalid")) endDateEl.classList.add("is-invalid")
        return false
    }
}

function sprintClicked (event) {
    document.getElementById("startDateRange").value = event.dataset.datestart
    document.getElementById("endDateRange").value = event.dataset.dateend
    if (event.dataset.datestart === "") {
        commitRangeStart = null
        commitRangeEnd = null
    } else {
        commitRangeStart = event.dataset.datestart
        commitRangeEnd = event.dataset.dateend
    }
    updateGroupCommits()
}

function searchByCommitId () {
    commitIdSearch = document.getElementById("newLinkedCommitId").value
    updateGroupCommits()
}
