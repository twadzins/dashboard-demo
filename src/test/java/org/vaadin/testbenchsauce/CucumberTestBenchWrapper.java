package org.vaadin.testbenchsauce;

//import cucumber.api.Scenario;

import com.vaadin.demo.dashboard.uitest.DemoTestBenchTestCase;

public class CucumberTestBenchWrapper extends DemoTestBenchTestCase {
    public CucumberTestBenchWrapper() {
        System.out.println("****CucumberTestBenchWrapper created");
    }


//    public void afterCucumber(Scenario scenario) {
//        if (scenario.isFailed())    {
//            onTestFailure(scenario.getStatus(), scenario.getStatus());
//        }
//        quitDriver();
//    }
}
