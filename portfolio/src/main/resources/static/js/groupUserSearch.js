let latestPaginationButtonRequest;
let latestPaginationUserRequest;

/**
 * Gets search query for a group user search
 * @param groupId ID of the group to get user query for
 * @returns {*} sanitised query passed into search box
 */
function getSearchQuery (groupId) {
    let rawQuery = document.getElementById("userSearchBar-" + groupId).value
    return rawQuery.replaceAll(/\W+/g, " ")
}

/**
 * Gets the pagination buttons for a user search in a group based on the query
 * @param groupId ID of the group to get buttons for
 * @param query query to search
 * @returns {Promise<void>} null
 */
async function getPaginationButton(groupId, query) {
    let URI = "api/users/search/" + groupId + "/buttons?query=" + query
    URI = URI.replaceAll(" ", "%20") // format for http

    //Perform query, but only receive response if it belongs to the latest request
    latestPaginationButtonRequest = URI
    fetch(URI).then(async response => {
        if (response.status === 200) {
            response.text().then((htmlText) => {
                if (response.url.includes(latestPaginationButtonRequest)) {
                    document.getElementById('usersList-' + groupId).innerHTML = htmlText
                }
            })
        }
    })
}

/**
 * Sets the styles for pagination buttons in group user search
 * @param groupId ID of the group to set pagination button styles for
 * @param page page num to set the style for
 */
function setPageButtonStyles (groupId, page) {
    //reset previous display
    let pageButtons = document.getElementsByName("searchButton-" + groupId)
    pageButtons.forEach(button => {
        if (button.classList.contains('active')) button.classList.remove('active')
    })
    //set current display
    document.getElementById("group-" + groupId + "-pageNumber-" + page + "-search").classList.add('active')
}

/**
 * Changes the search page by an offset
 * @param groupId ID of the group to change user search page for
 * @param offset number of pages to add to the offset. In our application this is +1 or -1
 * @returns {Promise<void>} null
 */
async function changeSearchPage (groupId, offset) {
    const pageButtons = document.getElementById("paginationSearchButtons-" + groupId)
    let current = parseInt(pageButtons.dataset.pagenum)
    let max = parseInt(pageButtons.dataset.maxpage)
    if ((current + offset) > 0 && (current + offset) <= max) {
        await getPaginatedFilteredUsers (groupId, current + offset)
    }
}

/**
 * Gets the paginated search user html from the backend endpoint
 * @param groupId ID of the group to search
 * @param page current page
 * @returns {Promise<void>} null
 */
async function getPaginatedFilteredUsers (groupId, page) {
    const pageButtons = document.getElementById("paginationSearchButtons-" + groupId)
    if (pageButtons) {
        pageButtons.dataset.pagenum = page
        setPageButtonStyles (groupId, page)
    } else {
        page = 1
    }
    page -= 1;
    let URI = "api/users/search/" + groupId + "?query=" + getSearchQuery(groupId) + "&page=" + page
    URI = URI.replace(" ", "%20") // format for http

    //Perform query, but only receive response if it belongs to the latest request
    latestPaginationUserRequest = URI;
    fetch (URI).then(async (response) => {
        if (response.status === 200) {
            response.text().then((htmlText) => {
                if (response.url.includes(latestPaginationUserRequest)) {
                    document.getElementById("userSearchLocation-" + groupId).innerHTML = htmlText;
                }
            })
        }
    })
}

/**
 * Initial loader for getting users in search query
 * @param groupId ID of the group to get users for
 * @returns {Promise<void>} null
 */
async function searchUsersInGroup(groupId) {
    await getPaginationButton(groupId, getSearchQuery(groupId));
    await getPaginatedFilteredUsers(groupId, 1);
}

async function clickOnUser(userId, groupId) {

}