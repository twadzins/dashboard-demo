package org.vaadin.testbenchsauce.webdriverwrapper;

import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.vaadin.testbenchsauce.Retry;
import org.vaadin.testbenchsauce.Retryable;

public class NavigationWrapper implements WebDriver.Navigation {
    private final WebDriver.Navigation navigation;

    public NavigationWrapper(WebDriver.Navigation navigation) {
        this.navigation = navigation;
    }

    @Override
    public void back() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                navigation.back();
                return null;
            }
        }).run();
    }

    @Override
    public void forward() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                navigation.forward();
                return null;
            }
        }).run();
    }

    @Override
    public void to(final String url) {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                navigation.to(url);
                return null;
            }
        }).run();

    }

    @Override
    public void to(final URL url) {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                navigation.to(url);
                return null;
            }
        }).run();

    }

    @Override
    public void refresh() {
        new Retry<Void>(new Retryable<Void>() {
            @Override
            public Void run() {
                navigation.refresh();
                return null;
            }
        }).run();

    }
}
