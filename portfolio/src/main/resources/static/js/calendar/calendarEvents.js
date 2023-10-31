/**
 * Handles all Event behaviour on the FullCalendar calendar instance
 * Anything unique or isolated to events to do with FullCalendar should exist in here
 */

/**
 * Finds if there is a FullCalendar Event of type "Event" that exists on a particular day
 * @param day Date ISOString date information representing the date to search
 * @returns {boolean} true if no event on that day, false otherwise
 */
function noEventOnDay (day) {
    for (let i = 0; i < adventEvents.length; i++) {
        if (adventEvents[i].type === "Event") {
            if (day === adventEvents[i].start) return false
        }
    }
    return true
}


/**
 * Adds an Event to the calendar and formats into a FullCalendar EventAPI object
 * @param event JSON event returned from EventController
 */
function addEventToCalendar (event) {
    const start = new Date(event.startDate)
    start.setDate(start.getDate() + 1)
    const end = new Date(event.endDate)
    end.setDate(end.getDate() + 2)

    //Go through each day that the event should be at
    let loopLocation = new Date (start)
    while (loopLocation < end) {

        let eventLocationOnCalendar = getEventLocationOnCalendar(loopLocation, "Event")
        if (eventLocationOnCalendar === -1) {
            //There is no event on the current day, so we have to create one
            if (noSprintOnDay(loopLocation.toISOString().substring(0, 10))) {
                //There is no sprint on the day, so we need to add the proper CSS class to format correctly on the display
                adventEvents.push({
                    title: "b",
                    start: loopLocation.toISOString().substring(0, 10),
                    type: "Event",
                    className: "eventCalendar",
                    display: 'list-item',
                    numOccur: 1,
                    names: [event.name]
                })
            } else {
                //A Sprint exists, so we don't need any extra CSS classes
                adventEvents.push({
                    title: "b",
                    start: loopLocation.toISOString().substring(0, 10),
                    type: "Event",
                    display: 'list-item',
                    className: "",
                    numOccur: 1,
                    names: [event.name]
                })
            }
        } else {
            //There is already an event on this day, so we just need to update the info with the current event
            adventEvents[eventLocationOnCalendar].numOccur += 1
            adventEvents[eventLocationOnCalendar].names.push(event.name)
        }
        //update loop location
        loopLocation = new Date(loopLocation.setDate (loopLocation.getDate () + 1))

    }
}


/**
 * Calls API GET request to EventController to get all JSON events for the current project
 * @returns {Promise<void>} null
 */
async function addEvents () {
    const response = await fetch( "api/project/"+project[3]+"/eventsJSON", {method: 'GET'})

    if (response.status === 200) {
        const responseBody = await response.json()
        //Add each event to the calendar
        responseBody.forEach (event => addEventToCalendar(event))
    }

}