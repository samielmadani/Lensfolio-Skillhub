import {Selector} from 'testcafe'
import {BASE_URL, getPageUrl, login} from './helpers'

fixture ('/user_details').page (BASE_URL + '/login')

export const TEST_USERNAME = new Date().getTime()

test ("AC1: Register with minimum expected values", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.typeText('[data-test="firstName"]', "Steve")
    await browser.typeText('[data-test="lastName"]', "Jobs")
    await browser.typeText('[data-test="email"]', TEST_USERNAME + "@lensfolio.nz")
    await browser.typeText('[data-test="username"]', (TEST_USERNAME).toString(10))
    await browser.typeText('[data-test="password"]', "Aa1234567")
    await browser.click('[data-test="register"]')
    await browser.expect(getPageUrl()).contains('/user_details')
})

test ("AC1: Register with expected values missing", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.typeText('[data-test="firstName"]', "Steve")
    await browser.typeText('[data-test="email"]', TEST_USERNAME + "@lensfolio.nz")
    await browser.typeText('[data-test="username"]', (TEST_USERNAME).toString(10))
    await browser.typeText('[data-test="password"]', "Aa1234567")
    await browser.click('[data-test="register"]')
    await browser.expect(getPageUrl()).contains('/register')
})

test ("AC2: Register with existing username/email", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.typeText('[data-test="firstName"]', "Steve")
    await browser.typeText('[data-test="lastName"]', "Jobs")
    await browser.typeText('[data-test="email"]', TEST_USERNAME + "@lensfolio.nz")
    await browser.typeText('[data-test="username"]', (TEST_USERNAME).toString(10))
    await browser.typeText('[data-test="password"]', "Aa1234567")
    await browser.click('[data-test="register"]')
    await browser.expect(getPageUrl()).contains('/register')
    await browser.expect(Selector('[data-test="usernameError"]').textContent).contains("This Username has been taken")
})

test ("AC2/AC3: Login with account that doesn't exist", async (browser) => {
    await browser.typeText('[data-test="username"]', (TEST_USERNAME + 1).toString(10))
    await browser.typeText('[data-test="password"]', "Aa1234567")
    await browser.click("[data-test='login']")
    await browser.expect(getPageUrl()).contains("/login")
    await browser.expect(Selector('[data-test="loginMessage"]').textContent).contains("Log in attempt failed: username or password incorrect")
})

test ("AC4/AC6: Validation exists for register", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.click('[data-test="register"]')
    await browser.expect(getPageUrl()).contains('/register')
    await browser.expect(Selector('[data-test="firstNameError"]').textContent).contains("Must provide first name")
    await browser.expect(Selector('[data-test="lastNameError"]').textContent).contains("Must provide last name")
    await browser.expect(Selector('[data-test="emailError"]').textContent).contains("Must enter an email")
    await browser.expect(Selector('[data-test="usernameError"]').textContent).contains("Must provide username")
    await browser.expect(Selector('[data-test="passwordError"]').textContent).contains("Password must be at least 8 characters")
})

test ("AC4/AC6: Email validation correct for register", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.typeText('[data-test="email"]', "walalala")
    await browser.click('[data-test="register"]')
    await browser.expect(getPageUrl()).contains('/register')
    await browser.expect(Selector('[data-test="emailError"]').textContent).contains("Email is invalid, must followed by the corect domain")
})

test ("AC5: Tab cycles through inputs", async (browser) => {
    await browser.click('[data-test="registerRedirect"]')
    await browser.pressKey("tab tab tab tab tab tab tab")
    await browser.pressKey("a a a")
    await browser.expect(Selector('[data-test="username"]').value).contains("aaa")
})

test ("AC8: account details are shown on user details page", async (browser) => {
    await login({"username": (TEST_USERNAME).toString(10), "password": "Aa1234567", "browser": browser})
    await browser.expect(Selector('[data-test="fullname"]').textContent).contains("Steve Jobs")
    await browser.expect(Selector('[data-test="firstname"]').textContent).contains("Steve")
    await browser.expect(Selector('[data-test="lastname"]').textContent).contains("Jobs")
    await browser.expect(Selector('[data-test="nickname"]').textContent).eql("")
    await browser.expect(Selector('[data-test="bio"]').textContent).eql("")
    await browser.expect(Selector('[data-test="pronouns"]').textContent).contains("She/Her")
    await browser.expect(Selector('[data-test="email"]').textContent).contains(TEST_USERNAME + "@lensfolio.nz")
    await browser.expect(Selector('[data-test="registrationDate"]').textContent).contains("(0 months)")
    await browser.expect(Selector('[data-test="roles"]').textContent).contains("Student")

})

test ("AC12: logout/login is persistent", async (browser) => {
    await login({"username": (TEST_USERNAME).toString(10), "password": "Aa1234567", "browser": browser})
    await browser.click('[data-test="userDropdown"]')
    await browser.click('[data-test="logoutButton"]')
    await browser.expect(getPageUrl()).contains("/login")
    await login({"username": (TEST_USERNAME).toString(10), "password": "Aa1234567", "browser": browser})
    await browser.expect(getPageUrl()).contains("/user_details")
})