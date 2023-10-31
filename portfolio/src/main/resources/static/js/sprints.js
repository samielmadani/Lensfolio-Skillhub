
window.addEventListener('load', () => {
    updateSprints();
});

let numberOfSprints = 0;

/**
 * Toggle a sprints edit mode
 */
function toggleSprintEdit(id) {
    const editFields = document.getElementById("editSprintDetails" + id);
    const sprintDetails = document.getElementById("sprintDetails" + id);
    const sprintButtons = document.getElementById("sprintButtons" + id.toString());

    editFields.hidden = !editFields.hidden;
    sprintButtons.hidden = !sprintButtons.hidden;
    sprintDetails.hidden = !sprintDetails.hidden;

    if (!editFields.hidden) {
        getSprintStartDateBounds(id)
        getSprintEndDateBounds(id)
    }
}

/**
 * Update full sprint list
 */
async function updateSprints() {
    const response = await fetch("api/sprint/getSprintIds/" + projectId)

    if (response.status === 200) {
        const sprintContainer = document.getElementById("sprintContainer");
        const ids = await response.json();

        if (ids !== null) numberOfSprints = ids.length;
        updateNoSprintsMessage()

        for (let id of ids) {
            sprintContainer.innerHTML += await fetchSprint(id);

            // Color the sprint
            const elementToColor = document.getElementById("sprintHeader-" + id)
            elementToColor.style.backgroundColor = getBackgroundColour(id);

            // Get date bounds
            await getSprintStartDateBounds(id);
            await getSprintEndDateBounds(id);
        }

        await displayInlineAdvents()

        return;
    }

    showErrorToast()
}

/**
 * Fetch a sprint partial
 */
async function fetchSprint(id) {
    const response = await fetch("sprint/" + id)

    if (response.status === 200) {
        const content = await response.text();
        return content;
    }

    showErrorToast()
    return null;
}

/**
 * Create a new default sprint
 */
async function createSprint() {
    const sprintIdResponse = await fetch("api/sprint/" + projectId, { method: "POST",})

    if (sprintIdResponse.status === 403) {
        showErrorToast("You must be a teacher to add a sprint")
        return
    } else if (sprintIdResponse.status === 400) {
        const content = await sprintIdResponse.json()
        showErrorToast(content.message)
        return
    } else if (sprintIdResponse.status !== 201) {
        showErrorToast()
        return
    }

    const sprintId = await sprintIdResponse.json();
    const sprintPartialResponse = await fetchSprint(sprintId)

    if (sprintPartialResponse == null) return

    const sprintContainer = document.getElementById("sprintContainer");
    sprintContainer.innerHTML += sprintPartialResponse;

    // Color the sprint
    const elementToColor = document.getElementById("sprintHeader-" + sprintId)
    elementToColor.style.backgroundColor = getBackgroundColour(sprintId);

    // Get date bounds
    getSprintStartDateBounds(sprintId);
    getSprintEndDateBounds(sprintId);

    numberOfSprints++;
    updateNoSprintsMessage()
    toggleSprintEdit(sprintId)

    await displayInlineAdvents()
}

async function toggleSprintDelete(id) {
    const confirmDeleteButtons = document.getElementById("confirmDeleteButtons" + id);
    const sprintButtons = document.getElementById("sprintButtons" + id);

    sprintButtons.hidden = !sprintButtons.hidden;
    confirmDeleteButtons.hidden = !confirmDeleteButtons.hidden;
}

/**
 * Fetch a sprint label by sprint id
 */
async function fetchSprintLabel(id) {
    const response = await fetch("api/sprint/" + id + "/Label");
    if (response.status !== 200) {
        const errorMessage = await response.json()
        console.log(errorMessage)
        showErrorToast()
        return
    }

    const label = await response.text();
    console.log(label)
    return label;
}

/**
 * Refresh all sprint labels from the backend
 */
async function refreshSprintLabels() {
    const response = await fetch("api/sprint/getSprintIds/" + projectId)

    if (response.status === 200) {
        const ids = await response.json();
        for (let id of ids) {
            const sprintLabel =  await fetchSprintLabel(id);
            document.getElementById("sprintLabel-" + id).innerText = sprintLabel;
        }
        return;
    }

    showErrorToast()
}

/**
 * Delete a sprint by id
 */
async function deleteSprint(id) {
    const deleteResponse = await fetch("api/sprint/" + id, {method: "DELETE",});

    if (deleteResponse.status === 200) {
        document.getElementById("sprint-" + id).remove();
        numberOfSprints--;
        updateNoSprintsMessage()
    } else if (deleteResponse.status === 403) {
        showErrorToast("You must be a teacher to delete a sprint.")
    } else {
        showErrorToast()
    }

    await refreshSprintLabels()

    await displayInlineAdvents()
}

function updateNoSprintsMessage() {
    const message = document.getElementById("noSprintMessage");
    message.hidden = numberOfSprints > 0;
}

/**
 * Update the sprint details.
 */
async function editSprint(id) {
    const sprintName = document.getElementById("sprintName-" + id).value;
    const sprintDescription = document.getElementById("sprintDescription-" + id).value;
    const sprintStartDate = document.getElementById("sprintStartDate-" + id).value;
    const sprintEndDate = document.getElementById("sprintEndDate-" + id).value;

    if (sprintName.length < 1) {
        // Show error on sprint name
        document.getElementById("sprintName-"+id).classList.add("is-invalid");
        document.getElementById("sprintName-feedback-"+id).innerText = "Sprint title is a required field."
        return
    } else {
        document.getElementById("sprintName-"+id).classList.remove("is-invalid");
    }

    const body = {
        name: sprintName,
        description: sprintDescription,
        startDateString: sprintStartDate,
        endDateString: sprintEndDate
    }

    const sprintIdResponse = await fetch("api/sprint/" + id + "/Update",
        {
            method: "POST",
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        }
    );

    if (sprintIdResponse.status === 403) {
        showErrorToast("You must be a teacher to edit a sprint.")
        return
    } else if (sprintIdResponse.status === 400) {
        let content = (await sprintIdResponse.json()).message
        console.log(content)

        if (content.startsWith("SprintName")) {
            content = content.substring("SprintName".length);
            document.getElementById("sprintName-"+id).classList.add("is-invalid");
            document.getElementById("sprintName-feedback-"+id).innerText = content;
            return
        } else {
            document.getElementById("sprintName-"+id).classList.remove("is-invalid");
        }

        if (content.startsWith("SprintStartDate")) {
            content = content.substring("SprintStartDate".length);
            document.getElementById("sprintStartDate-"+id).classList.add("is-invalid");
            document.getElementById("sprintDateFeedback-"+id).innerText = content;
            document.getElementById("sprintDateFeedback-"+id).hidden = false;
            return
        } else {
            document.getElementById("sprintStartDate-"+id).classList.remove("is-invalid");
            document.getElementById("sprintDateFeedback-"+id).hidden = true;
        }

        if (content.startsWith("SprintEndDate")) {
            content = content.substring("SprintEndDate".length);
            document.getElementById("sprintEndDate-"+id).classList.add("is-invalid");
            document.getElementById("sprintDateFeedback-"+id).innerText = content;
            document.getElementById("sprintDateFeedback-"+id).hidden = false;
            return
        } else {
            document.getElementById("sprintEndDate-"+id).classList.remove("is-invalid");
            document.getElementById("sprintDateFeedback-"+id).hidden = true;
        }

        showErrorToast()
        return
    } else if (sprintIdResponse.status !== 200) {
        showErrorToast()
        return
    }

    const sprintPartialResponse = await fetchSprint(id);
    if (sprintPartialResponse == null) return

    // Get the inner contents of the partial
    const responseDocument = document.createElement("html");
    responseDocument.innerHTML = sprintPartialResponse;
    const innerSprintPartial = responseDocument.querySelector("#sprint-" + id).innerHTML;

    const oldSprint = document.getElementById("sprint-" + id);
    oldSprint.innerHTML = innerSprintPartial;

    // Color the sprint
    const elementToColor = document.getElementById("sprintHeader-" + id)
    elementToColor.style.backgroundColor = getBackgroundColour(id);

    // Get date bounds
    await getSprintStartDateBounds(id);
    await getSprintEndDateBounds(id);
    await displayInlineAdvents();
}

/**
 * Get startDateBounds from java backend
 */
async function getSprintStartDateBounds(id) {
    const selectedEndDate =  document.getElementById("sprintEndDate-"+id).value;
    const response = await fetch("api/sprint/getStartDateBounds?sprintId=" + id + "&endDate=" + selectedEndDate)

    if (response.status === 200) {
        const results = await response.json();
        document.getElementById("sprintStartDate-"+id).setAttribute("min", results[0])
        document.getElementById("sprintStartDate-"+id).setAttribute("max", results[1])
    } else {
        console.log(response)
    }
}

/**
 * Get endDateBounds from java backend
 */
async function getSprintEndDateBounds(id) {
    const selectedStartDate =  document.getElementById("sprintStartDate-"+id).value;
    const response = await fetch("api/sprint/getEndDateBounds?sprintId=" + id + "&startDate=" + selectedStartDate)

    if (response.status === 200) {
        document.getElementById("sprintEndDate-"+id).setAttribute("min", results[0])
        document.getElementById("sprintEndDate-"+id).setAttribute("max", results[1])
    } else {
        console.log(response)
    }
}

/**
 * Get sprint header color by id
 */
function getBackgroundColour(index) {
    const colors = {0: "rgb(153, 0, 0)",
        1: "rgb(51, 102, 0)",
        2: "rgb(0, 51, 102)",
        3: "rgb(102, 0, 102)",
        4: "rgb(0, 102, 102)",
        5: "rgb(32, 32, 32)"}
    return colors[index % Object.keys(colors).length];
}