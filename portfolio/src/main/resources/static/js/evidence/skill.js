skillCreationList = []

/**
 * Validates skill inputs are correct. Checks for valid characters and length.
 */
function validateSkill() {
    let foundError = false;

    const skillInput = document.getElementById("newSkillName");
    const regex = new RegExp("^[A-Za-z0-9_-]*$", 'i');
    let skill = skillInput.value;
    if (skill.endsWith(" ")) skill = skill.slice(0, -1);

    if (skill === "") {
        if (skillInput.classList.contains("is-invalid")) skillInput.classList.remove("is-invalid")
        if (skillInput.classList.contains("is-valid")) skillInput.classList.remove("is-valid")
    } else {
        if (!regex.test(skill.value)) {
            foundError = true;
            if (skillInput.classList.contains("is-valid")) skillInput.classList.remove("is-valid")
            skillInput.classList.add("is-invalid")

            document.getElementById("skillErrorMessage").textContent = "Tag is too long!";
        } else if (!regex.test(skill)) {
            foundError = true
            if (skillInput.classList.contains("is-valid")) skillInput.classList.remove("is-valid")
            skillInput.classList.add("is-invalid")

            document.getElementById("skillErrorMessage").textContent = "Tag must only contain letters, numbers, hyphens or underscores!";
        } else if (skill.toUpperCase() === "NO_SKILLS") {
            foundError = true
            if (skillInput.classList.contains("is-valid")) skillInput.classList.remove("is-valid")
            skillInput.classList.add("is-invalid")

            document.getElementById("skillErrorMessage").textContent = "Cannot use skill '" + skill + "'";
        } else {
            if (skillInput.classList.contains("is-invalid")) skillInput.classList.remove("is-invalid")
            skillInput.classList.add("is-valid")
        }
    }

    return foundError
}

function validateEditedSkill(skill) {

    let foundError = false;
    const regex = new RegExp("^[A-Za-z0-9_-]*$", 'i');

    if (skill.endsWith(" ")) skill = skill.slice(0, -1);
    if (skill == "" || skill == "") {
        foundError = true;
    } else {
        if (!regex.test(skill.value)) {
            foundError = true;

            // document.getElementById("skillErrorMessage").textContent = "Tag is too long!";
        } else if (!regex.test(skill)) {
            foundError = true;
            // document.getElementById("skillErrorMessage").textContent = "Tag must only contain letters, numbers, hyphens or underscores!";
        } else if (skill.toUpperCase() === "NO_SKILLS") {
            foundError = true;
            // document.getElementById("skillErrorMessage").textContent = "Cannot use skill '" + skill + "'";
        }
    }

    return foundError
}

async function createSkills(evidenceId, skillString) {
    //Add new skill tag to user tag area if one doesn't already exist :3
    if (!document.getElementById("skill-" + skillString.toLowerCase())) {
        document.getElementById ("evidence-skill-area").innerHTML += ' <ol style="display: inline-flex; margin-bottom: 0; padding-left: 0;"> <a id="skill-'+skillString.toLowerCase()+'" class="pill" style="color: black; text-decoration: none;" href="evidenceSkill?skill='+skillString+'">'+skillString+'</a> </ol>'
    }
    await fetch("api/evidence/" + evidenceId + "/skill?skill=" + skillString, { method: "PUT",})
}

function removeSkillChip(caller) {
    const skill = caller.innerText

    const skillEdit = document.getElementById(caller.innerText + "-button")
    caller.remove()
    skillEdit.remove()
    caller.remove()

    skillCreationList = skillCreationList.filter((val) => val !== skill)

    removeLeftOverSkill(skill);

}

function removeLeftOverSkill(skill) {

    const leftOver = document.getElementById(skill + "-chip")
    leftOver.remove()

}

function addSkillChip(skill) {
    enableButtons()
    const container = document.getElementById("skillTagsArea")
    container.innerHTML += "<div style='display: flex' class='whole-tag'><div id='" + skill + "-chip' class='chip chipWithButton delete-icon' onclick='removeSkillChip(this)'><i class='bi bi-x delete-icon'></i>" + skill + "</div> <button id='" + skill + "-button' type='button' class='btn-edit' onclick='editChip(this)'><i class='bi bi-pencil-square'></i></button></div>"
    skillCreationList.push(skill)
}

function editChip(chip) {
    const chipName = chip.id.replace('-button','');
    const container = document.getElementById(chipName + "-chip");
    const text = container.innerText;

    container.removeAttribute("onclick");

    container.innerText = "";
    container.innerHTML += "<input id='" + text + "-input' oninput='oninputValidate(this)' type='text' class='edit-input' value=" + text + ">";
    chip.outerHTML = "<button id='" + text + "-button' type='button' class='btn-edit' onclick='confirmEditChip(this)'><i class='bi bi-check-square'></i></button>";

}

function oninputValidate(chip) {

    const skill = chip.value;


    if ((skill === null) || validateEditedSkill(skill)) {
        if (chip.classList.contains("btn-edit")) chip.classList.remove("btn-edit")
        chip.classList.add("edit-input-error");
    } else {
        if (chip.classList.contains("edit-input-error")) chip.classList.remove("edit-input-error")
        chip.classList.add("edit-input");
    }
}

function confirmEditChip(chip) {
    enableButtons()
    const chipName = chip.id.replace('-button', '');
    const container = document.getElementById(chipName + "-chip");
    const input = document.getElementById(chipName + "-input");

    const skill = input.value;
    if ((skill === null) || validateEditedSkill(skill)) {
        input.classList.remove("btn-edit");
        input.classList.add("edit-input-error");
    } else {
        container.innerText = skill;
        container.innerHTML = "<div id='" + skill + "-chip' class='chip chipWithButton' onclick='removeSkillChip(this)'><i class='bi bi-x delete-icon'>" + skill + "</div>";
        container.id = skill + "-chip"

        chip.outerHTML = "</div> <button id='" + skill + "-button' type='button' class='btn-edit' onclick='editChip(this)'><i class='bi bi-pencil-square'></i></button>";

        skillCreationList = skillCreationList.filter((val) => val !== chipName);
        skillCreationList.push(skill);
    }

}

function resetSkills() {
    skillCreationList = [];
    document.getElementById("skillTagsArea").innerHTML = "";
}

/**
 * Takes skill input string validates it and returns a processed list
 */
async function handleSkills(e) {
    const input = document.getElementById("newSkillName");
    let skill = input.value;

    //Remove latest chip on backspace
    if (e !== undefined && e.keyCode === 8) {
        if (skill.length <= 0) {
            const lastSkill = skillCreationList.pop()
            if (lastSkill !== undefined) {
                document.getElementById(lastSkill + "-chip").remove()
                document.getElementById(lastSkill + "-button").remove()

            }
        }
        return skillCreationList
    }

    //Update the recommendations
    const response = await fetch("api/evidence/skills?query=" + skill)
    if (response.status === 200) {
        document.getElementById("evidenceSkillSuggestionArea").innerHTML = await response.text();
        //Show dropdown initially
        if (document.getElementById("skillResultDropdown")) {
            document.getElementById("skillResultDropdown").style.display = "block"
        }
    } else showErrorToast("Invalid Query for Skill Matches")

    if (skill.endsWith(" ") || (e !== undefined && e.keyCode === 13)) {
        //Remove the ending space
        if (skill.endsWith(" ")) skill = skill.slice(0, -1);

        // Validate skill
        if (!validateSkill()) {
            // Reset the input
            input.value = "";
            closeAutoComplete()

            // Add the skill to a list
            if (!skillCreationList.includes(skill) && skill !== "") {
                addSkillChip(skill)
            }
        }
    }

    updateEvidenceTagSkillsCharInfo()
    return skillCreationList
}

function handleAutocomplete(e){
    const skillInput = document.getElementById("newSkillName");
    const newSkill = e.target.innerText.replaceAll(" ", "_")

    if (!skillCreationList.includes(newSkill)) addSkillChip(newSkill)

    skillInput.value = "";
    closeAutoComplete()
}

function closeAutoComplete() {
    const dropdown = document.getElementById("skillResultDropdown")
    if (dropdown !== undefined && dropdown !== null) dropdown.remove()
}

function updateEvidenceTagSkillsCharInfo () {
    document.getElementById("charRemainingEvidenceTagSkillsCreation").innerText = (30 - document.getElementById("newSkillName").value.length) + " Characters Remaining"
}
