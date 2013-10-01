Vaadin TestBench/Selenium enhancements - uses Vaadin's QuickTickets Dashboard Demo
==================================

NOTE: README is in progress. Project is not quite ready for general use yet, but, sure, keep reading.. 

This project presents a few ideas and Java code for making UI testing with Selenium/Vaadin TestBench more fun.
 
And by "fun", I mean that it: 

*   Minimizes boilerplate code with a pattern of sharing common actions and applying automatic asserts where possible.
*   Reduces sources of false negatives so tests can be a reliable part of Continuous Integration builds.
*   Test output that "automatically" tells a story that makes failure troubleshooting easier.
*   Runs headless using GhostDriver. Optionally you can toggle on the included ChromeDriver or whatever other web driver you want to install. 
*   Creates screenshots on failure that are visible in a Jenkins build (and optionally create screenshots during each action).
*   Test runs very fast (2-3 seconds for a simple test on modern hardware), due to: tuning slow operations such as findElement(), no arbitrary sleeps, using GhostDriver, avoiding re-login, unless needed, on each test when run in a suite.
*   Increases testing visibility within your organization - Integrates with Continuous Integration(CI)/Build tools such as Jenkins.
*   Increases testing visibility within your organization - Integrates with Story and Test Case management tools such as Rally (auto-update Rally testcases with test results when CI build runs)
*   Use of TestNG groups to achieve features like "incubator", making it so that UI tests don't break the CI build until they have been shown to be stable.

Much of the code here is specific to the testing apps built with the Vaadin framework, but conceptually should be applicable to other UI testing efforts using Selenium.

Example Test code and output
==

        public class DashboardUiTest extends DemoTestBenchTestCase {
        
            private void openPageUnderTest() {
                open("#!/dashboard");
            }
        
            @Test
            public void editDashboardTitle() throws Exception {
                openPageUnderTest(); //will login if needed.
                
                clickIconButton(IconButtonConstants.EDIT); //automatically asserts that button exists (and that no exceptions occur)
                
                String newTitle = "Random Title: " + RandomStringUtils.randomAlphabetic(12); //randomize so we are not asserting some previously saved value
                setTextInTextField("Dashboard Name", newTitle); //id avoidance
                clickSaveButton(); //auto-asserts that no warning occurred
                
                assertLabelExists(newTitle);
            }
        
            //......
        }

To run
==
Run the Maven install target and deploy the resulting WAR-file to your server. After running maven install once, I suggest using your IDE to compile the app and deploy the war. Then app changes and testand test changes and use JRebel to push the app changes out on the fly 

TODO: point out how to enable the maven profile for the ui tests to occur

How to use in you own project
==
There is not yet any Maven artifact for the underlying test library. So, for now, to use:
- Copy all of src/test into your src/test directory.
- Extend org.vaadin.testbenchsauce.BaseTestBenchTestCase using DemoTestBenchTestCase as a guide, for example MyGreatAppTestBenchTestCase.
- Create test classes that extend your MyGreatAppTestBenchTestCase, using the example tests under com.vaadin.demo.dashboard.uitest as a guide.
- When you are ready, you can remove the Demo Dashboard specific example tests that are under com.vaadin.demo.dashboard.uitest
Note: There are two included Selenium driver apps that are windows specific. For other OS's, alter BaseTestBenchTestCase to change 3 references from ".exe" (or submit a code change making this automatic :)) 

Notes about feature added inside BaseTestBenchTestCase
==
*   Performance - basic findElement is slow if element doesn't exist, so override findElement to call findElements (which is fast) and returns the first element.
*   Starts up background phantomjs process for fast GhostDriver operation.
*   Reliability - Selenium Driver and associated objects (like WebElement) are wrapped to perform automatic retry to overcome WebDriverException hiccups that occur un     
Integration with Story and Test Case management 
*   Two paths are windows specific - TODO: document these

Annoyances/TODO'S
==
* Dealing with more than one of the same selection on a page (narrowing to subelements, providing indexes)
* The superclass (BaseTestBenchTestCase) is getting superlarge. Consider break out to Assert object, though that has typical static downsides.  
* Add multi-login support (maybe with @LogInAs("") style test annotation) that does a login to another user (if that user isn't already the currently logged in user)
* Complete v1 of this document

Contributors
==
*   Tom 'T3' Wadzinski
*   Dan 'Flash Dan/Vaadan' Nelson.

Licenses
==
The source code is released under Apache 2.0.

The application uses the Vaadin Charts add-on, which is released under the Commercial Vaadin Addon License: https://vaadin.com/license/cval-2.0

The application uses the Vaadin TestBench add-on, which is released under the Commercial Vaadin Addon License: https://vaadin.com/license/cval-2.0
