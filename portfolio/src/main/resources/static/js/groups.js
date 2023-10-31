let selectedUsers = [];
let userIdsInGroup = {};
let groupCurrentPage = {};

// Fill out the groups after load
window.onload = async function () {
    await updateAllGroupContent();
}

/**
 * Update all groups on the page
 */
async function updateAllGroupContent() {
    // Get list of all group IDS
    const response = await fetch("api/groups/ids")

    if (response.status === 200) {
        const ids = await response.json();

        // Clears contents of groups then adds up-to-date list from backend
        for (let groupId of ids) {
            await getPaginatedUsers(groupId, 1)
            await updateGroupContent(groupId)
        }

        return
    }

    showErrorToast()
}

/**
 * Fetch a group partial
 */
async function fetchGroup(id) {
    const response = await fetch("group/" + id)

    if (response.status === 200) {
        return await response.text();
    }

    showErrorToast()
    return null;
}

/**
 * Toggle showing and hiding the group content
 */
async function toggleGroupContent(id) {
    const contentContainer = document.getElementById("group-" + id + "-content");
    const icon = document.getElementById("toggle-icon-" + id);

    // Play icon animation
    icon.classList.toggle("down");

    // Visibility of collapsed card copy button
    if (contentContainer.style.maxHeight){
        contentContainer.style.maxHeight = null;
    } else {
        contentContainer.style.maxHeight = contentContainer.scrollHeight + "px";
    }
}

/**
 * Delete a group by ID
 */
async function deleteGroup(id) {
    const response = await fetch("api/groups/" + id, {method: "DELETE"})

    if (response.status === 200) {
        const group = document.getElementById("group-" + id);

        selectedUsers = selectedUsers.filter(user => user.groupId !== id)
        group.remove();

        // Update MWAG
        const mwag = await response.json()
        await getPaginatedUsers(mwag, -1)
        await updateGroupContent(mwag)

        return;
    } else if (response.status === 403) {
        showErrorToast("Students cannot delete groups.")
        return
    } else if (response.status === 400) {
        showErrorToast("Cannot delete default groups.")
        return
    }

    showErrorToast()
}

/**
 * Get all users for a given group
 */
async function getPaginatedUsers(groupId, page) {
    const htmlResponse = await fetch("api/groups/" + groupId + "/users/page/" + page)

    if (htmlResponse.status === 200) {
        const users = document.getElementById("userContent-" + groupId);
        setPageNumber(groupId, page)

        // Get total all user ids
        const idsResponse = await fetch("api/groups/" + groupId + "/users?page=" + page)
        if (idsResponse.status !== 200) {showErrorToast(); return}

        // Set the group user variables
        const ids = await idsResponse.json();
        userIdsInGroup[groupId] = ids;

        // Set HTML
        users.innerHTML = await htmlResponse.text();

        // Select the selected users
        for (let id of ids) {
            if (selectedUsers.filter(user => user.groupId === groupId && user.userId === id).length > 0) {
                const item = document.getElementById("group-" + groupId + "-user-" + id);
                if (!item.classList.contains("selected"))
                    item.classList.add("selected")
            }
        }

        return;
    }

    showErrorToast()
}

/**
 * Change the page by a given value. Example -1 is back a page, 1 is forward one page.
 * @param count Page offset
 */
async function changePage(groupId, count) {
    const previouslySelectedPage = groupCurrentPage[groupId]
    setPageNumber(groupId, previouslySelectedPage + count)

    // Update if page number changed
    if (previouslySelectedPage !== groupCurrentPage[groupId])
        await getPaginatedUsers(groupId, groupCurrentPage[groupId])
}

/**
 * Set the current page for a group
 * @param page page number
 */
function setPageNumber(groupId, page) {
    const previousPageNumber = groupCurrentPage[groupId]
    const pageButton = document.getElementById("group-" + groupId +"-pageNumber-" + page)

    // Select if it exists
    if (pageButton) {
        if (previousPageNumber !== undefined) {
            const previousPageButton = document.getElementById("group-" + groupId +"-pageNumber-" + previousPageNumber)
            if (previousPageButton)
                previousPageButton.classList.remove("active")
        }

        pageButton.classList.add("active")
        groupCurrentPage[groupId] = page
    }
}

/**
 * Fetch a group partial
 */
async function fetchUser(id, groupId) {
    const response = await fetch("api/groups/users/" + id + "?groupId=" + groupId)

    if (response.status === 200) {
        return await response.text();
    }

    return null;
}

/**
 * Create a new group
 */
const createGroup = async () => {
    const short = document.getElementById("shortName");
    const long = document.getElementById("longName");

    if (short.value.length < 1) {
        showErrorToast("Must provide a short name.")
        return
    }
    if (long.value.length < 1) {
        showErrorToast("Must provide a long name.")
        return
    }

    let response = await fetch("api/groups/new?short=" + short.value + "&long=" + long.value)

    if (response.status === 200) {
        // Group created successfully
        const id = await response.json()

        const groupContainer = document.getElementById("groups");
        groupContainer.innerHTML += await fetchGroup(id)
        await getPaginatedUsers(id, 1)

        // Reset the values
        short.value = ""
        long.value = ""

        // Toggles the copy/move button
        toggleCopyVisual()
        await updateGroupContent(id)
        return;

    } else if (response.status === 400) {
        showErrorToast("Names given are not available")
        return;
    }

    showErrorToast()
}

/**
 * Update the remaining characters for the add group modal
 */
const updateCounts = () => {
    const short = document.getElementById("shortName");
    const long = document.getElementById("longName");

    document.getElementById("shortCount").innerText = (15 - short.value.length).toString(10) + " Characters Remaining";
    document.getElementById("longCount").innerText = (50 - long.value.length).toString(10) + " Characters Remaining";
}

/**
 * Get the total number of users in a group
 */
async function fetchGroupUsersCount(groupId) {
    const response = await fetch("api/groups/" + groupId + "/users/count")

    if (response.status === 200) {
        return await response.json();
    }

    return null;
}

/**
 * Copies or moves users appropriately
 */
async function copySelectedUsers(groupId) {
    let response = await fetch("api/groups/" + groupId + "/move",
        {
                method:"POST",
                headers: {'Content-Type': 'application/json' },
                body: JSON.stringify(selectedUsers)
        })

    if (response.status === 200) {
        let dto = await response.json()

        if (dto.copied != null) {
            await getPaginatedUsers(groupId, -1)
            await updateGroupContent(groupId)

            await getPaginatedUsers(dto.noGroupId, 1)
            await updateGroupContent(dto.noGroupId)

            const usersToCopy = selectedUsers.filter(user => !userIdsInGroup[groupId].includes(user.userId))

            if (!isCourseAdmin) {
                let thisUser = usersToCopy.filter(user => user.userId === userId)
                if (thisUser.length > 0) {
                    thisUser.forEach(user => {
                        document.getElementById("settingsButton-group-" + groupId).style.display = "inline"
                    })
                }
            }
        }

        unselectAll()
        toggleCopyVisual()
    }
}

/**
 * Removes the selected users from their groups
 */
async function removeFromGroups() {

    let response = await fetch("api/groups/remove", {method:"DELETE", headers: {'Content-Type': 'application/json' }, body: JSON.stringify(selectedUsers)})

    // Toggles the copy/move button, clears selections
    await unselectAll()

    if (response.status === 200) {
        await updateGroupsAfterResponse(response);
    }
}

/**
 * Refresh the contents with a list of IDS from the request response
 * @param response Response containing a list of groupIds to refresh
 */
async function updateGroupsAfterResponse(response) {
    const alteredGroups = await response.json()

    if (alteredGroups.length !== 0) {
        for (let groupId of alteredGroups) {
            await getPaginatedUsers(groupId, 1)
            await updateGroupContent(groupId)
        }

        if (!isCourseAdmin) {
            let thisUser = alteredGroups.filter(user => user[0] === userId)
            if (thisUser.length > 0) {
                thisUser.forEach(user => {
                    document.getElementById("settingsButton-group-" + user[1]).style.display = "none"
                })
            }
        }
    }

    unselectAll()
    toggleCopyVisual()
}

/**
 * Resize the height of the group content, change the member count, hide/show the no users message
 */
async function updateGroupContent(groupId) {
    const contentContainer = document.getElementById("group-" + groupId + "-content");
    const memberCount = document.getElementById("memberCount-" + groupId);
    const groupUsersCount = await fetchGroupUsersCount(groupId);

    // Update member count text
    memberCount.innerText = groupUsersCount + " members";

    // Update no members message
    const usersList = document.getElementById("userContent-" + groupId)
    if (usersList.innerHTML === "")
        usersList.innerHTML = "<h4>Currently no members in this group</h4>"

    // Update pagination buttons
    const oldButtons = document.getElementById("group-" + groupId + "-page-buttons")
    console.log(oldButtons)
    if (oldButtons) {
        const newButtons = await fetch("groups/" + groupId + "/buttons")
        if (newButtons.status === 200) {
            oldButtons.innerHTML = await newButtons.text();
        }
    }

    // Update the group removal modal
    let count = document.getElementById("countRemoval-" + groupId)
    count.innerText = groupUsersCount + " users will be removed if this is done."

    // Resize the card
    if (contentContainer.style.maxHeight) {
        contentContainer.style.maxHeight = contentContainer.scrollHeight + "px";
    }
    else {
        contentContainer.style.maxHeight = null
    }
}

/**
 * Select only this user. Used by the search pane
 * @param userId selected user
 * @param groupId of the current search
 */
async function selectSearchedUser(userId, groupId) {
    await unselectAll();
    selectedUsers.push({userId: userId, groupId: groupId});
}

/**
 * Add the selected users to their groups
 */
async function addSelectedUsersToGroup() {
    let response = await fetch("api/groups/add", {method:"PUT", headers: {'Content-Type': 'application/json' }, body: JSON.stringify(selectedUsers)})

    if (response.status === 200) {
        await updateGroupsAfterResponse(response);
    }
}

/**
 * Select a user item
 */
async function selectGroupUser(event, userId, groupId) {
    if (!isAdmin) return
    if (!isCourseAdmin) {
        let response = await fetch("api/groups/" + groupId + "/name")
        if (response.status === 200) {
            let groupShortName = await response.text()
            console.log(groupShortName)
            if (groupShortName === "TS") return
        } else return
    }

    const user = document.getElementById("group-" + groupId + "-user-" + userId)

    if (event.shiftKey) {
        let lastItem = selectedUsers[(selectedUsers.length-1)];

        // Dont run shift feature, just select
        if (selectedUsers.length === 0 || lastItem.groupId !== groupId) {
            const item = document.getElementById("group-" + groupId + "-user-" + userId);
            if (!item.classList.contains("selected")) {
                item.classList.toggle("selected")
                selectedUsers.push({"userId": userId, "groupId": groupId})
            }
            return;
        }

        // Selecting the same user
        if (lastItem.userId === userId) return;

        // Get the index of the last selected item and the item being clicked
        let index1 = userIdsInGroup[groupId].indexOf(
            lastItem.userId
        );
        let index2 = userIdsInGroup[groupId].indexOf(userId);

        // Add the selected class to everything between the indexes
        for (let tempId of userIdsInGroup[groupId].slice(Math.min(index1, index2), Math.max(index1, index2) + 1)) {
            const item = document.getElementById("group-" + groupId + "-user-" + tempId);
            if (!item.classList.contains("selected")) {
                item.classList.toggle("selected")
                selectedUsers.push({"userId": tempId, "groupId": groupId})
            }
        }
    }
    else if (event.ctrlKey) {
        // Deselect everything
        for (let x of selectedUsers) {
            document.getElementById("group-" + x.groupId + "-user-" + x.userId).classList.toggle("selected")
        }

        // Select the single user
        selectedUsers = [{"userId": userId, "groupId": groupId}];
        user.classList.add("selected")
    } else {
        // Toggle the selected item
        user.classList.toggle("selected")
        if (user.classList.contains("selected")) {
            selectedUsers.push({"userId": userId, "groupId": groupId})
        } else {
            selectedUsers = selectedUsers.filter((user) => !(user.userId === userId && user.groupId === groupId))
        }
    }

    // Toggles the copy/move button
    toggleCopyVisual()
}

/**
 * Toggle the copy buttons hidden and visible
 */
function toggleCopyVisual() {
    let buttonsList = document.getElementsByName("copyMoveButton");
    let addGroupButton = document.getElementById("addGroupButton");
    let unselectAllButton = document.getElementById("unselectAllButton");
    let removeSelectedButton = document.getElementById("removeSelectedButton");

    if (selectedUsers.length !== 0) {
        buttonsList.forEach(b => {
            b.style.opacity = "1"
            b.style.maxHeight = b.scrollHeight + "px";
        })

        addGroupButton.style.opacity = "0";
        unselectAllButton.style.opacity = "1";
        removeSelectedButton.style.opacity = "1";

        addGroupButton.style.maxHeight = "0%";
        unselectAllButton.style.maxHeight = "100%";
        removeSelectedButton.style.maxHeight = "100%";

        addGroupButton.setAttribute("disabled", "true");
        unselectAllButton.setAttribute("disabled", "false");
        removeSelectedButton.setAttribute("disabled", "false");

    } else {
        buttonsList.forEach(b => {
            b.style.opacity = "0"
            b.style.maxHeight = "0"
        })

        addGroupButton.style.opacity = "1";
        unselectAllButton.style.opacity = "0";
        removeSelectedButton.style.opacity = "0";

        addGroupButton.style.maxHeight = "100%";
        unselectAllButton.style.maxHeight = "0%";
        removeSelectedButton.style.maxHeight = "0%";

        addGroupButton.setAttribute("disabled", "false");
        unselectAllButton.setAttribute("disabled", "true");
        removeSelectedButton.setAttribute("disabled", "true");
    }
}

/**
 * Unselect all users, even users selected on different pages
 */
function unselectAll() {
    // Toggles the copy/move button, clears selections
    for (let x of selectedUsers) {
        const element = document.getElementById("group-" + x.groupId + "-user-" + x.userId)
        if (element)
            element.classList.remove("selected")
    }
    selectedUsers = []
    toggleCopyVisual()
}

//Go to the settings page for this group
function openGroupSettings (groupId) {
    window.location.href = "groupSettings?groupID=" + groupId;
}
