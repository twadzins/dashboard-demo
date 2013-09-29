package org.vaadin.testbenchsauce.webdriverwrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.HasInputDevices;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keyboard;
import org.openqa.selenium.Mouse;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.vaadin.testbenchsauce.Retry;
import org.vaadin.testbenchsauce.Retryable;

public class WebDriverWrapper implements WebDriver, TakesScreenshot, JavascriptExecutor, HasInputDevices {
    private final WebDriver driver;

    public WebDriverWrapper(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public void get(final String url) { 
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                driver.get(url);
                return null;
            }
        }).run();
    }

    @Override
    public String getCurrentUrl() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return driver.getCurrentUrl();
            }
        }).run();
    }

    @Override
    public String getTitle() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return driver.getTitle();
            }
        }).run();
    }

    @Override
    public List<WebElement> findElements(final By by) {
        List<WebElement> elements = new Retry<List<WebElement>>(new Retryable<List<WebElement>>() {
            @Override
            public List<WebElement> run() {
                try {
                    return driver.findElements(by);
                } catch (NoSuchElementException e) {
                    return new ArrayList<WebElement>();
                }
            }
        }).run();
        List<WebElement> wrappedElements = new ArrayList<WebElement>();
        if (elements != null) {
            for (WebElement element : elements) {
                wrappedElements.add(new WebElementWrapper(element));
            }
        }
        return wrappedElements;
    }

    @Override
    public WebElement findElement(final By by) {
        WebElement element = new Retry<WebElement>(new Retryable<WebElement>() {
            @Override
            public WebElement run() {
                try {
                    return driver.findElement(by);
                } catch (NoSuchElementException e) {
                    return null;
                }
            }
        }).run();
        if (element != null) {
            element = new WebElementWrapper(element);
        }
        return element;
    }

    @Override
    public String getPageSource() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return driver.getPageSource();
            }
        }).run();
    }

    @Override
    public void close() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                driver.close();
                return null;
            }
        }).run();

    }

    @Override
    public void quit() {
        System.out.println("Quitting webdriver");
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public synchronized Void run() {
                driver.quit();
                return null;
            }
        }).run();

    }

    @Override
    public Set<String> getWindowHandles() {
        return new Retry<Set<String>>(new Retryable<Set<String>>() {
            @Override
            public Set<String> run() {
                return driver.getWindowHandles();
            }
        }).run();
    }

    @Override
    public String getWindowHandle() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return driver.getWindowHandle();
            }
        }).run();
    }

    @Override
    public TargetLocator switchTo() {
        return new Retry<TargetLocator>(new Retryable<TargetLocator>() {
            @Override
            public TargetLocator run() {
                return new TargetLocatorWrapper(driver.switchTo());
            }
        }).run();
    }

    @Override
    public Navigation navigate() {
        return new Retry<Navigation>(new Retryable<Navigation>() {
            @Override
            public Navigation run() {
                return new NavigationWrapper(driver.navigate());
            }
        }).run();
    }

    @Override
    public Options manage() {
        return new Retry<Options>(new Retryable<Options>() {
            @Override
            public Options run() {
                return driver.manage();
            }
        }).run();
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return ((TakesScreenshot)driver).getScreenshotAs(target);
    }

    @Override
    public Object executeScript(final String script, final Object... args) {
        return new Retry<Object>(new Retryable<Object>() {
            @Override
            public Object run() {
                return ((JavascriptExecutor)driver).executeScript(script, args);
            }
        }).run();
    }

    @Override
    public Object executeAsyncScript(final String script, final Object... args) {
        return new Retry<Object>(new Retryable<Object>() {
            @Override
            public Object run() {
                return ((JavascriptExecutor)driver).executeAsyncScript(script, args);
            }
        }).run();
    }

    @Override
    public Keyboard getKeyboard() {
        return ((HasInputDevices)driver).getKeyboard();
    }

    @Override
    public Mouse getMouse() {
        return ((HasInputDevices)driver).getMouse();
    }
}
