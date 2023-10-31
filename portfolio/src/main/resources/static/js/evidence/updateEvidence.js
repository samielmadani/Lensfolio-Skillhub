async function updateEvidence() {
    const button = document.getElementById("evidence-edit-button")
    const id = button.title

    await createNewEvidence()
    await deleteEvidence(id)
}