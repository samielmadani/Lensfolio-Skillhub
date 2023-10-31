/**
 * Handles all Sprint behaviour on the FullCalendar calendar instance
 * Anything unique or isolated to sprints to do with FullCalendar should exist in here
 */

/**
 * Finds whether or not a sprint exists on the calendar on a particular day
 * @param day Date string of the day to search for
 * @returns {boolean} Returns true if there is no sprint on the passed in day, false otherwise
 */
function noSprintOnDay (day) {
    let dayDate = new Date(day)
    for (let i = 0; i < sprintEvents.length; i++) {
        if (new Date(sprintEvents[i].start) <= dayDate && new Date(sprintEvents[i].end)  > dayDate) return false
    }
    return true
}

/**
 * This function should only be called from the fullCalendar definition eventClick function.
 * This will handle what happens when a particular event is clicked
 * @param info FullCalendar EventAPI object passed in from fullcalendar
 */
async function sprintClicked (info) {
    const data = await fetch("api/getUserPermissions")
    if (await data.json() === false) return

    let e = calendar.getEvents();
    //reset all sprint styles to non-selected
    for (let i = 0; i < e.length; i++) {
        if (e[i]._def.extendedProps.type === "Sprint") {
            e[i].setProp("borderColor", "white");
            e[i].setProp("color", getBackgroundColour(e[i].id % 6));
        }
    }

    //Change the selected sprints background colour to be lighter and to have a
    //black border to tell that the sprint is selected
    const SCALE = 1.5;
    let backgroundColor = getBackgroundColour(info.event.id%6);
    backgroundColor = backgroundColor.split("(");
    backgroundColor = backgroundColor[1].split(")");
    backgroundColor = backgroundColor[0].split(",");
    backgroundColor = "rgb(" + (parseInt(backgroundColor[0]) * SCALE).toString() + "," + (parseInt(backgroundColor[1]) * SCALE).toString() + "," + (parseInt(backgroundColor[2]) * SCALE).toString() + ")"
    //apply the style changes to the sprint event in the calendar
    calendar.getEventById(info.event.id).setProp("color", backgroundColor);
    calendar.getEventById(info.event.id).setProp("borderColor", "black");

    //Change the selected sprint display in the html to show more details about the currently
    //selected sprint
    let selectedSprint = sprints[info.event.id];
    document.getElementById("sprint_label").innerText = selectedSprint[3];
    document.getElementById("sprint_name").innerText = selectedSprint[0];
    document.getElementById("sprint_dates").innerText = (new Date(selectedSprint[1])).toDateString() + " - " + (new Date(selectedSprint[2])).toDateString();
    document.getElementById("sprint_description").innerText = selectedSprint[4];

    calendar.render() //Rerender calendar with new changes
}

/**
 * Updates a sprint in the database after an event has been changed on the calendar
 * @param index Index that the sprint exists at on the Sprints passed in from thymeleaf and CalendarController
 * @param info FullCalendar EventAPI instance generated from FullCalendar event object
 * @param startDate new start date of the sprint in String format (must be Java compatible)
 * @param endDate new end date of the sprint in String format (must be Java compatible)
 * @returns {Promise<void>} null
 */
async function updateSprintInDB (index, info, startDate, endDate) {
    fetch("api/updateSprintCalendar?projectID=" + project[3]+"&sprintLabel="+sprints[index][3]+"&startDate="+startDate+"&endDate="+endDate,
        {method:"POST"}).then(response => {
        response.json().then(endDateInfo => {
            // Updates list of sprints with new edited sprint
            for (let sprint of sprints) {
                if (sprint[3].split(" ")[1] === (parseInt(info.event.id) + 1).toString()) {
                    sprint[1] = startDate;
                    sprint[2] = endDateInfo.split("T")[0];
                }
            }
        })
    });
}

/**
 * This function should only ever be called from FullCalendar eventResize function. This handles what should happen
 * after a user has resized a sprint
 * @param info FullCalendar EventAPI instance generated from FullCalendar event object
 * @returns {Promise<void>} null
 */
async function sprintTimeChanged(info){
    //Find the relevant sprint index in sprints from the sprints passed in by Thymeleaf
    let index = -1;
    for (let i = 0; i < sprints.length; i++) {
        if (sprints[i][0] === info.event._def.title) {
            index = i;
            break;
        }
    }
    //No Sprint was found and we should break out before it causes an error
    if (index === -1) {
        console.log("ERROR: No sprint found!");
        return;
    }

    //Get the start and end dates for the new event to be passed back to the controller
    //to store in the database and check for any errors
    let startDate = info.event._instance.range.start.toLocaleDateString("en-US");
    let endDate = info.event._instance.range.end.toLocaleDateString("en-US");
    let formattedEndDate = new Date(endDate)
    formattedEndDate.setDate(formattedEndDate.getDate() - 1)
    //Update the sprint start and end dates with the locally stored SprintEvents
    sprintEvents[index].start = startDate
    sprintEvents[index].end = endDate
    //Reset the advent classes to get correct positioning
    refetchCalendarEvents().then(calendar.render())
    //apply changes and pass back to controller using POST api call
    updateSprintInDB (index, info, startDate, endDate).then(() => {
        //Update the sprint selector panel with updated information
        document.getElementById("sprint_label").innerText = sprints[index][3];
        document.getElementById("sprint_name").innerText = sprints[index][0];
        document.getElementById("sprint_dates").innerText = (new Date(startDate)).toDateString() + " - " + formattedEndDate.toDateString()
        document.getElementById("sprint_description").innerText = sprints[index][4];
    })
    //Apply the changes to the frontend
    calendar.render()
}

/**
 * Adds all SprintEvents to the calendar in a FullCalendar friendly way
 * Initial Sprints are retrieved from the sprints array passed in from Thymeleaf located in calendar.html
 */
function addSprintEvents () {
    // Will stay false if user does not have edit access
    let arrows = false;

    sprintEvents = []

    for (let i = 0; i < sprints.length; i++) {
        let endDate = new Date(sprints[i][2])
        endDate.setDate(endDate.getDate() + 1)
        if (editable) { //If user has edit access
            sprintEvents.push( {
                title: sprints[i][0],
                start: sprints[i][1],
                end: endDate.toISOString().substring(0, 10),//to nz time
                allDay: true,
                borderColor: "white",
                color: getBackgroundColour(i%6),
                textColor: "rgb(255, 255, 255)",
                editable: false,
                className: "sprintEvent",
                resizableFromStart: true,
                durationEditable: true,
                type: "Sprint",
                id: i,
            });

            arrows = true;

        } else {
            sprintEvents.push( {
                title: sprints[i][0],
                start: sprints[i][1],
                end: endDate.toISOString().substring(0, 10),
                allDay: true,
                color: getBackgroundColour(i%6),
                textColor: "rgb(255, 255, 255)",
                editable: false,
                resizableFromStart: false,
                durationEditable: false,
                type: "Sprint",
                id: i,
            });

            arrows = false;
        }
    }

    // will remove the sprint calendar arrows if the user does not have edit access by removing the entire
    // arrows stylesheet
    if (arrows === false && document.querySelector('link[href$="sprintArrows.css"]')) {
        document.querySelector('link[href$="sprintArrows.css"]').remove();
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