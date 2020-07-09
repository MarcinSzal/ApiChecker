package StepDef;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import ApiChecker.*;

public class Steps
{
    ApiChecker checker = new ApiChecker ();

    @Given("Rates API for Latest Foreign Exchange rates")
    public void buildAPIurlForLatestRates() {
        RestAssured.baseURI=checker.defaultURL+"latest";
        System.out.println("Default API URI is: "+RestAssured.baseURI);
    }

    @Given("Rates API for {string} Foreign Exchange rates")
    public void buildAPIurlForSpecificDateRates(String specificDate) {
        RestAssured.baseURI=checker.defaultURL+specificDate;

        System.out.println("Default API URI is: "+RestAssured.baseURI);
    }


    @Given("Rates API for Latest Foreign Exchange rates with {string}")
    public void buildAPIurlForWrongAddress(String url) {
        RestAssured.baseURI=url;
        System.out.println("Default API URI is: "+RestAssured.baseURI);
    }

    @When("The API is available")
    public void the_API_is_available() {
        ApiChecker.isAPIavailable();
    }

    @Then("An automated test suite should run which will assert the success status of the response")
    public void verifyResponseStatusIsSuccess() {
        ApiChecker.isAPIreturningSuccesResponse();
    }

    @Then("An automated test suite should run for {string} rate and {string} base which will assert the response")
    public void positiveTestForGivenAPIquery(String rates, String base) {
        try{
            ApiChecker.GetandAssertResponse(rates, base,null, false);
        }
        catch(Exception e) {
            throw new io.cucumber.java.PendingException();
        }
    }

}
