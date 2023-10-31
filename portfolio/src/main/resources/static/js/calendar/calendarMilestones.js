/**
 * Handles all Milestone behaviour on the FullCalendar calendar instance
 * Anything unique or isolated to milestones to do with FullCalendar should exist in here
 */


/**
 * Adds a Milestone to the calendar and formats into a FullCalendar EventAPI object
 * @param milestone JSON milestone returned from API request
 */
function addMilestoneToCalendar (milestone) {
    const date = new Date(milestone.startDate)
    date.setDate(date.getDate() + 1)
    const dateStr = date.toISOString().substring(0, 10)

    let milestoneLocationOnCalendar = getEventLocationOnCalendar(date, "Milestone")
    if (milestoneLocationOnCalendar === -1) {
        //There is no Milestone that already exists on the current day
        if (noSprintOnDay(dateStr)) {
            if (noEventOnDay(dateStr)) {
                if (noDeadlineOnDay(dateStr)) {
                    //Format CSS class to handle positioning when it is the only object on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarNoSprint"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                } else {
                    //Format CSS class to handle positioning when there is only a deadline on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                }
            } else {
                if (noDeadlineOnDay(dateStr)) {
                    //Format CSS class to handle positioning when there is only an event on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                } else {
                    //Format CSS class to handle positioning when there is an event and deadline on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarNoDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                }
            }
        } else {
            if (noEventOnDay(dateStr)) {
                if (noDeadlineOnDay(dateStr)) {
                    //Format CSS class to handle positioning when there is only a sprint on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                } else {
                    //Format CSS class to handle positioning when there is a sprint and deadline on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarNoDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                }
            } else {
                if (noDeadlineOnDay(dateStr)) {
                    //Format CSS class to handle positioning when there is a sprint and event on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: ["milestoneCalendarNoDeadline"],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                } else {
                    //Format CSS class to handle positioning when there is a sprint, event, and deadline on this day
                    adventEvents.push({
                        title: "b",
                        start: date.toISOString().substring(0, 10),
                        type: "Milestone",
                        display: 'list-item',
                        className: [""],
                        numOccur: 1,
                        names: [milestone.name]
                    })
                }
            }
        }
    } else {
        //A milestone already exists on this day, so update the info with the current milestone
        adventEvents[milestoneLocationOnCalendar].numOccur += 1
        adventEvents[milestoneLocationOnCalendar].names.push(milestone.name)
    }
}

/**
 * Calls API GET request to MilestoneController to get all JSON milestones for the current project
 * @returns {Promise<void>} null
 */
async function addMilestones () {
    const response = await fetch ("api/project/"+project[3]+"/milestonesJSON", {method: 'GET'})

    if (response.status === 200) {
        const responseBody = await response.json()
        //Add each milestone to the calendar
        responseBody.forEach (milestone => addMilestoneToCalendar (milestone))
    }
}