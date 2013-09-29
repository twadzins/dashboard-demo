package com.vaadin.demo.dashboard.uitest;

import com.vaadin.demo.dashboard.IconButtonConstants;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.Test;
import org.vaadin.testbenchsauce.RallyTestCase;

public class DashboardUiTest extends DemoTestBenchTestCase {

    
    @Test(groups = "smokeTest") //smokeTest - tests that don't alter data that could be used in higher environments such as production 
    @RallyTestCase("ABC123")
    public void smokeTest() throws Exception {
//        setWalkThroughWaitMillis(1000); //uncomment to intentionally slow dow the test to easier see what is happening
        openPageUnderTest();
        
        clickButton("Sales");
        clickButton("Transactions");
        clickButton("Reports");
        clickButton("Schedule");
        clickButton("Dashboard");
    }

    @Test
    public void editDashboardTitle() throws Exception {
        //setWalkThroughWaitMillis(1000); //uncomment to intentionally slow dow the test to easier see what is happening
        openPageUnderTest(); //will login if needed.
        
        clickIconButton(IconButtonConstants.EDIT); //automatically asserts that button exists (and that no exceptions occur)
        
        String newTitle = "Random Title: " + RandomStringUtils.randomAlphabetic(12); //randomize so we are asserting some previously saved value
        setTextInTextField("Dashboard Name", newTitle); //id avoidance
        clickSaveButton(); //auto-asserts that no warning occurred
        
        assertLabelExists(newTitle);
    }

    @Test
    public void editDashboardTitleThenCancel() throws Exception {
        openPageUnderTest(); //will login if needed.
        
        clickIconButton(IconButtonConstants.EDIT); //automatically asserts that button exists (and that no exceptions occur)
        
        String newTitle = "Title-" + RandomStringUtils.randomAlphabetic(12); //randomize so we are asserting some previously saved value
        setTextInTextField("Dashboard Name", newTitle);
        
        clickSaveButton();
        
        assertLabelExists(newTitle);

        clickIconButton(IconButtonConstants.EDIT); //automatically asserts that button exists (and that no exceptions occur)
        setTextInTextField("Dashboard Name", "Will not be saved since we cancelled");
        clickCancelButton();
        
        assertLabelExists(newTitle);
    }

    private void openPageUnderTest() {
        open("#!/dashboard");
    }

}
