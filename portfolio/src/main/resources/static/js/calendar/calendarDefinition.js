/***
 * MAIN DEFINITION FILE FOR FULL CALENDAR
 * Anything to do with the creation of the calendar, or anything related to the calendar but not specific to any
 * particular calendar event should exist in this file.
 */

let calendarEvents = [] //FullCalendar Event storage (not human readable)
let sprintEvents = [] //All Sprints in a human and calendar friendly format
let adventEvents = [] //All advents in a human and calendar friendly format
let calendar //FullCalendar instance
let calendarEl

//When the page is loaded
document.addEventListener('DOMContentLoaded', async function() {
    //get the HTML div that we want to place the element in
    calendarEl = document.getElementById('calendar');
    calendarEvents = await getCalendarEvents()
    //Settings for the calendar
    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        events: calendarEvents,
        eventResizableFromStart: true,
        timeZone: 'NZST',
        height: "auto",
        eventResize: async function (info) {
            //Called every time a FullCalendar event has been resized by a user
            await sprintTimeChanged(info);
        },
        eventClick: function (info) {
            //Called every time a FullCalendar event has been clicked on by a user
            if (info.event._def.extendedProps.type === "Sprint") sprintClicked (info);
        },
        eventOverlap: function (stillEvent, movingEvent) {
            //Called to resolve overlap conflicts. In our case, because we can only resize sprints and we need to make
            //sure sprints don't overlap, we need to test if the stillEvent in the overlap is not a sprint
            return stillEvent._def.extendedProps.type !== "Sprint"
        },
        validRange: {
            //Valid date range for the calendar
            start: project[1],
            end: project[2]
        },
        eventMouseEnter: function(info) {
            //Handles when the mouse enters a fullCalendar event
            let alert = document.getElementById("sprint_info"); //hover popup
            if (info.event._def.extendedProps.type === "Sprint") {
                //Shows the hover popup with the sprint information
                let eventObj = info.event;
                let d = new Date(eventObj.end);
                d.setDate(d.getDate() - 1);
                alert.innerText = eventObj.title + "\r\n\r\n" + eventObj.start.toDateString() + "  -  " + d.toDateString();
                alert.style.visibility = "visible";
            } else {
                //We assume that the user is hovering over one of the advents
                let nameDisplay = ""
                info.event._def.extendedProps.names.forEach (name => {
                    nameDisplay += name + "\r\n"
                })
                alert.innerText = nameDisplay
                alert.style.visibility = "visible"
            }
        },
        eventMouseLeave : function (info) {
            //Handles when the users mouse leaves a fullCalendar event
            //We want to hide the popup
            let alert = document.getElementById("sprint_info");
            alert.style.visibility = "hidden";
        },
        eventContent: function (arg, createElement) {
            //Called when the calendar renders, we want to customise the styles on the custom advent events to show
            //the icon and number of occurrences. Note we are using Bootstrap icons for this
            if (arg.event._def.extendedProps.type === "Event") {
                return {html: "<i class='bi bi-calendar' style='color: black;'> "+arg.event._def.extendedProps.numOccur+"</i>"}
            } else if (arg.event._def.extendedProps.type === "Deadline") {
                return {html: "<i class='bi bi-slash-circle-fill' style='color: black;'>"+arg.event._def.extendedProps.numOccur+"</i>"}
            } else if (arg.event._def.extendedProps.type === "Milestone") {
                return {html: "<i class='bi bi-flag' style='color: black;'>"+arg.event._def.extendedProps.numOccur+"</i>"}
            }
        }
    })

    //
    let date = localStorage.getItem('month')
    if (date) calendar.gotoDate(new Date(date))

    //After the calendar has been created, we need to render it
    calendar.render()
})

/**
 * Gets all events that should exist on the calendar
 * @returns {Promise<*[]>} FullCalendar EventAPI compatible JSON objects of calendar events
 */
async function getCalendarEvents () {
    adventEvents = [] //Reset all advent events that have previously existed. There isn't a way to change the html of a fullCalendar event after initial rendering
    addSprintEvents ()
    await getAdventEvents()
    return sprintEvents.concat(adventEvents)
}

/**Changes fullCalendar class name info for advents. This function should be called whenever a sprint length changes,
 * as it may affect the location and view of some advent events on the page.
 * The CSS classnames added to the events here can be found at sprintArrows.css
 */
async function refetchCalendarEvents () {
    let e = calendar.getEvents()
    for (let i = 0; i < e.length; i++) {
        //Change event info
        if (e[i]._def.extendedProps.type === "Event") {
            //Ensure event has the correct class name for positioning, in this case we only need to check if a sprint exists on the same day
            if (noSprintOnDay(new Date(e[i]._instance.range.start).toISOString().substring(0, 10))) {
                e[i].setProp("classNames", ["eventCalendar"])
            } else {
                e[i].setProp("classNames", [""])
            }
        } else if (e[i]._def.extendedProps.type === "Deadline") {
            //Ensure deadline has the correct class name for positioning, in this case we need to check if a sprint
            // exists on the same day, and if an event exists, or both, or none.
            if (noSprintOnDay(new Date(e[i]._instance.range.start).toISOString().substring(0, 10))) {
                if (noEventOnDay(new Date(e[i]._instance.range.start).toISOString().substring(0, 10))) {
                    e[i].setProp("classNames", ["deadlineCalendarNoSprint"])
                } else {
                    e[i].setProp("classNames", ["deadlineCalendarNoEvent"])
                }
            } else {
                if (noEventOnDay(new Date(e[i]._instance.range.start).toISOString().substring(0, 10))) {
                    e[i].setProp("classNames", ["deadlineCalendarNoEvent"])
                } else {
                    e[i].setProp("classNames", [""])
                }
            }
        } else if (e[i]._def.extendedProps.type === "Milestone") {
            //Ensure milestones have the correct classname for positioning, in this case we need to check if a sprint
            // exists, if an event exists, and if a deadline exists on the same day
            let dateStr = new Date(e[i]._instance.range.start).toISOString().substring(0, 10)
            if (noSprintOnDay(dateStr)) {
                if (noEventOnDay(dateStr)) {
                    if (noDeadlineOnDay(dateStr)) {
                        e[i].setProp("classNames", ["milestoneCalendarNoSprint"])
                    } else {
                        e[i].setProp("classNames", ["milestoneCalendarDeadline"])
                    }
                } else {
                    if (noDeadlineOnDay(dateStr)) {
                        e[i].setProp("classNames", ["milestoneCalendarDeadline"])
                    } else {
                        e[i].setProp("classNames",["milestoneCalendarNoDeadline"])
                    }
                }
            } else {
                if (noEventOnDay(dateStr)) {
                    if (noDeadlineOnDay(dateStr)) {
                        e[i].setProp("classNames", ["milestoneCalendarDeadline"])
                    } else {
                        e[i].setProp("classNames", ["milestoneCalendarNoDeadline"])
                    }
                } else {
                    if (noDeadlineOnDay(dateStr)) {
                        e[i].setProp("classNames", ["milestoneCalendarNoDeadline"])
                    } else {
                        e[i].setProp("classNames", [""])
                    }
                }
            }
        }
    }
}

/**
 * Gets the number of days that exist between two dates
 * @param start String representation of the start date
 * @param end String representation of the end date
 * @returns {number} int number of days that exist between start and end
 */
function daysBetween (start, end) {
    let startDate = new Date(start)
    let endDate = new Date(end)
    let difference = endDate.getTime() - startDate.getTime();
    return Math.ceil(difference / (1000 * 3600 * 24));
}


/**
 * Gets all the advents to add to the calendar
 */
async function getAdventEvents () {
    await addEvents ()
    await addDeadlines ()
    await addMilestones ()
}

/**
 * Gets the position that a certain advent event exists at on adventEvents.
 * @param startDate Date object that we are checking if the event exists at
 * @param type FullCalendar EventAPI object type defined in the creation of the event. Could be one of "Event", "Milestone", or "Deadline"
 * @returns {number} Index that the advent on the day exists at in the array adventEvents. -1 if not found
 */
function getEventLocationOnCalendar (startDate, type) {
    for (let i = 0; i < adventEvents.length; i++) {
        if (adventEvents[i].type === type && startDate.toISOString().substring(0, 10) === adventEvents[i].start) {
            return i;
        }
    }
    return -1;
}