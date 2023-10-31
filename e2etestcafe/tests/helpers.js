import {ClientFunction} from 'testcafe'
import {TEST_USERNAME} from "./UUi.e2e";

export const login = async (username, password, browser) => {
    await browser.navigateTo(BASE_URL + "/login")
    await browser.typeText('[data-test="username"]', username)
    await browser.typeText('[data-test="password"]', password)
    await browser.click('[data-test="login"]')
}

export const loginDefault = async (browser) => {
    await browser.typeText('[data-test="username"]', TEST_USERNAME)
    await browser.typeText('[data-test="password"]', "Aa1234567")
    await browser.click("[data-test='login']")
}

export function currentTime() {
    return new Date().getTime();
}

export const getPageUrl = ClientFunction (() => window.location.href )

export const BASE_URL = 'http://localhost:9000'
