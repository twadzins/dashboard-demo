package org.vaadin.testbenchsauce;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import com.google.common.base.Predicate;
import com.google.gson.JsonObject;
import com.jayway.restassured.specification.RequestSpecification;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.vaadin.testbench.Parameters;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchTestCase;
import com.vaadin.testbench.screenshot.ImageFileUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.fest.util.Strings;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.internal.BaseTestMethod;
import org.vaadin.testbenchsauce.webdriverwrapper.WebDriverWrapper;
import org.vaadin.testbenchsauce.webdriverwrapper.WebElementWrapper;

import static ch.lambdaj.Lambda.*;
import static com.jayway.restassured.RestAssured.given;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

abstract public class BaseTestBenchTestCase extends TestBenchTestCase {
    protected static final Logger logStep = Logger.getLogger(BaseTestBenchTestCase.class.getCanonicalName() + ".stepLogger");

    private static final boolean USE_REMOTE_WEB_DRIVER = true;
    private static final boolean TAKE_SCREENSHOT = false;
    public static final String USERNAME = "";
    public static final String PASSWORD = "";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    public static final int SLOW_FIND_ELEMENTS_WARNING_MILLIS = 150;

    public static int REMOTE_DRIVER_PORT;
    private static String BASE_URL;
    private static boolean USE_CHROME_DRIVER;
    private static boolean UPDATE_RALLY_TEST_CASE = false;
    private static String JENKINS_BUILD_NUMBER;
    private static String JENKINS_BUILD_URL;
    private static String RALLY_SERVER_URL;
    private static String RALLY_USERNAME;
    private static String RALLY_PASSWORD;

    static {
        String appDir = getPhantomJsPath();

        //Change to "true" to enable ChromeDriver (or launch with -dtestbench.useChromeDriver=true
        USE_CHROME_DRIVER = Boolean.parseBoolean(System.getProperty("testbench.useChromeDriver", "false"));

        BASE_URL = System.getProperty("testbench.baseUrl", "http://localhost:8080/quicktickets-dashboard-1.0.1/");
        UPDATE_RALLY_TEST_CASE = Boolean.parseBoolean(System.getProperty("testbench.updateRallyTestCases", "false"));
        RALLY_SERVER_URL = System.getProperty("rallyServerUrl", "https://rally1.rallydev.com");
        RALLY_USERNAME = System.getProperty("rallyUsername", "not defined"); //
        RALLY_PASSWORD = System.getProperty("rallyPassword", "not defined");
        JENKINS_BUILD_NUMBER = System.getProperty("jenkinsBuildNumber", "unknown");
        JENKINS_BUILD_URL = System.getProperty("jenkinsBuildUrl", "javascript://alert('No jenkins url exists because this build was not run by jenkins for some reason...')");

        logStep.info("Base url:" + BASE_URL + " - Using Chrome Driver: " + USE_CHROME_DRIVER);

        if(!USE_CHROME_DRIVER){
            launchPhantomJs(appDir);
        }
    }
    
    private boolean closeBrowserOnFailEnabled = true;
    private WebDriver wrappedDriver;
    private Map<String,String> restAuthCookies;
    private TestngAppender testngAppender;
    private static int findElementCount;
    private static int tempScreenshotCounter;

    private static String getPhantomJsPath(){
        return System.getProperty("testbench.phantomjs.path", System.getProperty("user.dir") + "/src/test/resources/programs/phantomjs/");
    }

    public WebDriver getDriverWrapper() {
        if (wrappedDriver == null ) {
            wrappedDriver = new WebDriverWrapper(super.getDriver());
        }
        return wrappedDriver;
    }

    private static void launchPhantomJs(String appDir) {
//        System.out.println("Locating open port for phantomjs...");
        REMOTE_DRIVER_PORT = findFreePort();
//        System.out.println("Found open port for phantomjs: "+ REMOTE_DRIVER_PORT);

        String executionString = appDir + "phantomjs.exe --proxy-type=none --webdriver-loglevel=info --webdriver=" + REMOTE_DRIVER_PORT; //--webdriver-loglevel=debug for full js output and phantomjs commands 
        System.out.println("Executing "+executionString);
        CommandLine commandLine = CommandLine.parse(executionString);
        try {
            ProcessExecutor.runProcess(appDir, commandLine, new ProcessExecutorHandler() {
                @Override
                public void onStandardOutput(String msg) {
                    System.out.println("phantomjs:" + msg);
                }
    
                @Override
                public void onStandardError(String msg) {
                    System.err.println("phantomjs:" + msg);
                }
            }, 20 * 60 * 1000);
        } catch (IOException e) {
            throw new RuntimeException("Error starting phantomjs", e);
        }
    }

    private Integer walkThroughWaitMillis;

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult testResult) {
        if (!testResult.isSuccess()) {
            onTestFailure(testResult);
        } else {
            assertNoWarningMessage();
        }

        if(UPDATE_RALLY_TEST_CASE){
            updateTestDescriptionWithRallyLinks(testResult); //Let's Jenkins test reports link back to Rally
            updateRally(testResult);
        }

        quitDriver();
    }

    private void updateTestDescriptionWithRallyLinks(ITestResult testResult) {
        ITestNGMethod method = testResult.getMethod();
        String extraDescriptionDetail = null;
        if (method instanceof BaseTestMethod) {
            BaseTestMethod baseTestMethod = (BaseTestMethod) method;
            extraDescriptionDetail = "Rally test case(s): ";
            Method baseTestMethodMethod = baseTestMethod.getMethod();
            if (baseTestMethodMethod.isAnnotationPresent(RallyTestCase.class)) {
                String[] rallyTestCaseIds = baseTestMethodMethod.getAnnotation(RallyTestCase.class).value();
                if (rallyTestCaseIds != null) {
                    for (String rallyTestCaseId : rallyTestCaseIds) {
                        if(rallyTestCaseId != null && !rallyTestCaseId.isEmpty()){
                            extraDescriptionDetail += "<a target='_blank' href=\"https://rally1.rallydev.com/#/search?keywords=" +rallyTestCaseId  + "\">" + rallyTestCaseId + "</a>&nbsp;";
                        }
                    }
                }
                
                if (extraDescriptionDetail != null) {
                    String description = method.getDescription();
                    baseTestMethod.setDescription(description != null ? description + " - " + extraDescriptionDetail : extraDescriptionDetail);
                }
            }
        }
    }

    private void updateRally(ITestResult testResult) {
        //check to see if the test method has the RallyTestCase annoation.
        Method method = testResult.getMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(RallyTestCase.class)) {
            String[] rallyTestCaseIds = method.getAnnotation(RallyTestCase.class).value();
            if (rallyTestCaseIds != null) {
                for (String rallyTestCaseId : rallyTestCaseIds) {
                    if(rallyTestCaseId != null && !rallyTestCaseId.isEmpty()){
                        try {
                            updateRallyTestCase(rallyTestCaseId, testResult);
                        } catch (URISyntaxException e) {
                            printRallyServerError(e.getMessage());
                        } catch (IOException e) {
                            printRallyServerError(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void printRallyServerError(String message){
        System.out.println("------Rally Integration Error--------");
        System.out.println(message);
        System.out.println("Rally Server parameters:");
        System.out.println("rallyServerUrl:"+RALLY_SERVER_URL);
        System.out.println("rallyUsername:"+RALLY_USERNAME);
        System.out.println("rallyPassword:"+RALLY_PASSWORD);
        System.out.println("-------------------------------------");
    }

    private void updateRallyTestCase(String testCaseId, ITestResult testResult) throws URISyntaxException, IOException {
        //Rally API Setup
        RallyRestApi restApi = new RallyRestApi(new URI(RALLY_SERVER_URL), RALLY_USERNAME, RALLY_PASSWORD);
        restApi.setApplicationName("VaadinTestBench");

        int status = testResult.getStatus();
        String verdict = null;
        if(status == ITestResult.FAILURE){
            verdict = "Fail";
        }
        else if(status == ITestResult.SUCCESS){
            verdict = "Pass";
        }
        else {
            //For now if it's not a failure or success, don't update rally
            return;
        }

        Duration duration = new Duration(testResult.getStartMillis(), testResult.getEndMillis());

        try {
            //Get a reference to the test case
            QueryRequest testCaseRequest = new QueryRequest("TestCase");
            testCaseRequest.setFetch(new Fetch("FormattedID", "Name"));
            testCaseRequest.setQueryFilter(new QueryFilter("FormattedID", "=", testCaseId));
            QueryResponse testCaseResponse = restApi.query(testCaseRequest);
            if(testCaseResponse.getTotalResultCount() <= 0) {
                System.out.println("WARNING: Could not add test result to Rally test case '"+testCaseId+"' because it was not found.");
                return;
            }

            JsonObject testCaseJsonObject = testCaseResponse.getResults().get(0).getAsJsonObject();
            String testCaseRef = testCaseJsonObject.get("_ref").getAsString();

            //Add a test result to that test case
            JsonObject testCaseResult = new JsonObject();
            testCaseResult.addProperty("Verdict", verdict);
            testCaseResult.addProperty("Build", JENKINS_BUILD_NUMBER);
            testCaseResult.addProperty("TestCase", testCaseRef);
            testCaseResult.addProperty("Date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
            testCaseResult.addProperty("Duration",duration.getMillis());
            
            //rallyTestCaseIntegration(SolutionsTabUiTest)
            String methodName = testResult.getName();
            Class realClass = testResult.getTestClass().getRealClass();

            String jobHref = "<b><a target='_blank' href=\"" + JENKINS_BUILD_URL + "\">Jenkins Job</a></b>";

            //testngreports/<package>/<package>.<Class>/<methodName>/
            String testUrl = JENKINS_BUILD_URL + "testngreports/" + realClass.getPackage().getName() + "/" + realClass.getName() + "/" + methodName + "/";
            String testHref = "<a target='_blank' href=\"" + testUrl + "\">" + "Test Method: " + methodName + "(" + realClass.getSimpleName() + ")" + "</a>";
            String notes = jobHref + "&nbsp" + testHref;

            notes += "<div><b>Release Build Test Output:</b></div>";
            notes += getHtmlTestOutput(testResult);
            if (!testResult.isSuccess()) {
                notes += "<div style='color:red;'><b>Failure Details:</b></div>";
                
                notes += ExceptionUtils.getStackTrace(testResult.getThrowable());
            }
            
            testCaseResult.addProperty("Notes", notes);

            CreateRequest createTestResultRequest = new CreateRequest("testcaseresult", testCaseResult);
            CreateResponse createTestResultResponse = restApi.create(createTestResultRequest);

            //Output response
            if(createTestResultResponse.wasSuccessful()){
                System.out.println("Created test result for Rally test case '"+testCaseId+"' - "+testCaseRef);
            }else {
                String[] createErrors;
                createErrors = createTestResultResponse.getErrors();
                System.out.println("WARNING: Error occurred creating Rally test case result for '"+testCaseId+"'");
                for (int i=0; i<createErrors.length;i++) {
                    System.out.println(createErrors[i]);
                }
            }
        }finally {
            restApi.close();
        }
    }

    private String getHtmlTestOutput(ITestResult testResult) {
        StringBuilder output = new StringBuilder();
        for (Iterator<String> iterator = Reporter.getOutput(testResult).iterator(); iterator.hasNext(); ) {
            String line = iterator.next();
            output.append(line);
            if (iterator.hasNext()) {
                output.append("<br/>");
            }
        }
        return StringUtils.abbreviate(output.toString(), 4000); //don't overload rally, only allow up to 1000 characters
    }

    public void quitDriver() {
        if (isCloseBrowserOnFailEnabled() || !USE_CHROME_DRIVER) { //quit unless is closeBrowser disabled and using chrome driver
            getDriverWrapper().quit();
            wrappedDriver = null;
        }
    }

    /**
     * Preparing for actual test, create a firefox driver and define the address
     * where the app is located.
     */
    @BeforeMethod(alwaysRun = true)
    public final void setUpBase() throws Exception {
//        System.out = new PrintStream();
        testngAppender = new TestngAppender();
        Logger.getRootLogger().addAppender(testngAppender);
        setUpInternal();
    }

    public void setUpInternal() throws Exception {
        initDriver();
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                if(USE_CHROME_DRIVER) {
                    driver.manage().window().setSize(new Dimension(1280, 1024));
                }
                else {
                    //look into if we can use driver.manage.window.setSize for phantomjs and not just chrome driver
                    testBench().resizeViewPortTo(1280, 1024);
                }

                return null;
            }
        }).run();
    }

    private void initDriver() throws Exception {
        System.setProperty("vaadin.testbench.developer.license", "license-here");
        Parameters.setScreenshotErrorDirectory("target/test-classes/screenshots/errors");
        if (USE_CHROME_DRIVER) {
            useChromeDriver();
        } else {
            useGhostDriver();
        }
        Parameters.setDebug(false);

        //init the driver wrapper
        getDriverWrapper();
    }

    private void onTestFailure(ITestResult testResult) {
        Reporter.setCurrentTestResult(testResult);

        onTestFailure(testResult.getName(), testResult.getTestClass().getName());

        Reporter.setCurrentTestResult(null);
    }

    public void onTestFailure(String methodName, String className) {
        if (getDriverWrapper() == null) {
            Reporter.log("Unable to take screenshot on failure, since getDriverWrapper is null");
            return;
        }
        // Grab a screenshot when a test fails
        try {
            BufferedImage screenshotImage = ImageIO
                    .read(new ByteArrayInputStream(
                            ((TakesScreenshot) getDriverWrapper())
                                    .getScreenshotAs(OutputType.BYTES)));
            // Store the screenshot in the errors directory
            ImageFileUtil.createScreenshotDirectoriesIfNeeded();
            File errorScreenshotFile = ImageFileUtil.getErrorScreenshotFile(methodName + "(" + className + ")" + ".png");
            System.out.println("Writing error image to: " + errorScreenshotFile.getCanonicalPath());
            ImageIO.write(screenshotImage, "png", errorScreenshotFile);
//            Reporter.log("<a href='../../../" + errorScreenshotFile + "'>screenshot</a>"); //TODO: make work with real jenkins path.
        } catch (IOException e1) {
            e1.printStackTrace();
            Reporter.log("Couldn't create screenshot");
            Reporter.log(e1.getMessage());
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        Logger.getRootLogger().removeAppender(testngAppender);
        if (TAKE_SCREENSHOT) {
            takeScreenshot();
        }
    }

    protected void takeScreenshot() {
        File screenshotAs = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String pathname = "c:/temp/screenshot_" + tempScreenshotCounter++ + ".png";
        try {
            FileUtils.copyFile(screenshotAs, new File(pathname));
        } catch (IOException e) {
            throw new RuntimeException("Unable to write screenshot file: " + pathname, e);
        }
    }

    private void useGhostDriver() throws Exception {
        final DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("takesScreenshot", true);
        final URL remoteAddress = new URL("http://localhost:" + REMOTE_DRIVER_PORT);
        if (USE_REMOTE_WEB_DRIVER) {
            RemoteWebDriver remoteWebDriver = new Retry<RemoteWebDriver>(new Retryable<RemoteWebDriver>() {
                @Override
                public RemoteWebDriver run() {
                    return new RemoteWebDriver(remoteAddress, desiredCapabilities);
                }
            }).run();
            setDriver(TestBench.createDriver(remoteWebDriver));
        } else {
            //Haven't tested this in a while, sicne the remote driver always gave higher performance
           desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, getPhantomJsPath() + "phantomjs.exe");
            PhantomJSDriver phantomJSDriver = new PhantomJSDriver(desiredCapabilities);
//            phantomJSDriver.setLogLevel(java.util.logging.Level.ALL);
            driver = phantomJSDriver;
           setDriver(TestBench.createDriver(driver));
        }
    }

    private void useChromeDriver() {
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/src/test/resources/programs/chromedriver/chromedriver2_win32_0.8/chromedriver.exe");
        setDriver(TestBench.createDriver(new ChromeDriver()));
    }

    protected boolean isCloseBrowserOnFailEnabled() {
        return closeBrowserOnFailEnabled;
    }
    
    public void keepBrowserOpenOnFail() {
        closeBrowserOnFailEnabled = false;
    }

    protected void assertSubWindowNotActive() {
        assertSubWindowActiveInternal(false);
    }

    protected void assertSubWindowActive() {
        assertSubWindowActiveInternal(true);
    }

    private void assertSubWindowActiveInternal(boolean expectActive) {
        boolean windowPresent = isElementPresent(By.className("v-window"));
        if (windowPresent && !expectActive) {
            fail("SubWindow was expect to be NOT be active, but was found");
        }
        if (!windowPresent && expectActive) {
            fail("SubWindow was expect to be be active, but was NOT found");
        }
    }

    protected void closeSubWindow() {
        getDriverWrapper().findElement(By.className("v-window-closebox")).click();
    }

    protected boolean isSelected(WebElement webElement) {
        String selected = webElement.getAttribute("selected");
        return "true".equals(selected) || "selected".equals(selected);
    }

    protected boolean isDisabled(WebElement webElement) {
        return webElement.getAttribute("class").contains("v-disabled");
    }

    protected void assertTabSelected(String title) {
        assertTabExists(title);
        WebElement element = findElement(com.vaadin.testbench.By.xpath("//div[contains(@class, 'v-tabsheet-tabitem-selected')]/div/div[contains(text(), '" + title + "')]"));
        assertNotNull("Tab should have been selected containing text '" + title + "'", element);
    }

    protected void assertTextFieldHasFocus (String caption) {
        logStep.info("Asserting that field: " + caption + " has focus");
        WebElement current = getDriverWrapper().switchTo().activeElement();
        WebElement field = getTextField(caption);
        assertEquals("Expected field: " + caption + " to have focus", current, field);
    }

    protected void assertTextFieldHasValue(String textFieldCaption, String expectedValue) {
        logStep.info("Asserting that field: " + textFieldCaption + " has value: " + expectedValue);
        WebElement field = getTextField(textFieldCaption);
        assertEquals("Text field '" + textFieldCaption + "' has wrong value", expectedValue, getElementValue(field));
    }

    protected void assertFieldHasValue(WebElement textField, String expectedValue){
        assertFieldHasValue(null, textField, expectedValue);    
    }
    
    protected void assertFieldHasValue(String fieldDescription, WebElement textField, String expectedValue){
        String formattedFieldDescription = fieldDescription == null ? "" : "'" + fieldDescription + "'";
        assertEquals("Field " +
                formattedFieldDescription +
                " value doesn't match", expectedValue, getElementValue(textField));
    }

    protected void assertTextFieldValuesMatch(WebElement email1, WebElement email2) {
        assertEquals("Field values don't match", getElementValue(email1), getElementValue(email2));
    }

    protected void assertTabExists(String title) {
        logStep.info("Asserting that tab exists: " + title );
        WebElement element = findElement(com.vaadin.testbench.By.xpath("//div[contains(@class, 'v-tabsheet-tabitem')]/div/div[contains(text(), '" + title + "')]"));
        assertNotNull("Tab not found: '" + title + "'. Found:" + getTabsAsText(), element);
    }

    protected void assertButtonExists(String title){
        logStep.info("Asserting that button exists: " + title );
        WebElement button = getButton(title);
        assertNotNull("Button not found containing text '" + title + "'", button);
    }

    protected void assertButtonDoesNotExist(String title){
        logStep.info("Asserting that button does not exist: " + title );
        WebElement button = getButton(title);
        assertNull("Button found containing text '" + title + "', expected none", button);
    }

    protected List<WebElement> getTableRows() {
        List<WebElement> elements = findElements(By.xpath("//tr[contains(@class, 'v-table-row')]"));
        assertNotNull("Could not find table rows", elements);
        return elements;
    }

    protected List<WebElement> getColumnsForTableRow(WebElement element){
        return element.findElements(By.xpath(".//div[contains(@class,'v-table-cell-wrapper')]"));
    }


    protected WebElement getButton(String text) {
        return getButton(null, text);
    }

    protected WebElement getButton(WebElement parentElement, String text){
        String xPath = "//span[contains(@class, 'v-button-caption') or contains(@class, 'v-nativebutton-caption')][contains(text(),'" + text + "')]";
        WebElement button;
        if(parentElement != null) {
            button =parentElement.findElement(By.xpath("." + xPath));
        }else {
            button = findElement(By.xpath(xPath));
        }
        return button;
    }
    
    protected void doPostOperationChecks() {
        assertNoExceptionOccurred();
        doWalkThroughWait();
        //enable to do get a screenshot history of the test
//        takeScreenshot();
    }

    protected abstract void doPreOperationChecks();

    protected abstract void assertNoExceptionOccurred();

    protected void doWalkThroughWait() {
        if (walkThroughWaitMillis != null) {
            System.out.println("Walk-through Wait: "+ walkThroughWaitMillis+"ms");
            sleep(walkThroughWaitMillis);
        }
    }

    protected void clickButton(String value, String failureMessage, WebElement parent) {
        doPreOperationChecks();
        logStep.info("Clicking button in a subsection of the page with value: " + value);
        WebElement button = getButton(parent, value);
        assertNotNull(failureMessage, button);
        button.click();
        doPostOperationChecks();
    }
    
    protected void clickButton(String value, String failureMessage) {
        doPreOperationChecks();
        logStep.info("Clicking button with value: " + value);
        WebElement button = getButton(value);
        assertNotNull(failureMessage, button);
        button.click();
        doPostOperationChecks();
    }

    protected void clickButton(String value) {
        clickButton(value, "Could not find button with label " + value + " to click");
    }

    protected void clickSaveButton(){
        clickButton("Save", "Save button was not found");
    }

    protected void clickApplyButton(){
        clickButton("Apply", "Apply button was not found");
    }

    protected void clickEditButton(){
        clickButton("Edit", "Edit button was not found");
    }

    protected void clickCancelButton(WebElement parent){
        clickButton("Cancel", "Cancel button was not found", parent);
    }
    
    protected void clickCancelButton(){
        clickButton("Cancel", "Cancel button was not found");
    }

    protected void clickOkButton() {
        clickOkButton(null);
    }

    protected void clickOkButton(WebElement parent){
        clickButton("Ok", "Ok button was not found", parent);
    }

    protected WebElement findElement(final By by) {
        return findElementInternal(by);
    }
    protected WebElement findElementInternal(final By by) {
//        System.out.println("Find element count: " + findElementCount++);
        long startMillis = System.currentTimeMillis();
        List<WebElement> elements = getDriverWrapper().findElements(by); //findElements() is faster when expecting element not to be found
        WebElement element;
        if (elements.isEmpty()) {
            element = null;
        } else {
            element = elements.get(0);
        }
        long endMillis = System.currentTimeMillis();
        long duration = endMillis - startMillis;
//        if (duration > SLOW_FIND_ELEMENTS_WARNING_MILLIS) {
//            System.out.println("slow findElement() in " + duration + " ms for: " + by);
//        }
        return element;

    }

    protected List<WebElement> findElements(final By by) {
        long startMillis = System.currentTimeMillis();
        List<WebElement> elements = getDriverWrapper().findElements(by);
        long endMillis = System.currentTimeMillis();
        long duration = endMillis - startMillis;
//        if (duration > SLOW_FIND_ELEMENTS_WARNING_MILLIS) {
//            System.out.println("slow findElements() in " + duration + " ms for: " + by);
//        }
        return elements;
    }

    protected String getTabsAsText() {
        List<WebElement> elements = findElements(com.vaadin.testbench.By.xpath("//div[contains(@class, 'v-tabsheet-tabitem')]/div/div"));
        if (elements.isEmpty()) {
            return "No tabs found.";
        }
        return joinFrom(elements, ", ").getText();
    }

    protected void clickTab(String title) {
      clickTab(null, title);
    }

    protected void clickTab(WebElement element, String title){
        doPreOperationChecks();
        logStep.info("Clicking tab with title: " + title);
        String xpathValue = "//div[contains(@class, 'v-tabsheet-tabitem')]/div/div[contains(text(), '" + title + "')]";
        WebElement tabDiv;
        if( element != null){
            tabDiv =element.findElement(By.xpath("."+ xpathValue));
        }else{
            tabDiv = findElement(By.xpath(xpathValue));
        }
        assertNotNull(tabDiv);
        tabDiv.click();
        assertTabSelected(title);
        doPostOperationChecks();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void openWithoutAutoLogin(String pageUrl) {
        open(pageUrl, false);
    }
    
    public void open(String pageUrl) {
        open(pageUrl, true);
    }
    
    private void open(String pageUrl, boolean performAutoLogin) {
        doPreOperationChecks();
        openInternal(pageUrl);
        if (performAutoLogin && loginIfNeededInternal()) {
            //doesn't jump to final url, so we need to try again.
            openInternal(pageUrl);
        }
        doPostOperationChecks();
    }

    /**
     * Not used anymore, seemed like Vaadin might be releasing itself to early after load, but now we just retry the open when that happens
     */
    private void blockUntilAppLoaded() {
        //v-app-loading
        WebDriverWait wait = new WebDriverWait(getDriverWrapper(), 10, 100);
        wait.until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(@Nullable WebDriver webDriver) {
                WebElement appLoadingDiv = findElement(By.className("v-loading-indicator"));
                if (appLoadingDiv != null) {
                    System.out.println("v-loading-indicator found, displayed: " + appLoadingDiv.isDisplayed());
                } else {
                    System.out.println("****SOURCE:");
                    System.out.println(getDriverWrapper().getPageSource());
                }
                boolean loadingDivIsDisplayed = appLoadingDiv != null && appLoadingDiv.isDisplayed();
                if (loadingDivIsDisplayed) {
                    System.out.println("v-loading-indicator is displayed, shouldn't Base Testbench be catching this?!?");
                    //System.out.println(getDriverWrapper().getPageSource());
                }
                return !loadingDivIsDisplayed;
            }
        });
    }

    /**
     * Return true if no auto-redirect after login occurs and login was needed 
     */
    protected abstract boolean loginIfNeededInternal();

    private void openInternal(final String pageUrl) {
//        WebElement appLoadingDiv = findElement(By.className("v-loading-indicator"));
        //In some odd cases, appLoadingDiv is shown (seems to be an underlying vaadin startup error occasionally
        
        final String url = getUrl(pageUrl);
        logStep.info("Opening page : " + url);
        getDriverWrapper().get(url);
        
        WebDriverWait wait = new WebDriverWait(getDriverWrapper(), 10, 500);
        wait.until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(@Nullable WebDriver webDriver) {
                WebElement appLoadingDiv = findElement(By.className("v-loading-indicator"));
                boolean loadingDivIsDisplayed = appLoadingDiv != null && appLoadingDiv.isDisplayed();
                if (loadingDivIsDisplayed) {
                    logStep.error("v-loading-indicator is still displayed (TestBench would normally block until this is gone), retrying page load to avoid this intermittent Vaadin hiccup(turn driver logging on the see js exception");
                    getDriverWrapper().get(url);
                }
                return !loadingDivIsDisplayed;
            }
        });

        System.out.println("Page opened  : " + url);//not logStep since this doesn't seem like key step info.
    }

    private String getUrl(String pageUrl) {
        String url;
        if (pageUrl.startsWith("http")) {
            url = pageUrl;
        } else {
            url = BASE_URL + pageUrl;
        }
        return url;
    }

    protected void assertSuccessMessage() {
        logStep.info("Asserting success message is seen...");
        assertNoWarningMessage();
        final WebElement notification = getSuccessMessageElement();
        assertNotNull("The success message should have been displayed.", notification);
        new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    return testBenchElement(((WebElementWrapper) notification).getWrappedElement()).closeNotification(); //just clicking it causes it to fade away so next command fails
                } catch (StaleElementReferenceException ignored) {
                    //2nd chance
                    WebElement notificationSecondChance = getSuccessMessageElement();
                    if (notificationSecondChance != null) {
                        notificationSecondChance.click();
                        sleep(200); //to allow fade away to occur
                    }
                }
                return false;
            }
        }).run();

    }

    protected void assertNoWarningMessage() {
        assertNull("A warning message should not have been displayed.", getWarningMessageElement());
    }

    private WebElement getSuccessMessageElement() {
        return findElement(By.xpath("//div[contains(@class,'success-notification')]"));
    }

    protected String getElementValue(WebElement element){
        return element.getAttribute("value");
    }

    protected void assertElementValueMatches (WebElement element, String expectedValue){
        assertEquals(expectedValue, getElementValue(element));
    }

    protected void assertWarningMessage() {
        logStep.info("Asserting warning message is seen...");
        assertNull("A success message should not have been displayed.", getSuccessMessageElement());
        final WebElement notification = getWarningMessageElement();
        assertNotNull("A warning message should have been displayed.", notification);
        new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    return testBenchElement(((WebElementWrapper) notification).getWrappedElement()).closeNotification(); //just clicking it causes it to fade away so next command fails
                } catch (StaleElementReferenceException ignored) {
                    //2nd chance
                    WebElement notificationSecondChance = getWarningMessageElement();
                    if (notificationSecondChance != null) {
                        notificationSecondChance.click();
                        sleep(200); //to allow fade away to occur
                    }
                }
                return false;
            }
        }).run();
    }

    private WebElement getWarningMessageElement() {
        return findElement(By.xpath("//div[contains(@class,'warning-notification')]"));
    }

    protected Select getSelect(String label) {
        //first attempt to find the select within a form layout
        WebElement element = findElement(By.xpath("//div[contains(@class, 'v-caption')]/span[contains(text(),'" + label + "')]/../../../td[contains(@class, 'v-formlayout-contentcell')]/div/select"));

        if(element == null){
            //select appears to be outside a form
            element = findElement(By.xpath("//span[contains(@class,'v-caption') and contains(text(),'"+label+"')]/../../div/select"));
        }

        assertNotNull("Select was not found with label:" + label, element);
        return new Select(element);
    }

    protected void setSelectValue(String caption, int index){
        doPreOperationChecks();
        logStep.info("Setting select with caption: " + caption + " to index: " + index);
        Select select = getSelect(caption);
        select.selectByIndex(index);
        doPostOperationChecks();
    }

    protected WebElement setTextInTextField(String caption, String value) {
        logStep.info("Setting text in text field with caption '" + caption + "' to '" + value + "'");
        WebElement textField = getTextField(caption);
        assertNotNull("Could not set text in textfield with caption " + caption + " because it wasn't found.", textField);
        return setTextInTextField(caption, textField, value);
    }

    private WebElement setTextInTextField(String fieldDescription, WebElement textField, String value) {
        doPreOperationChecks();
        assertNotNull("Could not set text because field was wasn't found.", textField);
        textField.click(); //so that it receives focus, otherwise sendKeys() loses characters
        textField.clear();
        
        textField.sendKeys(value);
        assertFieldHasValue(fieldDescription, textField, value);

        unblurField(textField);
        doPostOperationChecks();               
        return textField;
    }

    protected void unblurField(WebElement textField) {
        //Send TAB key - only works in Ghost driver (doesn't work in chrome, etc.)
        textField.sendKeys(Keys.TAB); //Otherwise when a "per character" text listener is used, the results of it will appear too late for hte next test assert. //Is this really working??? Only in Ghost driver.

        if (USE_CHROME_DRIVER) {
            //Also do a click on the root, works as unblur for chrome, etc but not ghost driver.
            System.out.println("TODO: find an innocuous area to click to force unblur");
            //findElement(By.id("logoContainer")).click(); //create a blur by click on a known innocuous part of the page
        }
    }

    protected void setTextInTextFieldById(String textFieldId, String value){
        logStep.info("Setting text in text field with id: " + textFieldId + " to: " + value);
        WebElement textField = getElementById(textFieldId);
        assertNotNull("Could not set text in textfield with id " + textFieldId + " because it wasn't found.", textField);
        textField.click(); //so that it receives focus, otherwise sendKeys() loses characters
        textField.clear();
        textField.sendKeys(value);
        assertFieldHasValue("id: " + textFieldId, textField, value);
        unblurField(textField);
    }

    protected WebElement setTextInTextArea(String caption, String value){
        logStep.info("Setting text in text area with caption: " + caption + " to: " + value);
        WebElement textArea = getTextArea(caption);
        assertNotNull("Could not set text in text area with caption " + caption + " because it wasn't found.", textArea);
        return setTextInTextField(caption, textArea, value);
    }

    protected WebElement setLocalDateTimeInDateField(String caption, LocalDateTime localDateTime) {
        doPreOperationChecks();
        String localDateAsString = localDateTime == null ? "" : localDateTime.toString("MM/dd/yyyy hh:mm aaa");
        logStep.info("Setting date in date field with caption: " + caption + " to: " + localDateAsString);
        WebElement textField = getDateField(caption);
        assertNotNull("Could not set date in datefield with caption " + caption + " because it wasn't found.", textField);
        textField.click(); //so that it receives focus, otherwise sendKeys() loses characteres
        textField.clear();
        textField.sendKeys(localDateAsString);
        assertNoExceptionOccurred();
        assertFieldHasValue(caption, textField, localDateAsString);
        unblurField(textField);
        doPostOperationChecks();               
        return textField;
        
    }
    
    protected WebElement setLocalDateInDateField(String caption, LocalDate localDate) {
        String localDateAsString = localDate == null ? "" : localDate.toString("MM/dd/yyyy");
        return setLocalDateInDateFieldAsString(caption, localDateAsString);
    }
    
    protected WebElement setLocalDateInDateFieldAsString(String caption, String dateString) {
        doPreOperationChecks();
        logStep.info("Setting date in date field with caption: " + caption + " to: " + dateString);
        WebElement textField = getDateField(caption);
        assertNotNull("Could not set date in datefield with caption " + caption + " because it wasn't found.", textField);
        textField.click(); //so that it receives focus, otherwise sendKeys() loses characters
        textField.clear();
        textField.sendKeys(dateString);
        assertNoExceptionOccurred();
        assertFieldHasValue(caption, textField, dateString);
        unblurField(textField);
        doPostOperationChecks();               
        return textField;
    }
    
    protected void assertFieldIsReadOnly(String caption) {
        doPreOperationChecks();
        WebElement field = getField(caption);
        assertNotNull("Could not assert field is read-only with caption " + caption + " because it wasn't found.", field);
        assertThat(field.getAttribute("class")).describedAs("Field with caption " + caption + " should have been read-only").contains("v-readonly");
        doPostOperationChecks();               
    }
    

    protected WebElement getTextField(String caption) {
        return getField(caption);
    }

    protected WebElement getDateField(String caption) {
        return getField(caption);
    }

    private WebElement getField(String caption) {
        return findElement(By.xpath("//div[contains(@class,'v-caption')]/span[contains(text(),'"+caption+"')]/../../..//input"));
    }

    protected WebElement getTextArea(String caption) {
        return findElement(By.xpath("//div[contains(@class,'v-caption')]/span[contains(text(),'"+caption+"')]/../../..//textarea"));
    }

    protected WebElement getLabel(String text) {
        return findElement(By.xpath("//*[self::div or self::span][contains(@class,'v-label')][contains(text(),'" + text + "')]"));
    }

    protected void assertLabelExists(String text) {
        logStep.info("Asserting that label exists: '" + text + "'");
        WebElement label = getLabel(text);
        assertNotNull("Could not find label with text: " + text, label);
        assertTrue("Expected label found, but is not displayed: " + text, label.isDisplayed());
    }

    protected void assertLabelNotExists(String text) {
        logStep.info("Asserting that label does not exist: '" + text + "'");
        WebElement label = getLabel(text);
        if (label != null) {
            assertFalse("Unexpected label found, but and is displayed: " + text, label.isDisplayed());
        }
    }

    protected void assertDateFieldHasValue(String textFieldCaption, String expectedValue) {
        logStep.info("Asserting that date field '" + textFieldCaption + "' has value: " + expectedValue);
        WebElement field = getDateField(textFieldCaption);
        assertEquals("Date field '" + textFieldCaption + "' has wrong value", expectedValue, getElementValue(field));
    }

    protected void clickCheckBox(String caption) {
        doPreOperationChecks();
        WebElement checkBox = getCheckBox(caption);
        assertNotNull("Could not click on checkbox with caption " + caption + " because it wasn't found.");
        checkBox.click();
        doPostOperationChecks();
    }

    private WebElement getCheckBox(String caption) {
        return findElement(By.xpath("//span[contains(@class, 'v-checkbox')]/label[contains(text(), '" + caption + "')]/../input"));
    }

    /**
     * Find table cell with a particular cell value. Note this method will only look for visible cells currently displayed in the table.
     */
    protected void assertCellInTable(String tableCaption, String cellValue) {
        if (Strings.isNullOrEmpty(tableCaption)) {
            logStep.info("Asserting that cell in first table on page has value:" + cellValue);
            
        } else {
            logStep.info("Asserting that cell in table with caption: " + tableCaption + " has value:" + cellValue);
        }
        WebElement table = getTableByCaption(tableCaption);
        assertNotNull("Could not find table with caption:"+table);
        WebElement cell = getCellInTable(table, cellValue);
        assertNotNull("Could not find cell in table with value:" + cellValue, cell);
    }

    //Highly custom - Only working if you put the count in the caption using the string "Total Records" 
//    protected int getTableRowCount() {
//        WebElement element = findElement(By.xpath("//span[contains(@class, 'v-captiontext') and contains(text(),'Total Records')]"));
//        if (element == null) {
//            //try non-vaadin-caption way of placing "Row Count" on the page
//            element = findElement(By.xpath("//div[contains(@class, 'v-label') and contains(text(),'Total Records')]"));
//        }
//        assertNotNull("Could not find table row count label 'Total Records'",element);
//        String count = element.getText().split(":")[1].trim();
//        return Integer.parseInt(count);
//    }

    private WebElement getCellInTable(WebElement table, String cellValue) {
        try {
            return table.findElement(By.xpath(".//div[contains(text(), '"+cellValue+"')]"));//Note the .// that will start the search at the table element passed in.
        }
        catch(Exception e){
            return null;
        }
    }

    private WebElement getTableByCaption(String caption) {
        if (Strings.isNullOrEmpty(caption)) {
            return findElement(By.xpath("//div[contains(@class, 'v-table')]"));
        }
        return findElement(By.xpath("//span[contains(@class, 'v-captiontext') and contains(text(), '" + caption + "')]/../../div[contains(@class, 'v-table')]"));
    }

    protected void setComboboxValue(String caption, String value) {
        setComboboxValue(null, caption, value);
    }

    protected void setComboboxValue(WebElement element, String caption, String value) {
        doPreOperationChecks();
        WebElement input = null;
        String xpathValue = "//div[contains(@class, 'v-caption')]/span[contains(text(),'" + caption + "')]/ancestor::tr[contains(@class,'v-formlayout-row')][1]//input";
        if(element == null) {
            //global search
            input = findElement(By.xpath(xpathValue));
        }
        else {
            //search within element that was passed in
            input = element.findElement(By.xpath("."+xpathValue));
        }
        assertNotNull("Could not find combobox with caption '"+caption+"'", input);
        input.click();
        input.sendKeys(value);
        input.sendKeys(Keys.TAB);
        assertFieldHasValue(caption, input, value);
        doPostOperationChecks();               
    }

    protected WebElement getElementById(String elementId) {
        return findElement(By.id(elementId));
    }

    protected void assertFieldIsBlankById(String fieldId) {
        System.out.println("Checking to make sure " + fieldId + " is blank");
        WebElement element = getElementById(fieldId);
        assertNotNull("Could not find field with id " + fieldId, element);
        assertElementIsBlank(element);
    }

    protected void assertElementIsBlank(WebElement field){
        assertEquals("Field should be blank", "", trimToEmpty(getElementValue(field)));
    }

    public void setWalkThroughWaitMillis(Integer walkThroughWaitMillis) {
        this.walkThroughWaitMillis = walkThroughWaitMillis;
    }

    private static int findFreePort(){
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore IOException on close()
            }
            return port;
        } catch (IOException ignored) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
        throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on");
    }

    protected void clickBrowserBack() {
        doPreOperationChecks();
        logStep.info("Clicking browser back button");
        getDriverWrapper().navigate().back();
        logStep.info("After back, at: " + getDriverWrapper().getCurrentUrl());
        doPostOperationChecks();               
    }

    protected void clickBrowserForward() {
        doPreOperationChecks();
        logStep.info("Clicking browser forward button");
        logStep.info("After forward, at: " + getDriverWrapper().getCurrentUrl());
        getDriverWrapper().navigate().forward();
        doPostOperationChecks();               
    }

    protected void assertViewHashExists(String viewName) {
        String currentUrl = getDriverWrapper().getCurrentUrl();
        assertTrue(currentUrl.contains("!" + viewName));
    }

    protected void assertRadioButtonSelected(String formFieldCaption, String radioButtonCaption) {
        logStep.info("Asserting radio button is selected: with form caption " + formFieldCaption + " and radio button caption " + radioButtonCaption);
        WebElement radioButton = getRadioButton(formFieldCaption, radioButtonCaption);
        assertNotNull("Could not find radio button with form caption " + formFieldCaption + " and radio button caption " + radioButtonCaption, radioButton);
        assertThat(radioButton.isSelected());
    }

    protected void clickRadioButton(String formFieldCaption, String radioButtonCaption) {
        doPreOperationChecks();
        logStep.info("Clicking: radio button with form caption " + formFieldCaption + " and radio button caption " + radioButtonCaption);
        WebElement radioButton = getRadioButton(formFieldCaption, radioButtonCaption);
        assertNotNull("Could not click on checkbox with form caption " + formFieldCaption + " and radio button caption " + radioButtonCaption + " because it was not found", radioButton);
        radioButton.click();
        doPostOperationChecks();
    }
    
    private WebElement getRadioButton(String formFieldCaption, String radioButtonCaption) {
        return findElement(By.xpath("//div[contains(@class,'v-caption')]/span[contains(text(),'" + formFieldCaption + "')]/../../../td[contains(@class,'v-formlayout-contentcell')]/div/span/label[contains(text(),'" + radioButtonCaption + "')]/../input"));
    }
    
    protected void loginRestClient() {
        String loginUrl = "TODO-app-specific";
        restAuthCookies = given().
                    param("username", USERNAME).
                    param("password", PASSWORD).
                    expect().statusCode(200).post(getRestUrl() + loginUrl).getCookies();
    }

    private String getRestUrl() {
        return "TODO:put any rest url here";
    }

    protected RequestSpecification givenWithAuth(String description) {
        if (description != null) {
            logStep.info("REST call ): " + description);
        }
        if (restAuthCookies == null) {
            loginRestClient();
        }
        return given().
                cookie(SESSION_COOKIE_NAME, restAuthCookies.get(SESSION_COOKIE_NAME));
    }

    protected void refreshPage() {
        String currentUrl = getDriverWrapper().getCurrentUrl();
        logStep.info("Refreshing the page (first going to blank to avoid picking up old data: " + currentUrl);
        open("about:blank"); //to insure we are cleared, otherwise the wait hooks don't seem to work
        open(currentUrl);
    }

    public Object executeScript(String script, Object... args) {
        doPreOperationChecks();
        System.out.println("  Executing: " + script);
        Object object = ((JavascriptExecutor) getDriverWrapper()).executeScript(script, args);
        doPostOperationChecks();
        return object;
    }

    public Object executeScriptAsync(String script, Object... args) {
        doPreOperationChecks();
        System.out.println("  Executing(async): " + script);
        Object object = ((JavascriptExecutor) getDriverWrapper()).executeAsyncScript(script, args);
        doPostOperationChecks();
        return object;
    }

    public static final String EXTERNAL_OPERATION_IN_PROGRESS_JS_VARIABLE = "window.fooo_vaadin_externalOperationInProgress";
    
    protected void performSomeJsOperation(int propertyId) {
        logStep.info("performSomeJsOperation" + propertyId);
        
        executeScript(EXTERNAL_OPERATION_IN_PROGRESS_JS_VARIABLE + " = true"); //to allow testing tools to determine when the non-vaadin operation is complete. The work context change will trigger a vaadin call, which when complete will set the flag to false.
        executeScript("foooo_barr.someJsOperation(" + propertyId + ")");
        waitForExternalOperationCompleteFlag();
    }
    
    protected void waitForExternalOperationCompleteFlag() {
        WebDriverWait wait = new WebDriverWait(getDriverWrapper(), 10);
        wait.until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(@Nullable WebDriver webDriver) {
                Object inProgress = executeScript("return " + EXTERNAL_OPERATION_IN_PROGRESS_JS_VARIABLE);
                testBench().waitForVaadin(); //not sure if this matters, but is at least quick! :)
                return Boolean.FALSE.equals(inProgress);
            }
        });
    }

    protected void assertGridCellTextFieldEnabledInRow(String rowTitle, int rowIndexAfterTitle, boolean expectedEnabled, WebElement webElement) {
        logStep.info("Asserting that input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + " is enabled: " + expectedEnabled);
        WebElement field = getGridCellTextFieldInRow(rowTitle, rowIndexAfterTitle, webElement);
        assertNotNull("Could not find input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, field);
        if (expectedEnabled && isDisabled(field)) {
            fail("Text field in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + " has wrong enabled state, expected enable state: " + expectedEnabled);
        }
        if (!expectedEnabled & !isDisabled(field)) {
            fail("Text field in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + " has wrong enabled state, expected enable state: " + expectedEnabled);
        }
    }

    protected void assertGridCellTextFieldHasValueInRow(String rowTitle, int rowIndexAfterTitle, String expectedValue, WebElement webElement) {
        logStep.info("Asserting that input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + " has value: " + (Strings.isNullOrEmpty(expectedValue) ? "-Unset-" : expectedValue));
        WebElement field = getGridCellTextFieldInRow(rowTitle, rowIndexAfterTitle, webElement);
        assertNotNull("Could not find input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, field);
        assertEquals("Text field in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + "' has wrong value", expectedValue, getElementValue(field));
    }

    protected void assertGridCellLabelHasValueInRow(String rowTitle, int rowIndexAfterTitle, String expectedValue, WebElement webElement) {
        logStep.info("Asserting that label in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + " has value: " + expectedValue);
        WebElement gridCellElement = getGridCellInRow(rowTitle, rowIndexAfterTitle, webElement);
        assertNotNull("Could not find cell in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, gridCellElement);
        WebElement label = gridCellElement.findElement(By.xpath(".//div[contains(@class, 'v-label')"));
        assertNotNull("Could not find label in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, label);
        assertEquals("Label in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle + "' has wrong value", expectedValue, label.getText());
    }

    protected void setGridCellTextValueInRow(String rowTitle, int rowIndexAfterTitle, String value, WebElement baseElement) {
        WebElement field = getGridCellTextFieldInRow(rowTitle, rowIndexAfterTitle, baseElement);
        assertNotNull("Could not find input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, field);
        setTextInTextField("text field in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, field, value);
    }

    private WebElement getGridCellTextFieldInRow(String rowTitle, int rowIndexAfterTitle, WebElement baseElement) {
        WebElement gridCellElement = getGridCellInRow(rowTitle, rowIndexAfterTitle, baseElement);
        assertNotNull("Could not find cell in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, gridCellElement);
        WebElement input = gridCellElement.findElement(By.tagName("input"));
        assertNotNull("Could not find input in grid with row title '" + rowTitle + "' and index(after title): " + rowIndexAfterTitle, input);
        return input;
    }

    private WebElement getGridCellInRow(String rowTitle, int rowIndexAfterTitle, WebElement baseElement) {
        return baseElement.findElement(By.xpath(".//div[contains(@class, 'v-gridlayout')]/descendant::div[contains(@class, 'v-label') and contains(text(), '" + rowTitle + "')]" +
                "/ancestor::div[contains(@class, 'v-gridlayout-slot')][1]/following-sibling::div[" + rowIndexAfterTitle + "]"));
    }

    /**
     * Not yet workign reliably in both ChromeDriver and GhostDriver
     */
    protected void assertTooltipAppears(String fieldCaption, String tooltipText) {
        logStep.info("Asserting that tooltip appears on field '" + fieldCaption + "' with tooltip text: " + tooltipText);
        WebElement textField = getTextField(fieldCaption);

        testBenchElement(((WebElementWrapper) textField).getWrappedElement()).showTooltip();
//        System.out.println("showTooltip: " + findElement(By.xpath("//div[contains(@class,'v-tooltip')]")));
//        
//        new Actions(getDriverWrapper()).moveToElement(textField, 3, 3).build().perform();
//        System.out.println("moveToElement: " + findElement(By.xpath("//div[contains(@class,'v-tooltip')]")));
        
        assertNotNull("Tooltip not found (note: make sure to click save button or showing tooltip fails on ghost driver) with text: " + tooltipText, findElement(By.xpath("//div[contains(@class,'v-tooltip')]//div[contains(text(),'" + tooltipText + "')]")));
    }

    protected void assertDateFieldValidation(String fieldCaption, LocalDate value, String validationText) {
        assertDateFieldValidation(fieldCaption, value, validationText, false);
    }

    protected void assertDateFieldValidation(String fieldCaption, LocalDate value, String validationText, boolean isApplyButton) {
        logStep.info("Field Validation: Asserting date field '" + fieldCaption + "' set to '" + value + "' shows validation message: " + validationText);
        setLocalDateInDateField(fieldCaption, value);
        assertValidationText(fieldCaption, validationText, isApplyButton);
    }

    protected void assertFieldValidation(String fieldCaption, String value, String validationText) {
        assertFieldValidation(fieldCaption, value, validationText, false);
    }

    protected void assertFieldValidation(String fieldCaption, String value, String validationText, boolean isApplyButton) {
        logStep.info("Field Validation: Asserting field '" + fieldCaption + "' set to '" + value + "' shows validation message: " + validationText);
        setTextInTextField(fieldCaption, value);
        assertValidationText(fieldCaption, validationText, isApplyButton);
    }

    private void assertValidationText(String fieldCaption, String validationText, boolean isApplyButton) {
        if (isApplyButton) {
            clickApplyButton();
        } else {
            clickSaveButton();
        }
        logStep.info("TODO: Using simplified approach, reassert tooltip when can be done reliably");
        assertWarningMessage();
//        assertTooltipAppears(fieldCaption, validationText);
    }

    public void clickContextMenuItem(String contentMenuItemToFind) {
        WebElement element = findElement(By.xpath("//span[contains(@class,'v-button-caption') and contains(text(),'"+contentMenuItemToFind+"')]"));
        assertNotNull("Could not find the menu item '"+contentMenuItemToFind+"' in the dashboard content selection menu.", element);
        element.click();
    }

    protected void clickPieChartLabel(String name) {
        logStep.info("Looking for chart label: " + name);
        WebElement element = findElement(By.xpath("//*[name()='tspan'][contains(text(),'"+name+"')]"));
        assertNotNull("Label '" + name +"' could not be found ", element);
        element.click();
    }

    protected void assertTableIsEmpty(String tableCaption) {
        assertCellInTable(tableCaption, "No data found");
    }

//    protected void assertTableRowCount(int expectedCount) {
//        logStep.info("Asserting that first table found has row count: " + expectedCount);
//        assertEquals(getTableRowCount(), expectedCount);
//    }


    protected void selectMultiSelectOptionByIndex(String caption, int listSelectIndex) {
        logStep.info("Setting multi-select with caption: " + caption + " to index: " + listSelectIndex);
        //Chrome driver and ghost driver don't support clicking multi selects well (on change isn't fired)- bug reports - due to https://code.google.com/p/chromedriver/issues/detail?id=496&q=select&colspec=ID%20Status%20Pri%20Owner%20Summary and https://code.google.com/p/chromedriver/issues/detail?id=169#c9
        //Workaround is to arrow down to the item (which does trigger the on change logic)
        WebElement selectElement = getSelectElement(caption);

        assertNotNull("Could not find the multi-select field '" + caption + "'.", selectElement);
        
        selectElement.sendKeys(Keys.HOME);
        for (int i = 0; i < listSelectIndex; i++) {
            selectElement.sendKeys(Keys.ARROW_DOWN);
        }
    }

    protected WebElement getSelectElement(String optionGroupLabel) {
        return findElement(By.xpath("//span[contains(text(),'" + optionGroupLabel + "')]/../../..//select[contains(@class, 'v-select-select')]"));
    }

    protected WebElement getDivOrSpanWithClassAndText(String className, String text) {
        return findElement(By.xpath("//*[self::div or self::span][contains(@class,'" + className + "')][contains(text()," + escapeQuotes(text) + ")]"));
    }

    protected String selectComboBoxOptionByIndex(int optionIndex) {
        logStep.info("Selecting combobox(0) option index: " + optionIndex);
        
        WebElement comboBox = findElement(By.xpath("//div[contains(@class, 'v-filterselect')]"));
        WebElement filterSelectButton = comboBox.findElement(By.className("v-filterselect-button"));//show how to find this in the chrome dev tools
        filterSelectButton.click();
        WebElement filterInput = comboBox.findElement(By.className("v-filterselect-input"));
        filterInput.sendKeys(Keys.ARROW_DOWN); //one to enter list selection
        for (int i = 0; i < optionIndex; i++) {
            filterInput.sendKeys(Keys.ARROW_DOWN);
        }
        filterInput.sendKeys(Keys.ENTER);
        return getElementValue(filterInput);
    }

    private static class TestngAppender extends AppenderSkeleton {
        @Override
        protected void append(LoggingEvent loggingEvent) {
            Reporter.log(String.valueOf(loggingEvent.getMessage()));
        }

        @Override
        public void close() {

        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    @SuppressWarnings("IndexOfReplaceableByContains")
    protected String escapeQuotes(String toEscape) {
        // Convert strings with both quotes and ticks into: foo'"bar -> concat("foo'", '"', "bar")
        if (toEscape.indexOf("\"") > -1 && toEscape.indexOf("'") > -1) {
          boolean quoteIsLast = false;
          if (toEscape.lastIndexOf("\"") == toEscape.length() - 1) {
            quoteIsLast = true;
          }
          String[] substrings = toEscape.split("\"");
    
          StringBuilder quoted = new StringBuilder("concat(");
          for (int i = 0; i < substrings.length; i++) {
            quoted.append("\"").append(substrings[i]).append("\"");
            quoted
                .append(((i == substrings.length - 1) ? (quoteIsLast ? ", '\"')" : ")") : ", '\"', "));
          }
          return quoted.toString();
        }
    
        // Escape string with just a quote into being single quoted: f"oo -> 'f"oo'
        if (toEscape.indexOf("\"") > -1) {
          return String.format("'%s'", toEscape);
        }
    
        // Otherwise return the quoted string
        return String.format("\"%s\"", toEscape);
      }
}
