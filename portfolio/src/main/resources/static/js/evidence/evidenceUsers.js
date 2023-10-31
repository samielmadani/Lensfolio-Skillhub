let URI;

function addUserToEvidence (userId) {
    let usersName = document.getElementById("user-result-" + userId).textContent
    document.getElementById("addUsersInput").value = ""
    document.getElementById("userSearchArea").innerHTML = ""
    if (!userIds.includes(userId) && !userNames.includes(usersName)) {
        userIds.push(userId)
        userNames.push(usersName)
    }
    document.getElementById("usersInEvidence").innerText = userNames.join(", ")
}

function closeUserDropdown () {
    //So that it isn't instant, we wait 100ms
    new Promise(resolve => setTimeout(resolve, 100)).then(() => {
        document.getElementById("userSearchArea").style.opacity = "0";
    })
}

function showUserDropdown () {
    document.getElementById("userSearchArea").style.opacity = "1";
}

function getUsers () {
    URI = "evidence/getUsers?query=" + document.getElementById("addUsersInput").value.replaceAll(/\W+/g, " ").replaceAll(" ", "%20")
    fetch (URI).then(response => {
        if (response.status === 200) {
            response.text().then(htmlText => {
                if (response.url.includes(URI)) {
                    document.getElementById("userSearchArea").innerHTML = htmlText
                }
            })
        }
    })
}

function resetUsers () {
    document.getElementById("addUsersInput").value = ""
    document.getElementById("userSearchArea").innerHTML = ""
    userIds = []
    userNames = []
    document.getElementById("usersInEvidence").innerText = ""
}