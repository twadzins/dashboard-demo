package com.vaadin.demo.dashboard.uitest;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.vaadin.testbenchsauce.BaseTestBenchTestCase;

import static org.fest.assertions.api.Assertions.fail;
import static org.testng.AssertJUnit.assertNotNull;

public class DemoTestBenchTestCase extends BaseTestBenchTestCase {


    protected void doPreOperationChecks() {
        assertNoWarningMessage();
    }
    
    protected void assertNoExceptionOccurred() {
        String systemErrorDetailsId = "systemErrorDetailsId";
        WebElement systemErrorDetailsElement = findElement(By.id(systemErrorDetailsId));//Using id for max speed on this call since it is done several times per test...
        if (systemErrorDetailsElement != null) {
            fail(getExceptionText(systemErrorDetailsId));
        }
    }

    private String getExceptionText(String systemErrorDetailsId) {
        //exception text is hidden from user so must get via JS (selenium can't getText() on display:none elements
        String script = "return document.getElementById('" + systemErrorDetailsId + "').innerHTML";
        String detailsText = String.valueOf(((JavascriptExecutor) getDriverWrapper()).executeScript(script));
        return "Unhandled Exception Occurred on the web page, UI Exception:\n" + detailsText;
    }

    private WebElement getExceptionHeaderDiv() {
        return findElement(By.id("exceptionDetailId"));//Using id for speed on this call since it is done several times per test...
    }

    protected boolean loginIfNeededInternal() {
        WebElement loginPageIndicatorElement = findElement(By.className("login-layout"));
        if (loginPageIndicatorElement != null) {
            String username = USERNAME;
            logStep.info("Not Logged in, logging in as '" +  username + "'");
            clickButton("Sign In");
            return false;
        }
        return false;
    }

    protected void clickIconButton(String className) {
        doPreOperationChecks();
        logStep.info("Clicking image button with icon class: " + className);
        WebElement button = getIconButton(className);
        assertNotNull("Could not find image button with icon class: " + className, button);
        button.click();
        doPostOperationChecks();
    }

    protected WebElement getIconButton(String className) {
        return findElement(By.className(className));
    }

}
