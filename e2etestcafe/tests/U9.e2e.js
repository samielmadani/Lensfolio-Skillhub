import {Selector} from 'testcafe'
import {BASE_URL, login, currentTime} from './helpers'

fixture ('Evidence').page (BASE_URL + '/evidence')

async function selectProject(browser, project) {
    // Select the project
    await browser.navigateTo(BASE_URL + "/evidence")
    await browser.click('[data-test="selectProject"]')
    await browser.click('[data-test="' + project + '"]')
}

async function createEvidenceNameDescription(browser) {
    await browser.click('[data-test="createEvidence"]')
    await browser.selectText('[data-test="newEvidenceName"]').pressKey('delete')
    await browser.typeText('[data-test="newEvidenceName"]', "Evidence " + currentTime())
    await browser.typeText('[data-test="newEvidenceDescription"]', "Evidence " + currentTime() + " description")
}

test ("AC1-Add multiple skill with the space bar", async (browser) => {
    await login("admin200", "OEZZsr64wvYF7kFeV3dC", browser)
    await selectProject(browser, "Project 2022")
    await createEvidenceNameDescription(browser);

    // Add two skills
    await browser.typeText('[data-test="newSkillName"]', "skill1")
    await browser.typeText('[data-test="newSkillName"]', " ")
    await browser.typeText('[data-test="newSkillName"]', "skill2")
    await browser.typeText('[data-test="newSkillName"]', " ")

    // check its added correctly
    await browser.expect(Selector('#skill1-chip').exists).ok()
    await browser.expect(Selector('#skill2-chip').exists).ok()
})

test ("AC1-Supports many types of characters", async (browser) => {
    await login("admin200", "OEZZsr64wvYF7kFeV3dC", browser)
    await selectProject(browser, "Project 2022")
    await createEvidenceNameDescription(browser);

    let skill = "aAb#";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#newSkillName').hasClass('is-invalid')).ok()

    // Clear
    await browser.selectText('[data-test="newSkillName"]').pressKey('delete')

    skill = "aAb_b123";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#' + skill + '-chip').exists).ok()
})

test ("AC2-Autocomplete recommends correctly", async (browser) => {
    // create some skills
    await login("admin200", "OEZZsr64wvYF7kFeV3dC", browser)
    await selectProject(browser, "Project 2022")
    await createEvidenceNameDescription(browser);

    await browser.typeText('[data-test="newSkillName"]', "skill")
    await browser.typeText('[data-test="newSkillName"]', " ")
    await browser.typeText('[data-test="newSkillName"]', "alphabet")
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.click('[data-test="save"]')

    // Attempt to create new evidence
    await createEvidenceNameDescription(browser);

    await browser.typeText('[data-test="newSkillName"]', "sk")
    await browser.expect(Selector('a').withAttribute('data-test', 'skill-dropdown').exists).ok()
    await browser.expect(Selector('a').withAttribute('data-test', 'alphabet-dropdown').exists).notOk()

    await browser.selectText('[data-test="newSkillName"]').pressKey('delete')

    await browser.typeText('[data-test="newSkillName"]', "al")
    await browser.expect(Selector('a').withAttribute('data-test', 'alphabet-dropdown').exists).ok()
    await browser.expect(Selector('a').withAttribute('data-test', 'skill-dropdown').exists).notOk()
})

test ("AC1-No Skills cannot be used as a skill tag", async (browser) => {
    // create some skills
    await login("admin200", "OEZZsr64wvYF7kFeV3dC", browser)
    await selectProject(browser, "Project 2022")
    await createEvidenceNameDescription(browser);

    // Test skill "No_Skills" cannot be used
    let skill = "No_Skills";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#newSkillName').hasClass('is-invalid')).ok()

    // Clear
    await browser.selectText('[data-test="newSkillName"]').pressKey('delete')

    // Test skill "no_skills" cannot be used
    skill = "no_skills";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#newSkillName').hasClass('is-invalid')).ok()

    // Clear
    await browser.selectText('[data-test="newSkillName"]').pressKey('delete')

    // Test skill "no_Skills" cannot be used
    skill = "no_Skills";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#newSkillName').hasClass('is-invalid')).ok()

    // Clear
    await browser.selectText('[data-test="newSkillName"]').pressKey('delete')

    skill = "Normal_Skill";
    await browser.typeText('[data-test="newSkillName"]', skill)
    await browser.typeText('[data-test="newSkillName"]', " ")

    await browser.expect(Selector('#' + skill + '-chip').exists).ok()
})