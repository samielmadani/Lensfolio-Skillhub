import {Selector} from 'testcafe'
import {BASE_URL, getPageUrl} from './helpers'

const TEST_USERNAME = new Date().getTime()

fixture ('/editUserDetails').page (BASE_URL + '/user_details')

// Temporary untill we have time to fix
// test ("AC1: I can edit my details but not username.", async (browser) => {
//     await browser.click('[data-test="edit"]')
//     await browser.expect(getPageUrl()).contains('/editUserDetails')
//     await browser.typeText('[data-test="firstName"]', "Mark")
//     await browser.typeText('[data-test="middleName"]', "Steve")
//     await browser.typeText('[data-test="nickName"]', "CEO")
//     await browser.typeText('[data-test="lastName"]', "Zuckerberg")
//     await browser.typeText('[data-test="email"]', TEST_USERNAME + "@lens.nz")
//     await browser.typeText('[data-test="bio"]', "From Apple and Facebook")
//     await browser.click('[data-test="editUserDetails"]')
//     await browser.expect(getPageUrl()).contains('/editUserDetails')
// })
//
// test ("AC234: Validation exists for edit user details", async (browser) => {
//     await browser.click('[data-test="editUserDetails"]')
//     await browser.expect(getPageUrl()).contains('/editUserDetails')
//     await browser.expect(Selector('[data-test="firstNameError"]').textContent).contains("Must provide first name")
//     await browser.expect(Selector('[data-test="lastNameError"]').textContent).contains("Must provide last name")
//     await browser.expect(Selector('[data-test="emailError"]').textContent).contains("Must enter an email")
// })
