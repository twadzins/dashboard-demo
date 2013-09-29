package org.vaadin.testbenchsauce.webdriverwrapper;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsElement;
import org.vaadin.testbenchsauce.Retry;
import org.vaadin.testbenchsauce.Retryable;

public class WebElementWrapper implements WebElement, WrapsElement, Locatable {
    private final WebElement wrappedElement;

    public WebElementWrapper(WebElement wrappedElement) {
        this.wrappedElement = wrappedElement;
    }

    @Override
    public void click() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                wrappedElement.click();
                return null;
            }
        }).run();
    }

    @Override
    public void submit() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                wrappedElement.submit();
                return null;
            }
        }).run();
    }

    @Override
    public void sendKeys(final CharSequence... keysToSend) {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                wrappedElement.sendKeys(keysToSend);
                return null;
            }
        }).run();
    }

    @Override
    public void clear() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                wrappedElement.clear();
                return null;
            }
        }).run();
    }

    @Override
    public String getTagName() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return wrappedElement.getTagName();
            }
        }).run();
    }

    @Override
    public String getAttribute(final String name) {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return wrappedElement.getAttribute(name);
            }
        }).run();
    }

    @Override
    public boolean isSelected() {
        return new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                return wrappedElement.isSelected();
            }
        }).run();
    }

    @Override
    public boolean isEnabled() {
        return new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                return wrappedElement.isEnabled();
            }
        }).run();
    }

    @Override
    public String getText() {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return wrappedElement.getText();
            }
        }).run();
    }

    @Override
    public List<WebElement> findElements(final By by) {
        List<WebElement> elements = new Retry<List<WebElement>>(new Retryable<List<WebElement>>() {
            @Override
            public List<WebElement> run() {
                try {
                    return wrappedElement.findElements(by);
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
        WebElement childElement = new Retry<WebElement>(new Retryable<WebElement>() {
            @Override
            public WebElement run() {
                try {
                    return wrappedElement.findElement(by);
                } catch (NoSuchElementException e) {
                    return null;
                }
            }
        }).run();
        if (childElement != null) {
            childElement = new WebElementWrapper(childElement);
        }
        return childElement;
    }

    @Override
    public boolean isDisplayed() {
        return new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                return wrappedElement.isDisplayed();
            }
        }).run();
    }

    @Override
    public Point getLocation() {
        return new Retry<Point>(new Retryable<Point>() {
            @Override
            public Point run() {
                return wrappedElement.getLocation();
            }
        }).run();
    }

    @Override
    public Dimension getSize() {
        return new Retry<Dimension>(new Retryable<Dimension>() {
            @Override
            public Dimension run() {
                return wrappedElement.getSize();
            }
        }).run();
    }

    @Override
    public String getCssValue(final String propertyName) {
        return new Retry<String>(new Retryable<String>() {
            @Override
            public String run() {
                return wrappedElement.getCssValue(propertyName);
            }
        }).run();
    }

    @Override
    public WebElement getWrappedElement() {
        return wrappedElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final WebElementWrapper that = (WebElementWrapper) o;

        return new Retry<Boolean>(new Retryable<Boolean>() {
            @Override
            public Boolean run() {
                return !(wrappedElement != null ? !wrappedElement.equals(that.wrappedElement) : that.wrappedElement != null);
            }
        }).run();
    }

    @Override
    public int hashCode() {
        return wrappedElement != null ? wrappedElement.hashCode() : 0;
    }

    @Override
    public Coordinates getCoordinates() {
        return wrappedElement != null ? ((Locatable)wrappedElement).getCoordinates() : null;
    }
}
