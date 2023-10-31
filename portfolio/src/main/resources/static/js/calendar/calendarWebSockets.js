//Stomp Client used for WebSockets
let stompClient = null;

/**
 * Connect to the WebSocket configuration setup on the backend server-side
 */
function connectWebsockets () {
    let socket = new SockJS("websocket"); //Setup WebSocket connection to the endpoint specified in the WebSocketConfig.java class
    stompClient = Stomp.over(socket); //Create a new StompClient object with the correct WebSocket endpoint
    stompClient.connect( {}, function(frame) { //Sets up callback function for when the stompClient connects to the endpoint
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
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventCreated", async function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventUpdated", function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/eventDeleted", function(payload) {
        window.location.reload()
    });
}


/**
 * Handles all the WebSocket endpoints and callback functions. Subscribes the user to all the ones needed for the deadlines and sets up the callback
 * functions to call when a message is received
 */
function subscribeToDeadlineWebSockets () {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineCreated", function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineUpdated", function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/deadlineDeleted", function(payload) {
        window.location.reload()
    });
}


function subscribeToMilestoneWebSockets () {
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneCreated", function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneUpdated", function(payload) {
        window.location.reload()
    });
    stompClient.subscribe("/websocketsReceive/project/" + projectId + "/milestoneDeleted", function(payload) {
        window.location.reload()
    });
}

window.onload = async function () {
    connectWebsockets();
}
