import {Selector} from 'testcafe'
import {BASE_URL, login, currentTime, getPageUrl} from './helpers'

fixture ('/projects').page (BASE_URL + '/projects')

async function selectProject(browser) {
    // Select the project
    await browser.navigateTo(BASE_URL + "/projects")
    await browser.click('[data-test="selectProject-1"]')
}

test ("AC1-I can browse to the page that contains the project details", async (browser) => {
    await login("student200", "wkhMNHn6HROm8Lx19G3T", browser)
    await selectProject(browser)
    await browser.expect(getPageUrl()).contains('projectID=1')
})


test ("AC-3 The default project exists with project name is project current year", async (browser) => {
    await login("teacher200", "j8vgxsVUddfHk4g0skSc", browser)
    await selectProject(browser)
    await browser.expect(Selector('[data-test="projectNameDetails"]').textContent).eql("Project 2022")
})