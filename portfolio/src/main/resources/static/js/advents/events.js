/**
 * Add the text to an event that a user is editing an event
 */
function addEventEditingAttribute (payload) {
    const data = JSON.parse(payload.body);
    document.getElementById("EventCardUpdateText" + data.eventId).innerText = data.username + " is currently editing this event!"

    // Disable buttons
    toggleAdventButtons(data.eventId)
}

/**
 * Remove the text (if exists) that a user has finished editing an event
 */
function removeEventEditingAttribute (payload) {
    // TODO: we also need to call this function if a user closes the page while editing an event
    const data = JSON.parse(payload.body)
    document.getElementById("EventCardUpdateText" + data.eventId).innerText = ""

    // Enable buttons
    toggleAdventButtons(data.eventId)
}


/**
 * Handles the updating of an Event for the display
 */
function updateEventOnPage (htmlText) {
    //Gets the eventID from the HTML text passed in
    const positionOfID = htmlText.indexOf("EventCard")
    const eventId = htmlText.substring(positionOfID, htmlText.indexOf('"', positionOfID))
    //Get the old/existing event pane
    const oldEventInfo = document.getElementById(eventId);
    oldEventInfo.classList.remove("d-none"); //Reset event display
    oldEventInfo.remove(); //Remove old pane
    insertAdvent (htmlText, "event", "getAllEventsNoneFound",
        "EventCard", "events-pane") //Insert new event into correct place
}


/**
 * Remove a Event Pane from the page 
 */
function eventDeleted (payload) {
    // TODO: we should check if the event was the last one on the page, and if so we should add back the message for 0 events
    document.getElementById("EventCard" + payload.body).remove();
}


//=================================================================================
//                                  API CALLS
//=================================================================================


/**
 * Makes a call to the API to get all the events for the current project
 */
function getAllEvents () {
    const URL = "api/project/" + projectId + "/events"
    fetch (URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                addAllEvents(htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to create a new event
 */
function createNewEvent (data) {
    const URL = 'api/project/' +projectId+'/events'
    fetch (URL, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the information from the API to all other users in the project
                stompClient.send("/websocketsSend/project/" + projectId + "/createEvent", {}, htmlText);
            } else showErrorToast()
        });
}


/**
 * Makes a call to the API to delete an event
 */
function deleteEvent (eventId) {
    const URL = 'api/project/' +projectId+ '/events/' +eventId;
    fetch (URL, {method: 'DELETE'})
        .then(response => {
            if (response.status === 200) { //All OK
                response.json().then(body => {
                    const messageURI = '/websocketsSend/project/'+projectId+'/eventDelete'
                    //Send the information from the API to all other users in the project
                    stompClient.send(messageURI, {}, JSON.stringify(body))
                })
            } else showErrorToast()
        })
}


/**
 * Makes a call to the API to update an event
 */
function updateEvent (event, eventId) {
    const URL = "api/project/"+projectId+"/events/" + eventId;
    fetch (URL, {method: "PUT", headers: {'Content-Type': 'application/json'}, body: JSON.stringify(event)})
        .then(async response => {
            if (response.status === 201) { //All OK
                const htmlText = await response.text();
                //Send the updated information to all other users in the project
                stompClient.send("/websocketsSend/project/"+projectId+"/eventUpdate", {}, htmlText);
            } else showErrorToast()
        })
}


/**
 * Makes an API call to request to update an event
 */
function requestUpdateEvent (eventId) {
    //get the edit template from the API
    const URL = "api/project/"+projectId+"/events/"+eventId+"/edit"
    fetch (URL, {method: 'GET'})
        .then(async response => {
            if (response.status === 200) { //All OK
                const htmlText = await response.text();
                const eventCard = document.getElementById('EventCard' + eventId); //Find the old event pane
                eventCard.insertAdjacentHTML("beforebegin", htmlText); //insert the new edit template where the old event pane is
                eventCard.classList.add("d-none"); //Hide the old Event Pane
                updateEventCharInfoEdit() //show the character left indicator
                const data = {"username": document.getElementById("userName").value, "eventId": eventId}
                //Send the information that someone is editing this event
                stompClient.send("/websocketsSend/project/"+projectId+"/startEventEdit", {}, JSON.stringify(data));
            } else showErrorToast ()
        })
}


//=================================================================================
//                               HTML Control
//=================================================================================





let eventFocus;

/**
 * This function is called when an event is hovered over/clicked on, to animate all similar event displays on the page,
 * and will also show the event timeline display viewer (blue line showing range of inline events) as well as the appropriate
 * start and end dates.
 */
function animateAllEventAttributes (eventId) {
    //Check if and event has previously been focused on, and that it's not this event
    if (eventFocus !== eventId && eventFocus) {
        //resets event focus information and stops previous events animation
        stopAnimationForEvent(eventFocus, true)
        eventFocus = null
    }

    //all event icons representing this particular event
    const eventDisplays = document.getElementsByName("EventIcon" + eventId)
    const startDates = document.getElementsByName("startDate" + eventId)
    const endDates = document.getElementsByName("endDate" + eventId)


    const padding = document.getElementsByName("padding" + eventId)
    console.log(padding)
    if (padding[0] !== undefined) {
        padding[0].remove()
    }

    //checks if start date and end date are on the same line
    if (startDates.length === 1 && startDates[0].innerText !== "") {
        startDates[0].innerText = startDates[0].innerText + "-"
    }

    startDates.forEach (startDate => {
        //hide all start dates
        startDate.style.visibility = "hidden"
    })

    endDates.forEach (endDate => {
        //hide all end dates
        endDate.style.visibility = "hidden"
    })

    startDates.item(0).removeAttribute('hidden')
    endDates.item(endDates.length-1).removeAttribute('hidden')
    startDates.item(0).style.visibility = "visible"
    endDates.item(endDates.length-1).style.visibility = "visible"

    eventDisplays.forEach (eventDisplay => {
        //animate the icons
        eventDisplay.classList.add("eventAnimate")
        eventDisplay.style.transform = "scale(1.1)"

    })

    //all event inline displays representing this particular event
    const eventsInline = document.getElementsByName("EventPartial" + eventId)
    //icon to be added in the middle of the sidebar line advent
    let icon = "<i class='bi bi-calendar-fill'></>"
    displayAdventTimeline(eventsInline, "blue", icon)
}


/**
 * Handler for when an event pane is clicked on to set event focus
 */
function persistEventAnimations (eventId) {
    if (eventFocus === eventId) {
        eventFocus = null
        stopAnimationForEvent(eventId)
    } else {
        eventFocus = eventId;
    }
}


/**
 * Stops the event animation and removes the event timeline viewer as well as start and end dates from the page. This will be called when a user stops
 * focusing on an event
 */
function stopAnimationForEvent (eventId, forced=false) {
    if (!forced && eventFocus === eventId) return // This is to prevent the animation stopping prematurely, e.g. if a user clicks on an event and then the mouse leaves the event
    const eventDisplays = document.getElementsByName("EventIcon" + eventId)
    const startDates = document.getElementsByName("startDate"+ eventId)
    const endDates = document.getElementsByName("endDate"+ eventId)


    eventDisplays.forEach (eventDisplay => {
        eventDisplay.classList.remove("eventAnimate")
        eventDisplay.style.transform = "scale(1)"
    })
    const eventTimeline = document.getElementById("adventTimelineDisplay")
    eventTimeline.style.visibility = 'hidden'

    startDates.forEach (startDate => {
        startDate.style.visibility = "hidden"
    })

    endDates.forEach (endDate => {
        endDate.style.visibility = "hidden"

    })



}


/**
 * Adds the HTML template for creating a new event to the page
 */
function createNewEventTemplate() {
    if (document.getElementById("event-creation-pane").classList.contains("d-none")) {
        //Show the event creation template
        document.getElementById("event-creation-pane").className = "";
    } else {
        //Reset all the information on the event creation template.
        resetEventCreationForm();
    }
    updateEventCharInfo()

}


/**
 * Remove Event confirmation
 */
function removeEvent (eventId) {
    if (confirm("Are you sure you want to delete this Event?")) {
        deleteEvent(eventId);
    }
}


/**
 * Resets the form for creating event. This allows us to reuse the same form for creating multiple events
 */
function resetEventCreationForm () {
    //Reset the validation
    document.getElementById("newEventName").className = "form-control";
    document.getElementById("newEventStartDate").className = "form-control";
    document.getElementById("newEventEndDate").className = "form-control";

    //Reset the values
    document.getElementById("newEventName").value = "New Event";
    document.getElementById("newEventStartDate").value = document.getElementById("newEventStartDate").min;
    document.getElementById("newEventEndDate").value = document.getElementById("newEventEndDate").max;
    document.getElementById("newEventStartTime").value = "00:00"
    document.getElementById("newEventEndTime").value = "00:00"

    //Reset the character indicator
    document.getElementById("charRemainingEventCreation").innerText = "41 Characters Remaining"
}


/**
 * Handles when the user wants to cancel creation of an event
 */
function cancelEventCreate () {
    resetEventCreationForm();
    //Hide the form
    document.getElementById("event-creation-pane").className = "d-none";
}


/**
 * Handles when a user wants to cancel updating an event
 */
function cancelEventUpdate (eventId) {
    document.getElementById ("EventUpdate" + eventId).remove(); //Remove the editing HTML form
    document.getElementById("EventCard" + eventId).classList.remove("d-none"); //Shows the old event data
    const data = {"username": document.getElementById("userName").value, "eventId": eventId}
    //Send message that the user is no longer editing the event
    stompClient.send("/websocketsSend/project/" + projectId + "/endEventEdit", {}, JSON.stringify(data))
}




/**
 * Event update/create form general validation function. Ensures the Event name is not null, and the dates are valid
 */
function generalEventFormValidation (eventNameID, startDateID, endDateID, startTimeID, endTimeID) {
    let foundError = validateAdventNameInput (eventNameID)

    const startDateDOM = document.getElementById (startDateID)
    const endDateDOM = document.getElementById (endDateID)
    const startTimeDOM = document.getElementById (startTimeID)
    const endTimeDOM = document.getElementById (endTimeID)
    const startDate = createDateFromDateTimeInput(startDateDOM, startTimeDOM);
    const endDate = createDateFromDateTimeInput(endDateDOM, endTimeDOM);
    const errorMessageStart = document.getElementById("invalid-feedback-event-start");
    const errorMessageEnd = document.getElementById("invalid-feedback-event-end");

    const inputStartDate = startDate.getTime() + 43200000;
    const inputEndDate = endDate.getTime() + 43200000;
    const minStartDate = Date.parse(startDateDOM.getAttribute("min"));
    const maxEndDate = Date.parse(startDateDOM.getAttribute("max"));

    //Ensures the start date is after the end date
    if (startDate >= endDate) {
        foundError = true;
        if (startDateDOM.classList.contains("is-valid")) startDateDOM.classList.remove("is-valid")
        if (endDateDOM.classList.contains("is-valid")) endDateDOM.classList.remove("is-valid")
        startDateDOM.classList.add("is-invalid")
        endDateDOM.classList.add("is-invalid")

        errorMessageStart.textContent  = "Event Start Date must be before Event End Date!";
        errorMessageEnd.textContent  = "Event Start Date must be before Event End Date!";

    } else {
        if (startDateDOM.classList.contains("is-invalid")) startDateDOM.classList.remove("is-invalid")
        if (endDateDOM.classList.contains("is-invalid")) endDateDOM.classList.remove("is-invalid")
        startDateDOM.classList.add("is-valid")
        endDateDOM.classList.add("is-valid")


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

        //Ensures the end date is within the project range
        if (inputEndDate < minStartDate || maxEndDate < inputEndDate) {
            foundError = true;
            errorMessageEnd.textContent  = "Date is out of project Range!";

            if (endDateDOM.classList.contains("is-valid")) endDateDOM.classList.remove("is-valid")
            endDateDOM.classList.add("is-invalid")

        } else {
            if (endDateDOM.classList.contains("is-invalid")) endDateDOM.classList.remove("is-invalid")
            endDateDOM.classList.add("is-valid")
        }
    }

    return foundError
}


/**
 * Handles the validation for updating an event
 */
function validateUpdateForm (eventId) {
    if (!generalEventFormValidation("eventUpdateName", "eventUpdateStartDate", "eventUpdateEndDate", "eventUpdateStartTime", "eventUpdateEndTime")) {
        const eventData = {"name": document.getElementById("eventUpdateName").value, "startDate": createDateFromDateInput(document.getElementById("eventUpdateStartDate")),
                            "endDate": createDateFromDateInput(document.getElementById("eventUpdateEndDate")), "startTime": document.getElementById("eventUpdateStartTime").value,
                            "endTime": document.getElementById("eventUpdateEndTime").value}
        updateEvent(eventData, eventId);
        document.getElementById("EventUpdate" + eventId).remove(); //Removes the Event Update form from the page
    }
}



/**
 * Handles the validation for creating an event
 */
function validateCreationForm () {
    if (!generalEventFormValidation("newEventName", "newEventStartDate", "newEventEndDate", "newEventStartTime", "newEventEndTime")) {
        const eventData = {"projectId": projectId, "name": document.getElementById("newEventName").value, "startDate": createDateFromDateInput(document.getElementById("newEventStartDate")), 
                        "endDate": createDateFromDateInput(document.getElementById("newEventEndDate")), "startTime": document.getElementById("newEventStartTime").value, 
                        "endTime": document.getElementById("newEventEndTime").value}
        createNewEvent(eventData);
        resetEventCreationForm();
        document.getElementById("event-creation-pane").className = "d-none";
    }
}


/**
 * This should only be called when the order of the events passed into the parameter htmlText is already correct, e.g. if the API has 
 * already sorted the events in order
 */
function addAllEvents (htmlText) {
    document.getElementById("events-pane").innerHTML += htmlText;
}


function updateEventCharInfo () {
    document.getElementById("charRemainingEventCreation").innerText = (50 - document.getElementById("newEventName").value.length) + " Characters Remaining"
}

function updateEventCharInfoEdit () {
    document.getElementById("charRemainingEventEdit").innerText = (50 - document.getElementById("eventUpdateName").value.length) + " Characters Remaining"
}
