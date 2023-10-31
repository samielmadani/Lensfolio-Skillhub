/**
 * Handles all Deadline behaviour on the FullCalendar calendar instance
 * Anything unique or isolated to deadlines to do with FullCalendar should exist in here
 */

/**
 * Finds if there is a FullCalendar Event of type "Deadline" that exists on a particular day
 * @param day Date ISOString date information representing the date to search
 * @returns {boolean} true if no deadline on that day, false otherwise
 */
function noDeadlineOnDay (day) {
    for (let i = 0; i < adventEvents.length; i++) {
        if (adventEvents[i].type === "Deadline") {
            if (day === adventEvents[i].start) return false
        }
    }
    return true
}


/**
 * Adds a Deadline to the calendar and formats into a FullCalendar EventAPI object
 * @param deadline JSON deadline returned from API request
 */
function addDeadlineToCalendar (deadline) {
    const date = new Date(deadline.startDate)
    date.setDate(date.getDate() + 1)

    let deadlineLocationOnCalendar = getEventLocationOnCalendar(date, "Deadline")
    if (deadlineLocationOnCalendar === -1) {
        //No deadline on the current day
        if (noSprintOnDay(date.toISOString().substring(0, 10))) {
            if (noEventOnDay(date.toISOString().substring(0, 10))) {
                //Handles CSS classes when there is nothing else on this day
                adventEvents.push({
                    title: "b",
                    start: date.toISOString().substring(0, 10),
                    type: "Deadline",
                    display: 'list-item',
                    className: ["deadlineCalendarNoSprint"],
                    numOccur: 1,
                    names: [deadline.name]
                })
            } else {
                //Handles CSS classes when there is only an event on this day
                adventEvents.push({
                    title: "b",
                    start: date.toISOString().substring(0, 10),
                    type: "Deadline",
                    display: 'list-item',
                    className: ["deadlineCalendarNoEvent"],
                    numOccur: 1,
                    names: [deadline.name]
                })
            }
        } else {
            if (noEventOnDay(date.toISOString().substring(0, 10))) {
                //Handles CSS classes when there is only a Sprint on this day
                adventEvents.push({
                    title: "b",
                    start: date.toISOString().substring(0, 10),
                    type: "Deadline",
                    display: 'list-item',
                    className: ["deadlineCalendarNoEvent"],
                    numOccur: 1,
                    names: [deadline.name]
                })
            } else {
                //Handles CSS classes when there is a Sprint and event on this day
                adventEvents.push({
                    title: "b",
                    start: date.toISOString().substring(0, 10),
                    type: "Deadline",
                    display: 'list-item',
                    className: [""],
                    numOccur: 1,
                    names: [deadline.name]
                })
            }
        }
    } else {
        //A Deadline already exists on this day, so update the information with this deadline
        adventEvents[deadlineLocationOnCalendar].numOccur += 1
        adventEvents[deadlineLocationOnCalendar].names.push(deadline.name)
    }
}

/**
 * Calls API GET request to DeadlineController to get all JSON deadlines for the current project
 * @returns {Promise<void>} null
 */
async function addDeadlines () {
    const response = await fetch ("api/project/"+project[3]+"/deadlinesJSON", {method: 'GET'})

    if (response.status === 200) {
        const responseBody = await response.json()
        //Add each deadline to the calendar
        responseBody.forEach (deadline => addDeadlineToCalendar(deadline))
    }
}