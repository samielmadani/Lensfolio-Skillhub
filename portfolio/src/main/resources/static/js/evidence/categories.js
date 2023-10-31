/**
 * Updates the categories for evidence creation when a category is selected/deselected.
 * This will update the visual display for the user indicating what categories are currently selected
 * @param category name of the category
 */
function updateCategory (category) {
    enableButtons()

    const categoryText = document.getElementById("category-text")
    const categoryString = categoryText.textContent
    if (categoryString.includes(category)) {
        //remove the ', ' character, if it exists
        if (categoryString.includes(category + ", ")) category = category + ", "
        if (categoryString.indexOf(category) === categoryString.length - category.length && categoryString.length !== category.length) category = ", " + category
        categoryText.textContent = categoryString.replace(category, "")
    } else {
        //if this is the first category to be added, we don't need to add a ', ' before
        if (categoryString.length !== 0) {
            category = ", " + category
        }
        categoryText.textContent += category
    }
}

/**
 * Resets the category HTML input
 */
function resetCategoryInput () {
    document.getElementById("category-text").textContent = ""
    document.getElementById("quantitative-checkbox").checked = false
    document.getElementById("qualitative-checkbox").checked = false
    document.getElementById("service-checkbox").checked = false
}

/**
 * Gets a list of the categories added to the currently being created piece of evidence
 * @returns {(string)[]} list of all the categories to be added
 */
function getCategories () {
    return [(document.getElementById("quantitative-checkbox").checked) ? "Quantitative Skill" : null,
            (document.getElementById("qualitative-checkbox").checked) ? "Qualitative Skill" : null,
            (document.getElementById("service-checkbox").checked) ? "Service" : null]
}