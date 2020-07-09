package ApiChecker;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.regex.Pattern;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.junit.Assert;


import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class ApiChecker
{
    static boolean isBaseProvided;
    static boolean areRatesCcyProvided;
    static int numberOfRatesCcy = 0;
    public static Response response;
    public static JsonPath jsonPathEvaluator;
    public static ValidatableResponse Vresponse;

    final String BaseURL = "https://api.ratesapi.io/api/";
    public String defaultURL = BaseURL;

    public static void verifyIfBaseSyntaxIsCorrect(String baseCcy) {
        Assert.assertTrue("Wrong base currency syntax. Base must be provided as 3 upper case letters e.g. USD. " +
                "Only one base currency can be provided at the time", Pattern.matches("^[A-Z]{3}$", baseCcy));
    }

    public static void verifyIfCurrenciesSyntaxIsCorrect(String currencies) {
        for (String currency : currencies.split(",")) {
            Assert.assertTrue("Wrong rates currencies syntax. " +
                    "Currency must be provided as 3 upper case letters e.g. USD. " +
                    "If you want to check more than one currency then use comma e.g. USD,GBP,EUR", Pattern.matches("^[A-Z]{3}$", currency));
            numberOfRatesCcy += numberOfRatesCcy;
        }
    }

    public static void runGetRequestWithParams(String currencies, String baseCcy) {
        RequestSpecification httpRequest = given();
        response = httpRequest.get("?base=" + baseCcy + "&symbols=" + currencies);
        QueryableRequestSpecification queryable = SpecificationQuerier.query(httpRequest);
        System.out.println("Header is: " + queryable.getURI());
        jsonPathEvaluator = response.jsonPath();
        Vresponse = when().get("https://api.ratesapi.io/api/latest?base=" + baseCcy + "&symbols=" + currencies).then().header("Connection", "keep-alive");

    }

    public static void checkStatusCode(Response response, boolean isItNegativeTest) {

        String responseCode = response.getStatusLine();
        //negative cases if server is not returning results
        Assert.assertNotEquals("Server is busy or not available, try again later", "HTTP/1.1 408 Request Timeout", responseCode);
        Assert.assertNotEquals("Server is not available, try again later", "HTTP/1.1 404 Not Found", responseCode);
        Assert.assertNotEquals("The requested resource is unavailable at this present time", "HTTP/1.1 403 Forbidden", responseCode);
        Assert.assertNotEquals("The user is unauthorized to access the requested resource.", "HTTP/1.1 401 Unauthorized", responseCode);
        if (responseCode.equals("HTTP/1.1 400 Bad Request"))
        {
            System.out.println("Error message is: " + getErrorMessageForFailedRequest(response));
            if (!isItNegativeTest)
                Assert.assertEquals("The request cannot be fulfilled due to bad syntax.", "HTTP/1.1 200 OK", responseCode);
        } else if (isItNegativeTest)
            System.out.println("Error message is: " + getErrorMessageForFailedRequest(response));  // oczekujemy, ze wszystkie negatywne testy przejdą tu i dadzą error

        //expected result for correct syntax query
        if (!isItNegativeTest)
            Assert.assertEquals("API is not available OR wrong URL provided", "HTTP/1.1 200 OK", responseCode);
    }

    public static void ValidateBaseCcyResponse(String inputBaseCcy) {
        String actualBaseCcy = jsonPathEvaluator.get("base");
        if (isBaseProvided)
            Assert.assertEquals("Wrong base currency in response", inputBaseCcy, actualBaseCcy);
        else
            Assert.assertEquals("Default base currency should be EUR", "EUR", actualBaseCcy);
    }

    public static void validateCCyBody(String currencies) {
        for (String currency : currencies.split(",")) {
            Vresponse.body("rates." + currency, notNullValue());
            Vresponse.body("rates." + currency, not(equalTo(0)));

            Map<String, Double> rates = Vresponse.extract().response().path("rates");
            for (String currencyKey : rates.keySet()) {
                Assert.assertTrue(Pattern.matches("^[A-Z]{3}$", currencyKey));
            }
        }
    }

    public static void verifyIfCorrectDateIsUsed(String specificDate) {
        String actualGetTime = jsonPathEvaluator.get("date");
        if (specificDate == null)
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);

            if (actualGetTime.equals(String.valueOf(LocalDate.now())) || actualGetTime.equals(dateFormat.format(cal.getTime()))) {
                System.out.println("Date not provided so default (current) date used: " + actualGetTime);
            } else
                Assert.assertEquals("Wrong date returned in response", java.time.LocalDate.now(), actualGetTime);

        } else
        {
            LocalDate input = LocalDate.parse(specificDate);
            LocalDate output = LocalDate.parse(actualGetTime);
            boolean isFutureDateTest = input.isAfter(LocalDate.now());
            if (isFutureDateTest)
                System.out.println("Input date: " + input + " is in future hence converting to latest (current) date: " + output);
            else
                Assert.assertEquals("Wrong date returned in response", specificDate, actualGetTime);
        }
    }

    public static String getErrorMessageForFailedRequest(Response response) {
        JsonPath jsonPathEvaluator = response.jsonPath();
        return jsonPathEvaluator.get().toString();
    }

    public static void GetandAssertResponse(String currencies, String baseCcy, String SpecificDate, boolean isItNegativeTest) {
        if (baseCcy.length() > 0 && !isItNegativeTest) {  //sprawdza składnię
            verifyIfBaseSyntaxIsCorrect(baseCcy);
            isBaseProvided = true;
        } else {
            System.out.println("Base currency was not provided so default currency will be used as a base: EUR");
            isBaseProvided = false;
        }

        if (currencies.length() > 0 && !isItNegativeTest) { //sprawdza składnię tylko jeśli currency fiter został dostarczony
            verifyIfCurrenciesSyntaxIsCorrect(currencies);
            areRatesCcyProvided = true;
        } else {
            System.out.println("No filter was applied on Rates hence all currencies rate for base currency will be returned");
            areRatesCcyProvided = false;
        }
        runGetRequestWithParams(currencies, baseCcy);
        checkStatusCode(response, isItNegativeTest);  // test sprawdza czy testy faktycznie nie przeszły

        if (!isItNegativeTest) // odpala tylko dla pozytywnych testów
        {
            ValidateBaseCcyResponse(baseCcy);
            validateCCyBody(currencies);
            String responseBody = response.getBody().asString();
            System.out.println("Response Body is =>  " + responseBody);
            verifyIfCorrectDateIsUsed(SpecificDate);
        }

    }

    public static void isAPIreturningSuccesResponse() { ;
        String statusCode = callAPIforGivenURL().getStatusLine();
        Assert.assertEquals("API is not available OR wrong URL provided", "HTTP/1.1 200 OK", statusCode);
    }

    public static void isAPIavailable() {
        String ResponseFormatType = callAPIforGivenURL().getContentType();
        Assert.assertEquals("API does not return JSON", "application/json", ResponseFormatType);
    }


    public static Response callAPIforGivenURL()
    {
        RequestSpecification httpRequest = RestAssured.given();
        return httpRequest.request(Method.GET);
    }

}
