let editing = false;

/**
 * Open or close the project edit panel and update the date bounds
 */
function toggleProjectEdit() {
    const projectDetails = document.getElementById("projectDetails");
    const projectEditDetails = document.getElementById("projectEditDetails");
    const projectEditButton = document.getElementById("projectEditButton");

    editing = !editing;

    if (editing) {
        getProjectStartDateBounds();
        getProjectEndDateBounds();
    }

    projectDetails.hidden = editing;
    projectEditDetails.hidden = !editing;
    projectEditButton.hidden = editing;
}

/**
 * Save the project details in the backend
 */
async function saveProjectDetails() {
    const projectName = document.getElementById("projectName").value;
    const projectDescription = document.getElementById("projectDescription").value;
    const projectStartDate = document.getElementById("projectStartDate").value;
    const projectEndDate = document.getElementById("projectEndDate").value;

    if (projectName.length < 1) {
        // Show error on project name
        document.getElementById("projectName").classList.add("is-invalid");
        document.getElementById("projectName-feedback").innerText = "Project name is a required field."
        return
    } else {
        document.getElementById("projectName").classList.remove("is-invalid");
    }

    const body = {
        name: projectName,
        description: projectDescription,
        startDateString: projectStartDate,
        endDateString: projectEndDate
    }
    const updateResponse = await fetch("api/project/" + projectId + "/Update",
        {
            method: "POST",
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        }
    );

    if (updateResponse.status === 403) {
        showErrorToast("You must be a teacher to edit a project")
        return
    } else if (updateResponse.status === 400) {
        let content = (await updateResponse.json()).message
        console.log(content)

        if (content.startsWith("ProjectName")) {
            content = content.substring("ProjectName".length);
            document.getElementById("projectName").classList.add("is-invalid");
            document.getElementById("projectName-feedback").innerText = content;
            return
        } else {
            document.getElementById("projectName").classList.remove("is-invalid");
        }

        if (content.startsWith("ProjectStartDate")) {
            content = content.substring("ProjectStartDate".length);
            document.getElementById("projectStartDate").classList.add("is-invalid");
            document.getElementById("projectDateFeedback").innerText = content;
            document.getElementById("projectDateFeedback").hidden = false;
            return
        } else {
            document.getElementById("projectStartDate").classList.remove("is-invalid");
            document.getElementById("projectDateFeedback").hidden = true;
        }

        if (content.startsWith("ProjectEndDate")) {
            content = content.substring("ProjectEndDate".length);
            document.getElementById("projectEndDate").classList.add("is-invalid");
            document.getElementById("projectDateFeedback").innerText = content;
            document.getElementById("projectDateFeedback").hidden = false;
            return
        } else {
            document.getElementById("projectEndDate").classList.remove("is-invalid");
            document.getElementById("projectDateFeedback").hidden = true;
        }

        showErrorToast()
        return
    } else if (updateResponse.status !== 200) {
        showErrorToast()
        return
    }

    await updateProjectDetails()

    toggleProjectEdit()
}

/**
 * Update the UI details for the project
 */
async function updateProjectDetails() {
    // Get the new project details
    const projectDetailsResponse = await fetch("api/project/" + projectId + "/MinDetails")

    if (projectDetailsResponse.status !== 200) {
        showErrorToast()
        return
    }

    const projectDetails = await projectDetailsResponse.json();

    // Update the details
    const projectNameDetails = document.getElementById("projectNameDetails");
    const projectDescriptionDetails = document.getElementById("projectDescriptionDetails")
    const projectStartDetails = document.getElementById("projectStartDetails")
    const projectEndDetails = document.getElementById("projectEndDetails")

    projectNameDetails.innerText = projectDetails.name
    projectDescriptionDetails.innerText = projectDetails.description
    projectStartDetails.innerText = projectDetails.startDate
    projectEndDetails.innerText = projectDetails.endDate
}