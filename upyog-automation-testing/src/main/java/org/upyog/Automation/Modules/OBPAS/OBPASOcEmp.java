package org.upyog.Automation.Modules.OBPAS;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.Utils.DriverFactory;
import org.upyog.Automation.Utils.TestDataStore;
import org.upyog.Automation.config.WebDriverFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OBPASOcEmp {

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void OBPASOcInbox() {
        OBPASOcInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("obpas.login.username"),
                ConfigReader.get("obpas.login.password"),
                ConfigReader.get("obpas.application.number"));
    }

    public void OBPASOcInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        System.out.println("Advertisement Application Employee Workflow");

        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to Search Application
            navigateToSearchApplication(driver, wait, js);

            // STEP 3: Search Application by Application Number
            searchByApplication(driver, wait, js, applicationNumber);

            // STEP 4: Take Action Forward Document Verifier
            takeActionAndForward(driver, wait, js);

            // STEP 5: Pop Up Forward Document Verifier
            handleForwardPopup(driver, wait, js);

            // STEP 6: Go to Home Page For Field Inspection
            takeActionGoToHome(driver, wait, js);

            // STEP 7; Inbox Again For Field Inspector
            navigateToSearchApplicationAgain(driver, wait, js);

            // STEP 8: Search Application by Application Number Again Field Inspector
            searchByApplicationAgain(driver, wait, js, applicationNumber);

            // STEP 9: Inspection Report Field Inspector
            inspectionReport(driver, wait, js);

            // STEP 10: Pop Up Forward Again Field Inspector
            handleForwardPopupAgain(driver, wait, js);

            // STEP 11: Go to Home Page Again For Fire NOC
            takeActionGoToHomeAgain(driver, wait, js);

            // STEP 12: Inbox Again1 Fire NOC
            navigateToSearchApplicationAgain1(driver, wait, js);

            // STEP 13: Search Application by Application Number Again1 Fire NOC
            searchByApplicationAgain1(driver, wait, js, applicationNumber);

            // STEP 14: Take Action Forward1 Fire NOC
            takeActionAndForward1(driver, wait, js);

            // STEP 15: Pop Up Forward Fire NOC
            handleForwardPopup1(driver, wait, js);

            // STEP 16: Go to Home Page Again2 For OBPASOc NOC
            takeActionGoToHomeAgain2(driver, wait, js);

            // STEP 17: Inbox Again2 OBPASOc NOC
            navigateToSearchApplicationAgain2(driver, wait, js);

            // STEP 18: Search Application Again 2
            searchByApplicationAgain2(driver, wait, js, applicationNumber);

            // STEP 19: Take Action Forward1 OBPASOc NOC
            takeActionAndForward2(driver, wait, js);

            // STEP 20: Pop Up Forward OBPASOc NOC
            handleForwardPopup2(driver, wait, js);

            // STEP 21: Go to Home Page Again1 For Approver
            takeActionGoToHomeAgain3(driver, wait, js);

            // STEP 22: Inbox Again1 Approver
            navigateToSearchApplicationAgain3(driver, wait, js);

            // STEP 23: Search Application Again 2
            searchByApplicationAgain3(driver, wait, js, applicationNumber);

            // STEP 24: Take Action Approver
            takeActionAndApprover(driver, wait, js);

            // STEP 25: Pop Up  Approver
            handleApproverPopup(driver, wait, js);

            // STEP 26: Go to Home Page Again4
            takeActionGoToHomeAgain4(driver, wait, js);


            System.out.println("OBPASOc Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            System.out.println("Exception in OBPASOc Application Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
           if (driver != null) {
                driver.quit();
            }}
    }

    // =====================================================================
    // STEP 1: EMPLOYEE LOGIN
    // =====================================================================


    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        System.out.println("Open the Employee Login Portal");

        // Enter credentials from configuration
        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        System.out.println("Filled username and password");

        // Select city dropdown
        selectCityDropdown(driver, wait, actions);

        // Click Continue button
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");


        // =====================================================================
        // STEP 2: SEARCH APPLICATION
        // =====================================================================
    }

    private void navigateToSearchApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Property Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='OBPS']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='OBPS Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 3: SEARCH APPLICATION BY APPLICATION NUMBER
    // =====================================================================


    private void searchByApplication(WebDriver driver, WebDriverWait wait,
                                     JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String obpasId = applicationNumber.trim();
        System.out.println("Application Number " + obpasId);

        WebElement obpasInput = null;

        try {
            obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNo")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasInput);
        Thread.sleep(500);

        obpasInput.clear();
        obpasInput.sendKeys(obpasId);

        System.out.println("Application Number entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        System.out.println("Search button clicked");

        By applicationLinkLocator = By.xpath("//a//span[contains(text(),'" + obpasId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.elementToBeClickable(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + obpasId);
    }

    // =====================================================================
    // STEP 4: TAKE ACTION FORWARD
    // =====================================================================

    private void takeActionAndForward(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Forward");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // FORWARD CLICK

        By forwardLocator = By.xpath("//p[normalize-space()='FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Forward clicked");
    }

    // =====================================================================
    // STEP 5: POP UP FORWARD
    // =====================================================================

    private void handleForwardPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Forward Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);


        // =========================
        // STEP 2: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Forwarded");

        System.out.println("Comment entered");

        // =========================
        // STEP 3: CLICK FORWARD BUTTON
        // =========================
        By forwardBtnLocator = By.xpath("//button[normalize-space()='VERIFY AND FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Final Forward clicked");
    }

    // =====================================================================
    // STEP 6: TAKE ACTION GO TO HOME
    // =====================================================================

    private void takeActionGoToHome(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }

    // =====================================================================
    // STEP 7: SEARCH APPLICATION AGAIN
    // =====================================================================

    private void navigateToSearchApplicationAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Property Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='OBPS']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='OBPS Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 8: SEARCH APPLICATION BY APPLICATION NUMBER AGAIN
    // =====================================================================


    private void searchByApplicationAgain(WebDriver driver, WebDriverWait wait,
                                          JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String obpasId = applicationNumber.trim();
        System.out.println("Application Number " + obpasId);

        WebElement obpasInput = null;

        try {
            obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNo")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasInput);
        Thread.sleep(500);

        obpasInput.clear();
        obpasInput.sendKeys(obpasId);

        System.out.println("Application Number entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        System.out.println("Search button clicked");

        By applicationLinkLocator = By.xpath("//a//span[contains(text(),'" + obpasId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.elementToBeClickable(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + obpasId);
    }

    // =====================================================================
    // STEP 9: INSPECTION REPORT PAGE
    // =====================================================================

    private void inspectionReport(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("On Inspection Report Page");
        Thread.sleep(500);

        // 1 DropDown

        selectDropdownByIndex(driver, wait, js, 0, 1);
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 1, 1);
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 2, 0);
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 3, 0);
        Thread.sleep(1000);

        // 2 Documents

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

        uploadFile(driver, wait, js, 1, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

        uploadFile(driver, wait, js, 2, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

        uploadFile(driver, wait, js, 3, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

        uploadFile(driver, wait, js, 4, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

// 3 DATE
        String currentDate = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));  // for type="date"

        WebElement dateInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='date']")
        ));

// scroll
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dateInput);
        Thread.sleep(200);

//  REACT HARD FIX (_valueTracker)
        js.executeScript(
                "var input = arguments[0];" +
                        "var lastValue = input.value;" +
                        "input.value = arguments[1];" +

                        "var tracker = input._valueTracker;" +
                        "if (tracker) { tracker.setValue(lastValue); }" +

                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('blur', { bubbles: true }));",
                dateInput,
                currentDate
        );

// VERIFY (important for debugging)
        String finalVal = dateInput.getAttribute("value");
        System.out.println("Final Date Value: " + finalVal);

// EXTRA SAFETY (agar reset ho jaye to re-fill)
        if (finalVal == null || finalVal.isEmpty()) {

            js.executeScript(
                    "var input = arguments[0];" +
                            "var lastValue = input.value;" +
                            "input.value = arguments[1];" +

                            "var tracker = input._valueTracker;" +
                            "if (tracker) { tracker.setValue(lastValue); }" +

                            "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                            "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                            "input.dispatchEvent(new Event('blur', { bubbles: true }));",
                    dateInput,
                    currentDate
            );
        }

        System.out.println("Final Date: " + dateInput.getAttribute("value"));


// 4 TIME
        new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement timeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@type='time']")
                )
        );

// React-safe set

        js.executeScript(
                "const input = arguments[0];" +
                        "const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                        "nativeInputValueSetter.call(input, '10:30');" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "input.blur();",
                timeInput
        );

        // 5. Take Action

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // FORWARD CLICK

        By forwardLocator = By.xpath("//p[normalize-space()='FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Forward clicked");
    }

    // =====================================================================
    // STEP 10: POP UP FORWARD AGAIN
    // =====================================================================

    private void handleForwardPopupAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Forward Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);


        // =========================
        // STEP 2: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Forwarded");

        System.out.println("Comment entered");
        Thread.sleep(3000);

        // =========================
        // STEP 3: CLICK FORWARD BUTTON
        // =========================
        By forwardBtnLocator = By.xpath("//button[normalize-space()='VERIFY AND FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Final Forward clicked");
        Thread.sleep(5000);
    }

    // =====================================================================
    // STEP 11: TAKE ACTION GO TO HOME AGAIN
    // =====================================================================

    private void takeActionGoToHomeAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }

    // =====================================================================
    // STEP 12: SEARCH APPLICATION AGAIN1
    // =====================================================================

    private void navigateToSearchApplicationAgain1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Property Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='NOC']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 13: SEARCH APPLICATION BY APPLICATION NUMBER AGAIN1
    // =====================================================================


    private void searchByApplicationAgain1(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String obpasId = applicationNumber.trim();
        System.out.println("Application Number " + obpasId);

        WebElement obpasInput = null;

        try {
            obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[2]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='BPA Application Number']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNo")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasInput);
        Thread.sleep(500);

        obpasInput.clear();
        obpasInput.sendKeys(obpasId);

        System.out.println("Application Number entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        System.out.println("Search button clicked");

        // CLICK RESULT

        Thread.sleep(2000);

// CLICK FIRST ROW (ALWAYS WORKING)
        WebElement applicationLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("(//table//tbody//tr//a)[1]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("First application clicked");
    }

    // =====================================================================
    // STEP 14: TAKE ACTION FORWARD1 FIRE NOC
    // =====================================================================

    private void takeActionAndForward1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Forward");


        //UPLOAD DOC

        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@type='file']")
                )
        );

        js.executeScript("arguments[0].style.display='block';", fileInput);
        fileInput.sendKeys(ConfigReader.get("document.iDentity.proof"));

        System.out.println("File uploaded");
        Thread.sleep(1000);


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVE CLICK

        By approveLocator = By.xpath("//p[normalize-space()='Approve']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        System.out.println("Forward clicked");
    }

    // =====================================================================
    // STEP 15: POP UP APPROVE1 FIRE NOC
    // =====================================================================

    private void handleForwardPopup1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Forward Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);


        // =========================
        // STEP 2: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Approved");

        System.out.println("Comment entered");

        // =========================
        // STEP 3: CLICK FORWARD BUTTON
        // =========================
        By approveBtnLocator = By.xpath("//button[normalize-space()='Approve']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        System.out.println("Final Forward clicked");
    }

    // =====================================================================
    // STEP 16: TAKE ACTION GO TO HOME AGAIN 2
    // =====================================================================

    private void takeActionGoToHomeAgain2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }

    // =====================================================================
    // STEP 17: SEARCH APPLICATION AGAIN2 OBPAS NOC
    // =====================================================================

    private void navigateToSearchApplicationAgain2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Property Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='OBPS']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='OBPS Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 18: SEARCH APPLICATION BY APPLICATION NUMBER
    // =====================================================================


    private void searchByApplicationAgain2(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String obpasId = applicationNumber.trim();
        System.out.println("Application Number " + obpasId);

        WebElement obpasInput = null;

        try {
            obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNo")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasInput);
        Thread.sleep(500);

        obpasInput.clear();
        obpasInput.sendKeys(obpasId);

        System.out.println("Application Number entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        System.out.println("Search button clicked");

        By applicationLinkLocator = By.xpath("//a//span[contains(text(),'" + obpasId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.elementToBeClickable(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + obpasId);
    }

    // =====================================================================
    // STEP 19: TAKE ACTION FORWARD1 OBPAS NOC
    // =====================================================================

    private void takeActionAndForward2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Forward");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // FORWARD CLICK

        By forwardLocator = By.xpath("//p[normalize-space()='FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Forward clicked");
    }

    // =====================================================================
    // STEP 20: POP UP FORWARD1 OBPAS NOC
    // =====================================================================

    private void handleForwardPopup2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Forward Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);


        // =========================
        // STEP 2: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Forwarded");

        System.out.println("Comment entered");

        // =========================
        // STEP 3: CLICK FORWARD BUTTON
        // =========================
        By forwardBtnLocator = By.xpath("//button[normalize-space()='VERIFY AND FORWARD']");

        WebElement forwardBtn = wait.until(ExpectedConditions.elementToBeClickable(forwardBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", forwardBtn);
        Thread.sleep(500);

        try {
            forwardBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", forwardBtn);
        }

        System.out.println("Final Forward clicked");
    }

    // =====================================================================
    // STEP 21: TAKE ACTION GO TO HOME AGAIN 2
    // =====================================================================

    private void takeActionGoToHomeAgain3(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }

    // =====================================================================
    // STEP 22: SEARCH APPLICATION AGAIN3
    // =====================================================================

    private void navigateToSearchApplicationAgain3(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Property Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='OBPS']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='OBPS Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 23: SEARCH APPLICATION BY APPLICATION NUMBER
    // =====================================================================


    private void searchByApplicationAgain3(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String obpasId = applicationNumber.trim();
        System.out.println("Application Number " + obpasId);

        WebElement obpasInput = null;

        try {
            obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                obpasInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNo")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasInput);
        Thread.sleep(500);

        obpasInput.clear();
        obpasInput.sendKeys(obpasId);

        System.out.println("Application Number entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        System.out.println("Search button clicked");

        By applicationLinkLocator = By.xpath("//a//span[contains(text(),'" + obpasId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.elementToBeClickable(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + obpasId);
    }

    // =====================================================================
    // STEP 24: TAKE ACTION APPROVER
    // =====================================================================

    private void takeActionAndApprover(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Approver");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVER CLICK

        By approverLocator = By.xpath("//p[normalize-space()='APPROVE']");

        WebElement approverBtn = wait.until(ExpectedConditions.elementToBeClickable(approverLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approverBtn);
        Thread.sleep(500);

        try {
            approverBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approverBtn);
        }

        System.out.println("Forward clicked");
    }

    // =====================================================================
    // STEP 25: POP UP APPROVER
    // =====================================================================

    private void handleApproverPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Approver Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);


        // =========================
        // STEP 2: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Approved");

        System.out.println("Comment entered");

        // =========================
        // STEP 3: CLICK FORWARD BUTTON
        // =========================
        By approveBtnLocator = By.xpath("//button[normalize-space()='APPROVE']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        System.out.println("Final Forward clicked");
    }

    // =====================================================================
    // STEP 26: TAKE ACTION GO TO HOME AGAIN 4
    // =====================================================================

    private void takeActionGoToHomeAgain4(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }


    // =====================================================================
    // UTILITY METHODS
    // =====================================================================

    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    /**
     * Utility method to click buttons with XPath
     */
    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].scrollIntoView(true);", button);
        Thread.sleep(300);
        button.click();
    }

    /**
     * Selects city dropdown during login
     */
    private void selectCityDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
        WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
        actions.moveToElement(cityDropdownArrow).click().perform();

        WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card")));
        WebElement firstCityOption = dropdownOptions.findElement(By.cssSelector(".profile-dropdown--item:first-child"));
        actions.moveToElement(firstCityOption).click().perform();
    }

    /**
     * Clicks the PAY button
     */
    private void clickPayButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='PAY']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        System.out.println("Clicked PAY button");
    }

    /**
     * Clicks the TAKE ACTION button
     */
    private void clickTakeActionButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='TAKE ACTION']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        System.out.println("Clicked TAKE ACTION button");
    }

    /**
     * Clicks the ASSESS PROPERTY button
     */

    private void clickAssessPropertyButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement assessPropertyButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Assess Property']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", assessPropertyButton);
        Thread.sleep(300);
        assessPropertyButton.click();
        System.out.println("Clicked ASSESS PROPERTY button");
    }

    /**
     * Clicks the GO BACK TO HOME button
     */
    private void clickGoBackToHomeButton (WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement goBackToHomeButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Go back to home page']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", goBackToHomeButton);
        Thread.sleep(300);
        goBackToHomeButton.click();
        System.out.println("Clicked GO Back To Home button");
    }


    /**
     * Handles the take action menu and selects appropriate action
     */
    private void handleTakeActionMenu(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        try {
            WebElement menuWrap = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.menu-wrap")));
            List<WebElement> actionOptions = menuWrap.findElements(By.tagName("p"));

            for (WebElement option : actionOptions) {
                String text = option.getText().trim().toUpperCase();
                if (text.equals("VERIFY")) {
                    option.click();
                    System.out.println("Clicked VERIFY");
                    handlePopupAndSubmit(driver, wait, "Automated verification comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("FORWARD")) {
                    option.click();
                    System.out.println("Clicked FORWARD");
                    handlePopupAndSubmit(driver, wait, "Automated forward comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("APPROVE")) {
                    option.click();
                    System.out.println("Clicked APPROVE");
                    handlePopupAndSubmit(driver, wait, "Automated approval comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("PAY")) {
                    option.click();
                    System.out.println("Clicked PAY");
                    break;
                } else if (text.equals("REJECT")) {
                    System.out.println("Application Rejected");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Take Action Menu not found or no valid option present: " + e.getMessage());
        }
    }

    /**
     * Handles popup submission with comment and document upload
     */
    private void handlePopupAndSubmit(WebDriver driver, WebDriverWait wait, String comment, String filePath) throws InterruptedException {
        // Enter comment
        WebElement commentField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("comments")));
        commentField.clear();
        commentField.sendKeys(comment);

        // Upload document
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-doc")));
        fileInput.sendKeys(filePath);
        System.out.println("Document uploaded");

        // Click Verify or Approve button
        List<WebElement> verifyButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Verify']]"));
        List<WebElement> approveButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Approve']]"));

        WebElement actionButton = null;
        if (!verifyButtons.isEmpty()) {
            actionButton = verifyButtons.get(0);
            System.out.println("Clicking Verify button");
        } else if (!approveButtons.isEmpty()) {
            actionButton = approveButtons.get(0);
            System.out.println("Clicking Approve button");
        } else {
            throw new RuntimeException("Neither Verify nor Approve button found!");
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", actionButton);
        Thread.sleep(300);
        actionButton.click();
    }

    private void selectRadioButtonByLabel(WebDriver driver, String labelText) {
        try {
            WebElement radio = null;

            try {
                radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/preceding-sibling::span/input"));
            } catch (Exception e1) {
                try {
                    radio = driver.findElement(By.xpath("//label[contains(text(),'" + labelText + "')]/preceding-sibling::input"));
                } catch (Exception e2) {
                    try {
                        radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/..//input[@type='radio']"));
                    } catch (Exception e3) {
                        try {
                            radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/following-sibling::input[@type='radio']"));
                        } catch (Exception e4) {
                            radio = driver.findElement(By.xpath("//input[@type='radio'][@value='" + labelText + "']"));
                        }
                    }
                }
            }

            if (radio != null && !radio.isSelected()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", radio);
                Thread.sleep(200);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", radio);
                System.out.println("Selected radio button: " + labelText);
            }
        } catch (Exception e) {
            System.out.println("Error selecting radio button '" + labelText + "': " + e.getMessage());
            throw new RuntimeException("Failed to select radio button: " + labelText, e);
        }
    }

    private void selectDropdownByIndex(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                       int dropdownIndex,
                                       int optionIndex)
            throws InterruptedException {

        List<WebElement> dropdowns = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("div.select svg.cp")
                )
        );

        WebElement dropdown = dropdowns.get(dropdownIndex);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);
        Thread.sleep(200);

        try {
            dropdown.click();
            Thread.sleep(1000);
        } catch (Exception e) {
            js.executeScript(
                    "var ev = document.createEvent('MouseEvents');" +
                            "ev.initEvent('click', true, true);" +
                            "arguments[0].dispatchEvent(ev);",
                    dropdown
            );
        }

        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.options-card")
                )
        );

        List<WebElement> options = optionsContainer.findElements(
                By.cssSelector("div.profile-dropdown--item")
        );

        WebElement option = options.get(optionIndex);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
        js.executeScript("arguments[0].click();", option);

        Thread.sleep(300);
    }

    private void enterCurrentTime(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        WebElement timeInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[@type='time']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", timeInput);

        String currentTime = java.time.LocalTime.now()
                .withSecond(0)
                .withNano(0)
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        timeInput.click();

        // clear properly
        timeInput.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        timeInput.sendKeys(Keys.DELETE);

        // type like real user
        timeInput.sendKeys(currentTime);

        System.out.println("Time entered: " + currentTime);
    }

    private void uploadFile(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                            int index, String filePath) throws InterruptedException {

        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));

        if (index >= fileInputs.size()) {
            System.out.println("ERROR: File input index " + index + " not found");
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        js.executeScript(
                "arguments[0].style.cssText = 'display:block !important; visibility:visible !important; opacity:1 !important;';",
                fileInput
        );
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        System.out.println("✓ Uploaded file at index " + index);

        js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fileInput);
        Thread.sleep(500);
    }

    private void fillTimeInput(WebElement timeElement, String time, JavascriptExecutor js) throws InterruptedException {
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", timeElement);
        timeElement.click();
        timeElement.clear();
        timeElement.sendKeys(time); // Use "10:30" not "10:30AM"
        js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", timeElement);
        Thread.sleep(500);
    }
}

