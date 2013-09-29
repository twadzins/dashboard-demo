package org.vaadin.testbenchsauce.webdriverwrapper;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class TargetLocatorWrapper implements WebDriver.TargetLocator {
    private final WebDriver.TargetLocator targetLocator;

    public TargetLocatorWrapper(WebDriver.TargetLocator targetLocator) {
        this.targetLocator = targetLocator;
    }

    @Override
    public WebDriver frame(int index) {
        return new WebDriverWrapper(targetLocator.frame(index));
    }

    @Override
    public WebDriver frame(String nameOrId) {
        return new WebDriverWrapper(targetLocator.frame(nameOrId));
    }

    @Override
    public WebDriver frame(WebElement frameElement) {
        return new WebDriverWrapper(targetLocator.frame(frameElement));
    }

    @Override
    public WebDriver window(String nameOrHandle) {
        return new WebDriverWrapper(targetLocator.frame(nameOrHandle));
    }

    @Override
    public WebDriver defaultContent() {
        return new WebDriverWrapper(targetLocator.defaultContent());
    }

    @Override
    public WebElement activeElement() {
        return new WebElementWrapper(targetLocator.activeElement());
    }

    @Override
    public Alert alert() {
        return targetLocator.alert();
    }
}
