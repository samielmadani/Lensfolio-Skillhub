//Stomp Client used for WebSockets
let stompClient = null;

/**
 * Connect to the WebSocket configuration setup on the backend server-side
 */
function connectWebsockets () {
    let socket = new SockJS("websocket"); //Setup WebSocket connection to the endpoint specified in the WebSocketConfig.java class
    stompClient = Stomp.over(socket); //Create a new StompClient object with the correct WebSocket endpoint
    stompClient.connect( {}, function() { //Sets up callback function for when the stompClient connects to the endpoint
        subscribeToEventWebSockets();
        subscribeToDeadlineWebSockets();
        subscribeToMilestoneWebSockets();
    });
}

/**
 * Handles all the WebSocket endpoints and callback functions. Subscribes the user to all the ones needed for the events and sets up the callback
 * functions to call when a message is received
 */
function subscribeToEventWebSockets () {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventCreated", function(payload) {
        insertAdvent (payload.body.toString(), "event", "getAllEventsNoneFound", "EventCard", "events-pane")
        displayInlineAdvents ()
    });
    if (isAdmin) {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventBeingEdited", function(payload) {
        addEventEditingAttribute(payload);
    });

    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventFinishedEdit", function(payload) {
        removeEventEditingAttribute(payload);
    });
    }
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventUpdated", function(payload) {
        updateEventOnPage (payload.body.toString());
        displayInlineAdvents ()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventDeleted", function(payload) {
        eventDeleted(payload);
        displayInlineAdvents ()
    });
}


/**
 * Handles all the WebSocket endpoints and callback functions. Subscribes the user to all the ones needed for the deadlines and sets up the callback
 * functions to call when a message is received
 */
function subscribeToDeadlineWebSockets () {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineCreated", function(payload) {
        insertAdvent(payload.body.toString(), "deadline", "getAllDeadlinesNoneFound", "DeadlineCard", "deadlines-pane")
        displayInlineAdvents ()
    });
    if (isAdmin) {
        stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineBeingEdited", function(payload) {
            addDeadlineEditingAttribute(payload);
        });
        stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineFinishedEdit", function(payload) {
            removeDeadlineEditingAttribute(payload);
        });
    }
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineUpdated", function(payload) {
        updateDeadlineOnPage (payload.body.toString());
        displayInlineAdvents ()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineDeleted", function(payload) {
        deadlineDeleted(payload);
        displayInlineAdvents ()
    });
}


function subscribeToMilestoneWebSockets () {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneCreated", function(payload) {
        insertAdvent(payload.body.toString(), "milestone", "getAllMilestonesNoneFound", "MilestoneCard", "milestones-pane")
        displayInlineAdvents ()
    });
    if (isAdmin) {
        stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneBeingEdited", function(payload) {
            addMilestoneEditingAttribute(payload);
        });
        stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneFinishedEdit", function(payload) {
            removeMilestoneEditingAttribute(payload);
        });
    }
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneUpdated", function(payload) {
        updateMilestoneOnPage (payload.body.toString());
        displayInlineAdvents ()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneDeleted", function(payload) {
        milestoneDeleted(payload);
        displayInlineAdvents ()
    });
}

window.onload = async function () {
    connectWebsockets();
    getAllEvents();
    getAllDeadlines();
    getAllMilestones();

    await displayInlineAdvents();
}

/**
 * Toggle the advent buttons as disabled/enabled
 */
function toggleAdventButtons(id) {
    const editButton = document.getElementById(id + "-editButton");
    const deleteButton = document.getElementById(id + "-deleteButton")

    editButton.disabled = !editButton.disabled;
    deleteButton.disabled = !deleteButton.disabled;
}