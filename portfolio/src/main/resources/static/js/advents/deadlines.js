/**
 * Add the text to an deadline that a user is editing an deadline
 */
function addDeadlineEditingAttribute (payload) {
    const data = JSON.parse(payload.body);
    document.getElementById("DeadlineCardUpdateText" + data.deadlineId).innerText = data.username + " is currently editing this deadline!"

    // Disable buttons
    toggleAdventButtons(data.deadlineId)
}

/**
 * Remove the text (if exists) that a user has finished editing an deadline
 */
function removeDeadlineEditingAttribute (payload) {
    // TODO: we also need to call this function if a user closes the page while editing an deadline
    const data = JSON.parse(payload.body)
    document.getElementById("DeadlineCardUpdateText" + data.deadlineId).innerText = ""

    // Enable buttons
    toggleAdventButtons(data.deadlineId)
}


/**
 * Handles the updating of an Deadline for the display
 */
function updateDeadlineOnPage (htmlText) {
    //Gets the deadlineID from the HTML text passed in
    const positionOfID = htmlText.indexOf("DeadlineCard")
    const deadlineId = htmlText.substring(positionOfID, htmlText.indexOf('"', positionOfID))
    //Get the old/existing deadline pane
    const oldDeadlineInfo = document.getElementById(deadlineId);
    oldDeadlineInfo.classList.remove("d-none"); //Reset deadline display
    oldDeadlineInfo.remove(); //Remove old pane
    insertAdvent(htmlText, "deadline", "getAllDeadlinesNoneFound",
        "DeadlineCard", "deadlines-pane") //Insert new deadline into correct place
}


/**
 * Remove a Deadline Pane from the page
 */
function deadlineDeleted (payload) {
    // TODO: we should check if the deadline was the last one on the page, and if so we should add back the message for 0 deadlines
    document.getElementById("DeadlineCard" + payload.body).remove();
}


//=================================================================================
//                                  API CALLS
//=================================================================================


/**
 * Makes a call to the API to get all the deadlines for the current project
 */
function getAllDeadlines () {
    const URL = "api/project/" + projectId + "/deadlines"
    fetch (URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                addAllDeadlines(htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to create a new deadline
 */
function createNewDeadline(data) {
    const URL = 'api/project/' + projectId + '/deadlines'
    fetch(URL, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the information from the API to all other users in the project
                stompClient.send("/websocketsSend/project/" + projectId + "/createDeadline", {}, htmlText);
            } else showErrorToast()
        });
}


/**
 * Makes a call to the API to delete an deadline
 */
function deleteDeadline(deadlineId) {
    const URL = 'api/project/' + projectId + '/deadlines/' + deadlineId;
    fetch(URL, {method: 'DELETE'})
        .then(response => {
            if (response.status === 200) { //All OK
                response.json().then(body => {
                    const messageURI = '/websocketsSend/project/' + projectId + '/deadlineDelete'
                    //Send the information from the API to all other users in the project
                    stompClient.send(messageURI, {}, JSON.stringify(body))
                })
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to update an deadline
 */
function updateDeadline(deadline, deadlineId) {
    const URL = "api/project/" + projectId + "/deadlines/" + deadlineId;
    fetch(URL, {method: "PUT", headers: {'Content-Type': 'application/json'}, body: JSON.stringify(deadline)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the updated information to all other users in the project
                stompClient.send("/websocketsSend/project/" + projectId + "/deadlineUpdate", {}, htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes an API call to request to update an deadline
 */
function requestUpdateDeadline(deadlineId) {
    //get the edit template from the API
    const URL = "api/project/" + projectId + "/deadlines/" + deadlineId + "/edit"
    fetch(URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                const deadlineCard = document.getElementById('DeadlineCard' + deadlineId); //Find the old deadline pane
                deadlineCard.insertAdjacentHTML("beforebegin", htmlText); //insert the new edit template where the old deadline pane is
                deadlineCard.classList.add("d-none"); //Hide the old Deadline Pane
                updateDeadlineCharInfoEdit() //Show the character left indicator
                const data = {"username": document.getElementById("userName").value, "deadlineId": deadlineId}
                //Send the information that someone is editing this deadline
                stompClient.send("/websocketsSend/project/" + projectId + "/startDeadlineEdit", {}, JSON.stringify(data));
            } else showErrorToast()
        })
}

//=================================================================================
//                               HTML Control
//=================================================================================

let deadlineFocus;

/**
 * This function is called when a deadline is hovered over/clicked on, to animate all similar deadline displays on the page,
 * and will also show the deadline timeline display viewer (blue line showing range of inline deadlines) as well as appropriate
 * dates.
 */
function animateAllDeadlineAttributes(deadlineId) {
    //Check if and deadline has previously been focused on, and that it's not this deadline
    if (deadlineFocus !== deadlineId && deadlineFocus) {
        //resets deadline focus information and stops previous deadlines animation
        stopAnimationForDeadline(deadlineFocus, true)
        deadlineFocus = null
    }

    //all deadline icons representing this particular deadline
    const deadlineDisplays = document.getElementsByName("DeadlineIcon" + deadlineId)
    //all deadline startDates representing this particular deadline
    const startDates = document.getElementsByName("startDate" + deadlineId)

    deadlineDisplays.forEach(deadlineDisplay => {
        //animate the icons
        deadlineDisplay.classList.add("deadlineAnimate")
        deadlineDisplay.style.transform = "scale(1.1)"
    })

    startDates.forEach(startDate => {
        //hide all start dates
        startDate.style.visibility = "hidden"
    })

    //all deadline inline displays representing this particular deadline
    const deadlinesInline = document.getElementsByName("DeadlinePartial" + deadlineId)
    //icon to be added in the middle of the sidebar line advent
    let icon = "<i class='bi bi-alarm-fill deadlineIcon'></>"
    displayAdventTimeline(deadlinesInline, "red", icon)

    let padding = document.getElementById("padding")
    padding.style.display = 'none'
    startDates.item(0).removeAttribute('hidden')
    startDates.item(0).style.visibility = "visible"
}


/**
 * Handler for when an deadline pane is clicked on to set deadline focus
 */
function persistDeadlineAnimations(deadlineId) {
    if (deadlineFocus === deadlineId) {
        deadlineFocus = null
        stopAnimationForDeadline(deadlineId)
    } else {
        deadlineFocus = deadlineId;
    }
}


/**
 * Stops the deadline animation and removes the deadline timeline viewer and start and end dates from the page. This will be called when a user stops
 * focusing on an deadline
 */
function stopAnimationForDeadline(deadlineId, forced = false) {
    if (!forced && deadlineFocus === deadlineId) return // This is to prdeadline the animation stopping prematurely, e.g. if a user clicks on an deadline and then the mouse leaves the deadline
    const deadlineDisplays = document.getElementsByName("DeadlineIcon" + deadlineId)
    deadlineDisplays.forEach(deadlineDisplay => {
        deadlineDisplay.classList.remove("deadlineAnimate")
        deadlineDisplay.style.transform = "scale(1)"
    })
    const deadlineTimeline = document.getElementById("adventTimelineDisplay")
    const startDates = document.getElementsByName("startDate" + deadlineId)

    deadlineTimeline.style.visibility = 'hidden'

    startDates.forEach(startDate => {
        startDate.style.visibility = "hidden"

    })
}


/**
 * Adds the HTML template for creating a new deadline to the page
 */
function createNewDeadlineTemplate() {
    if (document.getElementById("deadline-creation-pane").classList.contains("d-none")) {
        //Show the deadline creation template
        document.getElementById("deadline-creation-pane").className = "";
    } else {
        //Reset all the information on the deadline creation template.
        resetDeadlineCreationForm();
    }
    updateCharInfoDeadline()
}


/**
 * Remove Deadline confirmation
 */
function removeDeadline(deadlineId) {
    if (confirm("Are you sure you want to delete this Deadline?")) {
        deleteDeadline(deadlineId);
    }
}


/**
 * Resets the form for creating deadline. This allows us to reuse the same form for creating multiple deadlines
 */
function resetDeadlineCreationForm() {
    //Reset the validation
    document.getElementById("newDeadlineName").className = "form-control";
    document.getElementById("newDeadlineStartDate").className = "form-control";

    //Reset the values
    document.getElementById("newDeadlineName").value = "New Deadline";
    document.getElementById("newDeadlineStartDate").value = document.getElementById("newDeadlineStartDate").min;
    document.getElementById("newDeadlineStartTime").value = "00:00"

    //Reset the character indicator
    document.getElementById("charRemainingEventCreation").innerText = "38 Characters Remaining"
}


/**
 * Handles when the user wants to cancel creation of an deadline
 */
function cancelDeadlineCreate() {
    resetDeadlineCreationForm();
    //Hide the form
    document.getElementById("deadline-creation-pane").className = "d-none";
}


/**
 * Handles when a user wants to cancel updating an deadline
 */
function cancelDeadlineUpdate(deadlineId) {
    document.getElementById("DeadlineUpdate" + deadlineId).remove(); //Remove the editing HTML form
    document.getElementById("DeadlineCard" + deadlineId).classList.remove("d-none"); //Shows the old deadline data
    const data = {"username": document.getElementById("userName").value, "deadlineId": deadlineId}
    //Send message that the user is no longer editing the deadline
    stompClient.send("/websocketsSend/project/" + projectId + "/endDeadlineEdit", {}, JSON.stringify(data))
}

/**
 * Deadline update/create form general validation function. Ensures the Deadline name is not null, and the dates are valid
 */
function generalDeadlineFormValidation(deadlineNameID, startDateID) {
    let foundError = validateAdventNameInput(deadlineNameID)
    document.getElementById(startDateID).classList.add("is-valid")

    const startDateDOM = document.getElementById (startDateID)
    const startDate = createDateFromDateInput(startDateDOM);
    const errorMessageStart = document.getElementById("invalid-feedback-event-start");

    const inputStartDate = startDate.getTime() + 43200000;
    const minStartDate = Date.parse(startDateDOM.getAttribute("min"));
    const maxEndDate = Date.parse(startDateDOM.getAttribute("max"));

    //Ensures the start date is within the project range
    if (inputStartDate < minStartDate || maxEndDate < inputStartDate) {
        foundError = true;
        errorMessageStart.textContent  = "Date is out of project Range!";

        if (startDateDOM.classList.contains("is-valid")) startDateDOM.classList.remove("is-valid")
        startDateDOM.classList.add("is-invalid")
    } else {
        if (startDateDOM.classList.contains("is-invalid")) startDateDOM.classList.remove("is-invalid")
        startDateDOM.classList.add("is-valid")
    }
    return foundError
}


/**
 * Handles the validation for updating an deadline
 */
function validateDeadlineUpdateForm(deadlineId) {
    if (!generalDeadlineFormValidation("deadlineUpdateName", "deadlineUpdateStartDate")) {
        const deadlineData = {
            "name": document.getElementById("deadlineUpdateName").value,
            "startDate": createDateFromDateInput(document.getElementById("deadlineUpdateStartDate")),
            "startTime": document.getElementById("deadlineUpdateStartTime").value
        }
        updateDeadline(deadlineData, deadlineId);
        document.getElementById("DeadlineUpdate" + deadlineId).remove(); //Removes the Deadline Update form from the page
    }
}


/**
 * Handles the validation for creating an deadline
 */
function validateDeadlineCreationForm() {
    if (!generalDeadlineFormValidation("newDeadlineName", "newDeadlineStartDate")) {
        const deadlineData = {
            "projectId": projectId,
            "name": document.getElementById("newDeadlineName").value,
            "startDate": createDateFromDateInput(document.getElementById("newDeadlineStartDate")),
            "startTime": document.getElementById("newDeadlineStartTime").value
        }
        createNewDeadline(deadlineData);
        resetDeadlineCreationForm();
        document.getElementById("deadline-creation-pane").className = "d-none";
    }

}


/**
 * This should only be called when the order of the deadlines passed into the parameter htmlText is already correct, e.g. if the API has
 * already sorted the deadlines in order
 */
function addAllDeadlines(htmlText) {
    document.getElementById("deadlines-pane").innerHTML += htmlText;
}


function updateCharInfoDeadline() {
    document.getElementById("charRemainingDeadlineCreation").innerText = (50 - document.getElementById("newDeadlineName").value.length) + " Characters Remaining"
}

function updateDeadlineCharInfoEdit() {
    document.getElementById("charRemainingDeadlineEdit").innerText = (50 - document.getElementById("deadlineUpdateName").value.length) + " Characters Remaining"
}