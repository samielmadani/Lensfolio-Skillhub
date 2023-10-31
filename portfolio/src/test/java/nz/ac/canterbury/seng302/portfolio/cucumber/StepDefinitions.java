package nz.ac.canterbury.seng302.portfolio.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.assertEquals;


public class StepDefinitions {
    @Given("i have a test")
    public void i_have_a_test() {
        // Write code here that turns the phrase above into concrete actions
    }

    @When("i run the test")
    public void i_run_the_test() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("the test passes")
    public void the_test_passes() {
        // Write code here that turns the phrase above into concrete actions
        int a = 1;
        int b = 1;
        assertEquals(a, b);
    }
}
