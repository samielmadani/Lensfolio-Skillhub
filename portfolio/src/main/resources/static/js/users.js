let searchTerm = "";
let lastSuccessfulSearch = "";

/**
 * Fetch a new page of users
 */
async function fetchPage(pageNumber) {
    pageNumber = parseInt(pageNumber)
    console.log(pageNumber, typeof pageNumber != 'number', pageNumber < 1, pageNumber > maxPage);

    if(typeof pageNumber != 'number' || pageNumber < 1 || pageNumber > maxPage) {
        document.getElementById("currentPageInput").value = currentPage;
        return true;
    }

    const response = await fetch("getUsersPage/" + pageNumber + "?searchTerm=" + searchTerm);

    if (response.status === 200) {
        const html = await response.text();
        const container = document.getElementById("userTableBody");
        container.innerHTML = html;

        await updateControls()
        return true;
    }

    return false;
}

/**
 * Update the userList controls with the new data
 */
async function updateControls() {
    const response = await fetch("api/userListDetails?searchTerm=" + searchTerm);

    if (response.status === 200) {
        const dto = await response.json();
        maxPage = dto.totalPages;
        currentPage = dto.currentPage;

        // Set the page numbers
        document.getElementById("currentPageInput").value = currentPage;
        document.getElementById("totalPages").innerText = "/ " + maxPage;

        // Disable the previous buttons
        if (currentPage <= 1) {
            document.getElementById("prevPageButton").disabled = true;
            document.getElementById("firstPageButton").disabled = true;
        } else {
            document.getElementById("prevPageButton").disabled = false;
            document.getElementById("firstPageButton").disabled = false;
        }

        // Disable the next buttons
        if (currentPage >= maxPage) {
            document.getElementById("nextPageButton").disabled = true;
            document.getElementById("lastPageButton").disabled = true;
        } else {
            document.getElementById("nextPageButton").disabled = false;
            document.getElementById("lastPageButton").disabled = false;
        }
        return;
    }

    showErrorToast();
}

/**
 * Get the next page
 */
async function nextPage() {
    currentPage = Math.min(maxPage, currentPage + 1);
    const success = await fetchPage(currentPage);

    if (!success) showErrorToast();
}

/**
 * Get the previous page
 */
async function previousPage() {
    currentPage = Math.max(1, currentPage - 1);
    const success = await fetchPage(currentPage);

    if (!success) showErrorToast();
}

/**
 * Get the first page
 */
async function firstPage() {
    currentPage = 1;
    const success = await fetchPage(currentPage);

    if (!success) showErrorToast();
}

/**
 * Get the last page
 */
async function lastPage() {
    currentPage = maxPage;
    const success = await fetchPage(currentPage);

    if (!success) showErrorToast();
}

/**
 * Adds user role
 */
async function addRole(id, role) {
    const response = await fetch("api/user/" + id + "/addRole?role=" + role, {method: "POST"})

    if (response.status === 200) {
        const success = await fetchPage(currentPage);

        if (!success) showErrorToast();
    } else {
        const data = await response.json()
        showErrorToast(data.message);
    }
}

/**
 * Removes user role
 */
async function removeRole(id, role) {
    const response = await fetch("api/user/" + id + "/removeRole?role=" + role, {method: "DELETE"})

    if (response.status === 200) {
        const success = await fetchPage(currentPage);

        if (!success) showErrorToast();
    } else {
        const data = await response.json()
        showErrorToast(data.message);
    }
}

/**
 * Change the sorting column
 * @param sort - Column to sort
 */
async function sortBy(sort) {
    const response = await fetch("api/userList/sort?sortBy=" + sort);

    if (response.status === 200) {
        // Unselect all headers
        clearSelectedHeaders()

        // Select this header
        const selectedHeader = document.getElementById(sort.toLowerCase() + "-header");
        selectedHeader.classList.add("selected-column")
        selectedHeader.classList.add("show")

        // Change the icon
        const isAscending = await response.json();
        const icon = selectedHeader.getElementsByClassName("arrow")[0];
        const direction = isAscending? "up" : "down";
        icon.classList.add("bi-arrow-" + direction)
        icon.hidden = false;

        // Update contents
        const success = await fetchPage(currentPage);
        if (!success) showErrorToast();

        return
    }

    showErrorToast()
}

/**
* Update the search parameter and fetch all results that match the value.
**/
async function updateSearch(value) {
    searchTerm = value;
    currentPage = 1;

    const success = await fetchPage(currentPage);
    if (!success) {
        document.getElementById("userSearch").value = lastSuccessfulSearch;
        searchTerm = lastSuccessfulSearch;
    } else {
        lastSuccessfulSearch = searchTerm;
    }
}

/**
 * Deselect all column headers and hide their icons
 */
function clearSelectedHeaders() {
    const selectedHeaders = document.getElementsByClassName('selected-column');

    for (const header of selectedHeaders) {
        header.classList.remove('selected-column');
        header.classList.remove('show');
    }

    const icons = document.getElementsByClassName("arrow");

    for (const icon of icons) {
        icon.classList.remove('bi-arrow-up')
        icon.classList.remove('bi-arrow-down')
        icon.classList.remove('bi-arrow')
        icon.hidden = true;
    }
}