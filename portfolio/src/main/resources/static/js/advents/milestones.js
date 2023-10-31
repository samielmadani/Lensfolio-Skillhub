/**
 * Add the text to an milestone that a user is editing an milestone
 */
function addMilestoneEditingAttribute (payload) {
    const data = JSON.parse(payload.body);
    document.getElementById("MilestoneCardUpdateText" + data.milestoneId).innerText = data.username + " is currently editing this milestone!"

    // Disable buttons
    toggleAdventButtons(data.milestoneId)
}

/**
 * Remove the text (if exists) that a user has finished editing an milestone
 */
function removeMilestoneEditingAttribute (payload) {
    // TODO: we also need to call this function if a user closes the page while editing an milestone
    const data = JSON.parse(payload.body)
    document.getElementById("MilestoneCardUpdateText" + data.milestoneId).innerText = ""

    // Enable buttons
    toggleAdventButtons(data.milestoneId)
}


/**
 * Handles the updating of an Milestone for the display
 */
function updateMilestoneOnPage (htmlText) {
    //Gets the milestoneID from the HTML text passed in
    const positionOfID = htmlText.indexOf("MilestoneCard")
    const milestoneId = htmlText.substring(positionOfID, htmlText.indexOf('"', positionOfID))
    //Get the old/existing milestone pane
    const oldMilestoneInfo = document.getElementById(milestoneId);
    oldMilestoneInfo.classList.remove("d-none"); //Reset milestone display
    oldMilestoneInfo.remove(); //Remove old pane
    insertAdvent (htmlText, "milestone", "getAllMilestonesNoneFound",
        "MilestoneCard", "milestones-pane") //Insert new milestone into correct place
}


/**
 * Remove a Milestone Pane from the page 
 */
function milestoneDeleted (payload) {
    // TODO: we should check if the milestone was the last one on the page, and if so we should add back the message for 0 milestones
    document.getElementById("MilestoneCard" + payload.body).remove();
}


//=================================================================================
//                                  API CALLS
//=================================================================================


/**
 * Makes a call to the API to get all the milestones for the current project
 */
function getAllMilestones () {
    const URL = "api/project/" + projectId + "/milestones"
    fetch (URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                addAllMilestones(htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to create a new milestone
 */
function createNewMilestone (data) {
    const URL = 'api/project/' +projectId+'/milestones'
    fetch (URL, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the information from the API to all other users in the project
                stompClient.send("/websocketsSend/project/" + projectId + "/createMilestone", {}, htmlText);
            } else showErrorToast()
        });
}


/**
 * Makes a call to the API to delete an milestone
 */
function deleteMilestone (milestoneId) {
    const URL = 'api/project/' +projectId+ '/milestones/' +milestoneId;
    fetch (URL, {method: 'DELETE'})
        .then(response => {
            if (response.status === 200) { //All OK
                response.json().then(body => {
                    const messageURI = '/websocketsSend/project/'+projectId+'/milestoneDelete'
                    //Send the information from the API to all other users in the project
                    stompClient.send(messageURI, {}, JSON.stringify(body))
                })
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to update an milestone
 */
function updateMilestone (milestone, milestoneId) {
    const URL = "api/project/"+projectId+"/milestones/" + milestoneId;
    fetch (URL, {method: "PUT", headers: {'Content-Type': 'application/json'}, body: JSON.stringify(milestone)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the updated information to all other users in the project
                stompClient.send("/websocketsSend/project/"+projectId+"/milestoneUpdate", {}, htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes an API call to request to update an milestone
 */
function requestUpdateMilestone (milestoneId) {
    //get the edit template from the API
    const URL = "api/project/"+projectId+"/milestones/"+milestoneId+"/edit"
    fetch (URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                const milestoneCard = document.getElementById('MilestoneCard' + milestoneId); //Find the old milestone pane
                milestoneCard.insertAdjacentHTML("beforebegin", htmlText); //insert the new edit template where the old milestone pane is
                milestoneCard.classList.add("d-none"); //Hide the old Milestone Pane
                updateMilestoneCharInfoEdit() //Set the character indicator
                const data = {"username": document.getElementById("userName").value, "milestoneId": milestoneId}
                //Send the information that someone is editing this milestone
                stompClient.send("/websocketsSend/project/"+projectId+"/startMilestoneEdit", {}, JSON.stringify(data));
            } else showErrorToast ()
        })
}


//=================================================================================
//                               HTML Control
//=================================================================================

let milestoneFocus;

/**
 * This function is called when an milestone is hovered over/clicked on, to animate all similar milestone displays on the page,
 * and will also show the milestone timeline display viewer (blue line showing range of inline milestones) as well as appropriate
 * start and end dates.
 */
function animateAllMilestoneAttributes (milestoneId) {
    //Check if and milestone has previously been focused on, and that it's not this milestone
    if (milestoneFocus !== milestoneId && milestoneFocus) {
        //resets milestone focus information and stops previous milestones animation
        stopAnimationForMilestone(milestoneFocus, true)
        milestoneFocus = null
    }

    //all milestone icons representing this particular milestone
    const milestoneDisplays = document.getElementsByName("MilestoneIcon" + milestoneId)
    //all milestone start dates representing this particular milestone
    const startDates = document.getElementsByName("startDate" + milestoneId)

    milestoneDisplays.forEach (milestoneDisplay => {
        //animate the icons
        milestoneDisplay.classList.add("milestoneAnimate")
        milestoneDisplay.style.transform = "scale(1.1)"
    })

    startDates.forEach (startDate => {
        startDate.style.visibility = "hidden"
    })

    //all milestone inline displays representing this particular milestone
    const milestonesInline = document.getElementsByName("MilestonePartial" + milestoneId)
    //icon to be added in the middle of the sidebar line advent
    let icon = "<i class='bi bi-flag-fill milestoneIcon'></>"
    displayAdventTimeline(milestonesInline, "green", icon)

    let padding = document.getElementById("padding")
    padding.style.display = 'none'
    startDates.item(0).removeAttribute('hidden')
    startDates.item(0).style.visibility = "visible"
}


/**
 * Handler for when an milestone pane is clicked on to set milestone focus
 */
function persistMilestoneAnimations (milestoneId) {
    if (milestoneFocus === milestoneId) {
        milestoneFocus = null
        stopAnimationForMilestone(milestoneId)
    } else {
        milestoneFocus = milestoneId;
    }
}


/**
 * Stops the milestone animation and removes the milestone timeline viewer as well as the start and end dates from the page. This will be called when a user stops
 * focusing on an milestone
 */
function stopAnimationForMilestone (milestoneId, forced=false) {
    if (!forced && milestoneFocus === milestoneId) return // This is to prmilestone the animation stopping prematurely, e.g. if a user clicks on an milestone and then the mouse leaves the milestone
    const milestoneDisplays = document.getElementsByName("MilestoneIcon" + milestoneId)
    const startDates = document.getElementsByName("startDate"+ milestoneId)

    milestoneDisplays.forEach (milestoneDisplay => {
        milestoneDisplay.classList.remove("milestoneAnimate")
        milestoneDisplay.style.transform = "scale(1)"
    })
    const milestoneTimeline = document.getElementById("adventTimelineDisplay")
    milestoneTimeline.style.visibility = 'hidden'

    startDates.forEach (startDate => {
        startDate.style.visibility = "hidden"

    })
}


/**
 * Adds the HTML template for creating a new milestone to the page
 */
function createNewMilestoneTemplate() {
    if (document.getElementById("milestone-creation-pane").classList.contains("d-none")) {
        //Show the milestone creation template
        document.getElementById("milestone-creation-pane").className = "";
    } else {
        //Reset all the information on the milestone creation template.
        resetMilestoneCreationForm();
    }
    updateMilestoneCharInfo()
}


/**
 * Remove Milestone confirmation
 */
function removeMilestone (milestoneId) {
    if (confirm("Are you sure you want to delete this Milestone?")) {
        deleteMilestone(milestoneId);
    }
}


/**
 * Resets the form for creating milestone. This allows us to reuse the same form for creating multiple milestones
 */
function resetMilestoneCreationForm () {
    //Reset the validation
    document.getElementById("newMilestoneName").className = "form-control";
    document.getElementById("newMilestoneStartDate").className = "form-control";

    //Reset the values
    document.getElementById("newMilestoneName").value = "New Milestone";
    document.getElementById("newMilestoneStartDate").value = document.getElementById("newMilestoneStartDate").min;

    //Reset the character indicator
    document.getElementById("charRemainingEventCreation").innerText = "37 Characters Remaining"
}


/**
 * Handles when the user wants to cancel creation of an milestone
 */
function cancelMilestoneCreate () {
    resetMilestoneCreationForm();
    //Hide the form
    document.getElementById("milestone-creation-pane").className = "d-none";
}


/**
 * Handles when a user wants to cancel updating an milestone
 */
function cancelMilestoneUpdate (milestoneId) {
    document.getElementById ("MilestoneUpdate" + milestoneId).remove(); //Remove the editing HTML form
    document.getElementById("MilestoneCard" + milestoneId).classList.remove("d-none"); //Shows the old milestone data
    const data = {"username": document.getElementById("userName").value, "milestoneId": milestoneId}
    //Send message that the user is no longer editing the milestone
    stompClient.send("/websocketsSend/project/" + projectId + "/endMilestoneEdit", {}, JSON.stringify(data))
}




/**
 * Milestone update/create form general validation function. Ensures the Milestone name is not null, and the dates are valid
 */
function generalMilestoneFormValidation (milestoneNameID, startDateID) {
    let foundError = validateAdventNameInput (milestoneNameID)

    const startDateDOM = document.getElementById (startDateID)
    const startDate = createDateFromDateInput(startDateDOM);
    const errorMessageStart = document.getElementById("invalid-feedback-milestone-start");

    const inputStartDate = startDate.getTime() + 43200000;
    const minStartDate = Date.parse(startDateDOM.getAttribute("min"));
    const maxEndDate = Date.parse(startDateDOM.getAttribute("max"));

    if (startDateDOM.classList.contains("is-invalid")) startDateDOM.classList.remove("is-invalid")
    startDateDOM.classList.add("is-valid")

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
 * Handles the validation for updating an milestone
 */
function validateMilestoneUpdateForm (milestoneId) {
    if (!generalMilestoneFormValidation("milestoneUpdateName", "milestoneUpdateStartDate", "milestoneUpdateEndDate", "milestoneUpdateStartTime", "milestoneUpdateEndTime")) {
        const milestoneData = {"name": document.getElementById("milestoneUpdateName").value, "startDate": createDateFromDateInput(document.getElementById("milestoneUpdateStartDate"))}
        updateMilestone(milestoneData, milestoneId);
        document.getElementById("MilestoneUpdate" + milestoneId).remove(); //Removes the Milestone Update form from the page
    }
}



/**
 * Handles the validation for creating an milestone
 */
function validateMilestoneCreationForm () {
    if (!generalMilestoneFormValidation("newMilestoneName", "newMilestoneStartDate", "newMilestoneEndDate", "newMilestoneStartTime", "newMilestoneEndTime")) {
        const milestoneData = {"projectId": projectId, "name": document.getElementById("newMilestoneName").value, "startDate": createDateFromDateInput(document.getElementById("newMilestoneStartDate"))}
        createNewMilestone(milestoneData);
        resetMilestoneCreationForm();
        document.getElementById("milestone-creation-pane").className = "d-none";
    }

}


/**
 * This should only be called when the order of the milestones passed into the parameter htmlText is already correct, e.g. if the API has 
 * already sorted the milestones in order
 */
function addAllMilestones (htmlText) {
    document.getElementById("milestones-pane").innerHTML += htmlText;
}


function updateMilestoneCharInfo () {
    document.getElementById("charRemainingMilestoneCreation").innerText = (50 - document.getElementById("newMilestoneName").value.length) + " Characters Remaining"
}

function updateMilestoneCharInfoEdit () {
    document.getElementById("charRemainingMilestoneEdit").innerText = (50 - document.getElementById("milestoneUpdateName").value.length) + " Characters Remaining"
}