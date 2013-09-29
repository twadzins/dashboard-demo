package com.vaadin.demo.dashboard.uitest;

import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;
import org.vaadin.testbenchsauce.RallyTestCase;

import static org.testng.AssertJUnit.assertNotNull;

public class SalesUiTest extends DemoTestBenchTestCase {

    public static final String EMPTY_CHART_CAPTION = "Add a data set from the dropdown above";

    @Test(invocationCount = 1)
    @RallyTestCase("TC123")
    public void clearTimelineAndAddSeries() throws Exception {
        open();
        clickButton("Clear");
        assertLabelExists(EMPTY_CHART_CAPTION);
        
        String selectedMovieTitle = selectComboBoxOptionByIndex(5); //TODO: target this with a caption or allow for an element index
        
        clickButton("Add");
        //prove that this actually added something to the timeline
        assertTimelineLegendLabelExists(selectedMovieTitle);

    }

    private void assertTimelineLegendLabelExists(String label) {
        logStep.info("Asserting that timeline legend label exists: '" + label + "'");
        WebElement webElement = getDivOrSpanWithClassAndText("v-timeline-widget-legend-label", label);
        assertNotNull("Could not find timeline legend label: " + label, webElement);
        
    }

    private void open() {
        open("#!/sales");
    }

}
