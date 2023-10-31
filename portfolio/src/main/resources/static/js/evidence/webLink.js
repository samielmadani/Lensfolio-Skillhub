let userViewingId
let webLinkList = [];
let URLError = false
let InputError = true;

function validateURL() {
    let foundError = false;

    const webLinkInput = document.getElementById ("newWebLink");

    if (webLinkInput.value === "") {
        document.getElementById("webLink-creation-button").disabled = true;
        if (webLinkInput.classList.contains("is-invalid")) webLinkInput.classList.remove("is-invalid")
        if (webLinkInput.classList.contains("is-valid")) webLinkInput.classList.remove("is-valid")

    } else {
        if (webLinkList.length === 10) {
            foundError = true;
            if (webLinkInput.classList.contains("is-valid")) webLinkInput.classList.remove("is-valid")
            webLinkInput.classList.add("is-invalid")
            document.getElementById("webLinkErrorMessage").textContent = "Web link limit reached!";
        } else if (webLinkList.includes(webLinkInput.value)) {
            foundError = true;
            if (webLinkInput.classList.contains("is-valid")) webLinkInput.classList.remove("is-valid")
            webLinkInput.classList.add("is-invalid")
            document.getElementById("webLinkErrorMessage").textContent = "This Web Link has already been added!";
        } else if (!isURL(webLinkInput.value)) {
            foundError = true;
            if (webLinkInput.classList.contains("is-valid")) webLinkInput.classList.remove("is-valid")
            webLinkInput.classList.add("is-invalid")
            document.getElementById("webLinkErrorMessage").textContent = "Invalid URL!";

        } else {
            if (webLinkInput.classList.contains("is-invalid")) webLinkInput.classList.remove("is-invalid")
            webLinkInput.classList.add("is-valid")

        }
        document.getElementById("webLink-creation-button").disabled = foundError;

    }

    URLError = foundError
    canCreate()
    return foundError;
}

function canCreate() {
    document.getElementById("evidence-creation-button").disabled = !(!URLError && !InputError);
    document.getElementById("evidence-edit-button").disabled = !(!URLError && !InputError);

}



function isURL(str) {

    let urlRegex = "(https?:\\/\\/www\\.[a-zA-Z\\d@:%._\\+\\-~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)|https?:\\/\\/[^w.]([a-zA-Z\\d@:%._\\+\\-~#=]){2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*))";
    let url = new RegExp(urlRegex, 'i');
    return str.length < 2083 && url.test(str);
}

/**
 * Adds a weblink to the list of weblinks to be added to the piece of evidence.
 */
async function addWebLink() {

    const node = document.getElementById("newWebLink");
    let list = document.getElementById("webLinkList");

    node.addEventListener("keyup", function(event) {
        validateURL()
        if (event.key === "Enter") {
            if (!validateURL()) {
                if (node.value !== "" && !webLinkList.includes(node.value)) {
                    webLinkList.unshift(node.value);
                    let li = document.createElement("li");
                    let a = document.createElement("a");
                    a.href = "#";
                    a.onclick = removeWebLink;
                    a.textContent = node.value;
                    a.className = "listElement";

                    li.appendChild(a);
                    list.prepend(li);
                    cancelNewWebLinkTemplate();
                    node.value = "";
                }
            }
        }
    });
}

function handleNewWebLink(){
    const node = document.getElementById("newWebLink");
    validateURL()

    if (node.value !== "" && !webLinkList.includes(node.value)) {
        createWebLinkElement(node.value)
        cancelNewWebLinkTemplate();
        node.value = "";
    }
}

/**
 * Create a new weblink HTML element
 * @param newLink string representing the new link URL
 */
function createWebLinkElement(newLink) {
    let list = document.getElementById("webLinkList");

    // Add to the webLinkList
    webLinkList.unshift(newLink);

    // Create a new list element
    let li = document.createElement("li");
    let a = document.createElement("a");

    // Create a new link item
    a.href = "#";
    a.onclick = removeWebLink;
    a.textContent = newLink;
    a.className = "listElement";

    // Add to the list
    li.appendChild(a);
    list.prepend(li);
}

function removeWebLink() {
    this.remove();
    remove(webLinkList, this.textContent)
}

function resetWeblinks() {
    document.getElementById("webLinkList").innerHTML = "";
}

async function createWebLinks(evidenceId, webLinkString) {
    await fetch("api/evidence/" + evidenceId + "/webLink?weblink=" + webLinkString, { method: "PUT",})
}

function createNewWebLinkTemplate() {
    validateURL()
    if (document.getElementById("web-link-creation").classList.contains("d-none")) {
        document.getElementById("web-link-creation").className = "";
    } else {
        cancelNewWebLinkTemplate();
    }
}

/**
 * Handles when the user wants to cancel creation of a weblink
 */
function cancelNewWebLinkTemplate() {
    //Hide the form
    document.getElementById("newWebLink").value = "";

    document.getElementById("web-link-creation").className = "d-none";
}