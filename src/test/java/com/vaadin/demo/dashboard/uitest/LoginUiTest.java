package com.vaadin.demo.dashboard.uitest;

import com.vaadin.demo.dashboard.IconButtonConstants;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

public class LoginUiTest extends DemoTestBenchTestCase {

    public static final String SIGN_IN_CAPTION = "Sign In";

    @Test
    public void testLoginFailureThenSuccess() throws Exception {
        openWithoutAutoLogin(""); //root page
        logoutIfNeeded(); //session might already be active if this test is part of a suite

        setTextInTextField("Username", "badguy");
        clickButton(SIGN_IN_CAPTION);
        assertLabelExists("Wrong username or password.");

        setTextInTextField("Username", "");
        clickButton(SIGN_IN_CAPTION);
        assertLabelExists("My Dashboard");
    }

    private void logoutIfNeeded() {
        WebElement logoutButton = getIconButton(IconButtonConstants.LOGOUT);
        if (logoutButton != null) {
            logoutButton.click();
        }
    }

}
