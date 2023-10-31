function displayAdventTimeline (adventsInline, color, icon) {
    let startHeight = null;
    let endHeight = null;

    //Find the min/max height of the event inline displays for a particular event
    adventsInline.forEach (adventDisplay => {
        const adventDisplayView = adventDisplay.getBoundingClientRect()
        if (!startHeight) startHeight = adventDisplayView.top
        if (startHeight > adventDisplayView.top) startHeight = adventDisplayView.top
        if (!endHeight) endHeight = adventDisplayView.bottom
        if (endHeight < adventDisplayView.bottom) endHeight = adventDisplayView.bottom
    })

    const eventTimeline = document.getElementById("adventTimelineDisplay")
    eventTimeline.style.backgroundColor = color
    eventTimeline.style.visibility = 'visible'
    eventTimeline.style.height = (endHeight - startHeight) + "px" //Set height of event viewer
    //Set location of viewer from the top of the page
    eventTimeline.style.marginTop = (startHeight - document.getElementById("timelineContainer").getBoundingClientRect().top) + "px"
    // Adding icon to bar
    eventTimeline.innerHTML = icon
}


/**
 * Makes an API call to request all the date ranges in the project
 */
async function fetchDateRanges () {
    const URL = "api/project/" + projectId + "/ranges"
    const response = await fetch (URL, {method: 'GET'})
    if (response.status === 200) {
        return await response.json()
    } else showErrorToast ()
    return {}
}


async function getInlineAdvents (start, end) {
    const URL = "api/project/" +projectId+ "/adventsRange?start=" + start + "&end=" + end
    const response = await fetch (URL, {method: 'GET'})
    if (response.status === 200) {
        return await response.text();
    } else showErrorToast ()
    return null
}


async function displayInlineAdvents () {
    // document.getElementsByName("inlineAdvents").forEach(e => e.remove())

    let inlineAdvents = document.getElementsByName("inlineAdvents")
    for (let i = inlineAdvents.length-1; i>= 0; i--) {
        inlineAdvents[i].parentNode.removeChild(inlineAdvents[i])
    }

    const ranges = await fetchDateRanges()
    for (let i = 0; i < ranges.length; i++) {
        let range = ranges[i]
        const startDate = new Date(range.start.toLocaleString("en-US", {timeZone: 'Pacific/Auckland'})).getTime()
        const endDate = new Date(range.end.toLocaleString("en-US", {timeZone: 'Pacific/Auckland'})).getTime()
        const htmlText = await getInlineAdvents (startDate, endDate)
        //insert in the correct location
        while(!document.getElementById(range.location)) { //Wait until the element exists on the page if it doesn't already
            await new Promise(r => setTimeout(r, 500));
        }
        document.getElementById(range.location).innerHTML = htmlText;
    }
    //Add the colours of the sprints that the events are in to each advent pane
    addSprintColoursToAdventCards ()
}

/**
 * Adds the colours of the sprints that the advents are in to the advent pane so that the user can tell what sprint(s)
 * they belong to
 */
function addSprintColoursToAdventCards () {
    //Event pane sprint colours
    document.getElementsByName("EventPane").forEach(e => {
        resetAdventPaneTextColour("EventStart" + e.dataset.id, "", "normal")
        resetAdventPaneTextColour("EventEnd" + e.dataset.id, "", "normal")
        addAdventPaneColourAttributes(e.dataset.id, "Event")
    })

    //Deadline pane sprint colours
    document.getElementsByName("DeadlinePane").forEach(e => {
        resetAdventPaneTextColour("DeadlineStart" + e.dataset.id, "", "normal")
        addAdventPaneColourAttributes(e.dataset.id, "Deadline")

    })

    //Milestone pane sprint colours
    document.getElementsByName("MilestonePane").forEach(e => {
        resetAdventPaneTextColour("MilestoneStart" + e.dataset.id, "", "normal")
        addAdventPaneColourAttributes(e.dataset.id, "Milestone")

    })
}

/**
 * Adds the sprint colours that an advent belongs to the advent pane
 * @param adventId ID of the advent
 * @param type Type of advent (one of: "Event", "Milestone", "Deadline")
 */
function addAdventPaneColourAttributes (adventId, type) {
    matchAdventColour(adventId, type)
    //Clear any previous advent pane sprint colours
    document.getElementById(type + "SprintColours" + adventId).innerHTML = ""
    //Get all locations of the advent with the id on the page
    let elements = document.getElementsByName (type + "Icon" + adventId)
    elements.forEach (element => {
        let parentElement = element.parentElement
        //Goes up the hierarchy until a parent element with an ID is found, or until the top of the DOM is reached
        while (parentElement.id === "" && parentElement) {
            parentElement = parentElement.parentElement
        }
        //Checks if the parentElement exists
        if (!parentElement) return
        //Tests if the parentElement is inside a sprint
        if (parentElement.id.slice(0, 11) === "InnerSprint") {
            //If the element is inside a sprint, then we want to add that sprint colour to the advent pane
            document.getElementById(type + "SprintColours" + adventId).innerHTML += "<div style='display: inline-block; width: 15px; height: 15px; margin-right: 5px; border-radius: 50%; background-color: " + getBackgroundColour(parseInt(parentElement.id.slice(11))) + ";'></div>"
        }
    })
}

/**
 * Match the advent colour with the sprints colour
 * @param adventId - Id of the advent
 * @param type - type of the advent (event, deadline, milestone)
 */
function matchAdventColour(adventId, type) {
    let elements = document.getElementsByName(type+"Icon" + adventId)
    let relevantElements = (elements.length === 2) ? [elements[1]] : [elements[1], elements[elements.length-1]]
    for (let i=0; i<relevantElements.length; i++) {
        let element = relevantElements[i]
        let parentElement = element.parentElement
        //Goes up the hierarchy until a parent element with an ID is found, or until the top of the DOM is reached
        while (parentElement.id === "" && parentElement) {
            parentElement = parentElement.parentElement
        }

        //Tests if the parentElement is inside a sprint
        if (parentElement && parentElement.id.slice(0, 11) === "InnerSprint") {
            //If the element is inside a sprint, then we want to add that sprint colour to the advent pane
            let backgroundColor = getBackgroundColour(parseInt(parentElement.id.slice(11)))
            let adventName = element.parentElement.getElementsByTagName("p")[0]
            adventName.style.color = backgroundColor
            adventName.style.fontWeight = 'bold'
            if (relevantElements.length === 1) {
                resetAdventPaneTextColour(type+"Start" + adventId, backgroundColor, 'bold')
                if (type === "Event") {
                    resetAdventPaneTextColour(type + "End" + adventId, backgroundColor, 'bold')
                }
            } else if (i===0) {
                resetAdventPaneTextColour(type+"Start" + adventId, backgroundColor, 'bold')
            } else {
                resetAdventPaneTextColour(type+"End" + adventId, backgroundColor, 'bold')
            }
        }
    }
}

/**
 * Reset the Advent text colour
 * @param adventTextId - advent text Id
 * @param color - string for text colour
 * @param style - string for text style
 */
function resetAdventPaneTextColour(adventTextId, color, style) {
    let adventText = document.getElementById(adventTextId)
    adventText.style.color = color
    adventText.style.fontWeight = style
}


function insertAdvent (htmlText, type, emptyAdventMessageID, adventCardIDPrefix, adventPaneID) {
    //First, hide the text that says there are no advents, as we are adding an advent
    if (document.getElementById(emptyAdventMessageID)) document.getElementById(emptyAdventMessageID).innerText = ""
    //Remove the buttons if the user is not an admin
    if (document.getElementById ("userIsAdmin").getAttribute("value") === "false") {
        let beginHtml = htmlText.substring(0, htmlText.indexOf("<!-- Edit/Delete Buttons Start -->"))
        let endHtml = htmlText.substring(htmlText.indexOf("<!-- Edit/Delete Buttons Finish -->"), htmlText.length)
        htmlText = beginHtml + endHtml
    }
    //Start date of the new advent we are adding to the page
    const startDate = new Date(htmlText.substr(htmlText.indexOf('data-start="') + 12, 16))
    //Get all advents of this type in a list
    const advents = document.getElementsByClassName(type);
    for (let i = 0; i < advents.length; i++) {
        //Go through each of this type advent on the page and find the location that this new advent should be placed at
        if (startDate < new Date(advents[i].dataset.start)) {
            document.getElementById(adventCardIDPrefix + advents[i].dataset.id).insertAdjacentHTML('beforebegin', htmlText);
            return;
        }
    }

    //This advent should be after all other advents on the page (if any)
    document.getElementById(adventPaneID).innerHTML += htmlText;
}


function validateAdventNameInput (adventNameID) {
    let foundError = false
    const adventName = document.getElementById(adventNameID);
    if (adventName.value.replace(/\s/g, '') === '') {
        foundError = true

        //Set the name element to be invalid, bootstrap will handle error messages automatically
        if (adventName.classList.contains("is-valid")) adventName.classList.remove("is-valid")
        adventName.classList.add("is-invalid")
    } else {
        //Set the name element to be valid, bootstrap will remove error messages automatically
        if (adventName.classList.contains("is-invalid")) adventName.classList.remove("is-invalid")
        adventName.classList.add("is-valid")
    }
    return foundError
}


/**
 * Uses date and time information to create a single date-time object of both the information
 */
function createDateFromDateTimeInput (date, time) {
    const dateValues = date.value.split("-");
    const timeValues = time.value.split(":");
    return new Date(dateValues[0], dateValues[1] - 1, dateValues[2], timeValues[0], timeValues[1]);
}


/**
 * Create proper date object from input date string created from HTML date input
 */
function createDateFromDateInput (date) {
    const dateValues = date.value.split("-");
    return new Date(dateValues[0], dateValues[1] - 1, dateValues[2])
}