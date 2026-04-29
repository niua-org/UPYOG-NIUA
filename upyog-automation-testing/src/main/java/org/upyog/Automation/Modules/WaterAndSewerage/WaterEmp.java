package org.upyog.Automation.Modules.WaterAndSewerage;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.Utils.DriverFactory;
import org.upyog.Automation.config.WebDriverFactory;

import java.util.ArrayList;
import java.util.List;

@Component

public class WaterEmp {

    private static final Logger logger = LoggerFactory.getLogger(WaterEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void waterInbox() {
        waterInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("wns.login.username"),
                ConfigReader.get("wns.login.password"),
                ConfigReader.get("water.application.number"));
    }

    public void waterInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Water Connection Application Employee Workflow");

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

            // STEP 3: Search Application by Booking Number
            searchByApplicationNo(driver, wait, js, applicationNumber);

            // STEP 4: Take Action Verify and Forward
            takeActionAndVerifyAndForward(driver, wait, js);

            // STEP 5: Pop Up Verify and Forward
            handleVerifyAndForwardPopup(driver, wait, js);

            // STEP 5: Take Action Verify and Forward
            takeActionAndVerifyAndForward(driver, wait, js);

            // STEP 6: Take Action Edit
            takeActionAndEdit(driver, wait, js);

            // STEP 7: handleEditPageEmployee
            handleEditPageEmployee(driver, wait, js);

            // STEP 8: Take Action Verify and Forward 2
            takeActionAndVerifyAndForward1(driver, wait, js);

            // STEP 9: Pop up Verify and Forward 2
            handleVerifyAndForward1Popup(driver, wait, js);

            // STEP 10: Take Action Approve Connection
            takeActionAndApproveConnection(driver, wait, js);

            // STEP 11: Pop up Approve Connection
            handleApproveConnectionPopup(driver, wait, js);

            // STEP 12: Take Action Collect
            takeActionCollect(driver, wait, js);

            // STEP 13: Collect Payment
            fillPaymentAndCollect(driver, wait, js);

            // STEP 14: Go to Home Page
            takeActionGoToHome(driver, wait, js);

            // STEP 15: Navigate to Search Application
            navigateToSearchApplicationAgain(driver, wait, js);

            // STEP 16: Search Application by Booking Number
            searchByApplicationNoAgain(driver, wait, js, applicationNumber);

            // STEP 17: Take Action Activate Connection
            takeActionAndActivateConnection(driver, wait, js);

            // STEP 18: Activate Connection Page
            handleActivateConnectionEmployee(driver, wait, js);

            logger.info("Water Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.info("Exception in Water Application Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        }finally {
            if (driver != null) {
                driver.quit();
            }}
    }

    // =====================================================================
    // STEP 1: EMPLOYEE LOGIN
    // =====================================================================


    private void performEmployeeLogin (WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions
            actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        logger.info("Open the Employee Login Portal");

        // Enter credentials from configuration
        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        logger.info("Filled username and password");

        // Select city dropdown
        selectCityDropdown(driver, wait, actions);

        // Click Continue button
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");
    }

    // =====================================================================
    // STEP 2: SEARCH APPLICATION
    // =====================================================================

    private void navigateToSearchApplication (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Navigating to Search Water Tanker Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='Water']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        logger.info("Clicked Inbox link");
    }


    // =====================================================================
    // STEP 3: SEARCH APPLICATION BY APPLICATION NO.
    // =====================================================================


    private void searchByApplicationNo(WebDriver driver, WebDriverWait wait,
                                       JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Application No. in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String waterId = applicationNumber.trim();
        logger.info("Using Application No.: " + waterId);

        WebElement waterInput = null;

        try {
            waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.name("applicationNumber")
            ));
            logger.info("Found using name locator");

        } catch (Exception e1) {

            try {
                waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[normalize-space()='Application No.']/following::input[1]")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
                ));
                logger.info("Found using index fallback locator");
            }
        }
        Thread.sleep(2000);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", waterInput);
        Thread.sleep(500);

        waterInput.clear();
        waterInput.sendKeys(waterId);

        logger.info("Booking No entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        logger.info("Search button clicked");

        // WAIT FOR TABLE

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//table//tbody//tr")
        ));
        Thread.sleep(1500);

        // CLICK RESULT

        By applicationLinkLocator = By.xpath("//table//tbody//tr//a");

        WebElement applicationLink = wait.until(
                ExpectedConditions.elementToBeClickable(applicationLinkLocator)
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        logger.info("Application clicked");
    }

    // =====================================================================
    // STEP 4: TAKE ACTION VERIFY AND FORWARD
    // =====================================================================

    private void takeActionAndVerifyAndForward(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify and Forward");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVE CLICK

        By vAndfLocator = By.xpath("//p[normalize-space()='Verify and Forward']");

        WebElement vAndfBtn = wait.until(ExpectedConditions.elementToBeClickable(vAndfLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", vAndfBtn);
        Thread.sleep(500);

        try {
            vAndfBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", vAndfBtn);
        }

        logger.info("Verify and forward clicked");
    }

    // =====================================================================
    // STEP 5: POP UP VERIFY AND FORWARD
    // =====================================================================

    private void handleVerifyAndForwardPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Approve Popup");

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
        commentBox.sendKeys("Verified and Okay to forward");

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK VERIFY BUTTON
        // =========================

        By vAndfBtnLocator = By.xpath("//button[normalize-space()='Verify and Forward']");

        WebElement vAndfBtn = wait.until(ExpectedConditions.elementToBeClickable(vAndfBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", vAndfBtn);
        Thread.sleep(500);

        try {
            vAndfBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", vAndfBtn);
        }

        logger.info("Final Verify and Forward clicked");
    }

    // =====================================================================
    // STEP 6: TAKE ACTION EDIT
    // =====================================================================

    private void takeActionAndEdit(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Edit");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // EDIT CLICK

        By editLocator = By.xpath("//p[normalize-space()='Edit']");

        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(editLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", editBtn);
        Thread.sleep(500);

        try {
            editBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", editBtn);
        }

        logger.info("Verify and forward clicked");
    }

    // =====================================================================
    // STEP 7: EDIT PAGE
    // =====================================================================

    private void handleEditPageEmployee(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Edit Page");

        // SCROLL START (IMPORTANT)
        js.executeScript("window.scrollTo(0,0)");
        Thread.sleep(1000);

        fillNumberInputH2(driver, wait, js,"Number of Taps", "8");
        selectDropdownByIndex(driver, wait, js, 0, 0);


        // =========================
        // Connection Details
        // =========================

        selectDropdownByIndex(driver, wait, js, 1, 0);

        selectDropdownByIndex(driver, wait, js, 2, 0);

        selectDropdownByIndex(driver, wait, js, 3, 1);

        selectDropdownByIndex(driver, wait, js, 4, 0);

        fillInputSecondH2(driver, wait, js,"Number Of taps", "8");

        // =========================
        // Plumber Details
        // =========================

        selectDropdownByIndex(driver, wait, js, 5, 0);


        fillInputH2(driver, wait, js,"Plumber License Number", "7898765");

        fillInputH2(driver, wait, js,"Plumber Name", "Amit Kumar");

        fillInputH2(driver, wait, js,"Plumber Mobile Number", "9876543210");
        Thread.sleep(500);

        // =========================
        // Road Cutting
        // =========================

        scrollToLabel(driver, js, "Road Cutting Details");


        selectDropdownByIndex(driver, wait, js, 6, 0);


        fillInputSecondH2(driver,wait, js, "Area of Plot *", "1200");

        // =========================
        // Evidence
        // =========================

        scrollToLabel(driver, js, "Evidences");
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 7, 0);
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.identity1.proof"));


        selectDropdownByIndex(driver, wait, js, 8, 0);
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.identity1.proof"));


        selectDropdownByIndex(driver, wait, js, 9, 0);
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.identity1.proof"));


        selectDropdownByIndex(driver, wait, js, 10, 0);
        uploadFile(driver, wait, js, 3, ConfigReader.get("document.identity1.proof"));
        Thread.sleep(500);

        // =========================
        // Submit
        // =========================

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//header[text()='SUBMIT']]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", submitBtn);

        logger.info("Edit Page Completed");
    }

    // =====================================================================
    // STEP 8: TAKE ACTION VERIFY AND FORWARD
    // =====================================================================

    private void takeActionAndVerifyAndForward1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify and Forward");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVE CLICK

        By vAndfLocator = By.xpath("//p[normalize-space()='Verify and Forward']");

        WebElement vAndfBtn = wait.until(ExpectedConditions.elementToBeClickable(vAndfLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", vAndfBtn);
        Thread.sleep(500);

        try {
            vAndfBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", vAndfBtn);
        }

        logger.info("Verify and forward clicked");
    }

    // =====================================================================
    // STEP 9: POP UP VERIFY AND FORWARD
    // =====================================================================

    private void handleVerifyAndForward1Popup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Approve Popup");

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
        commentBox.sendKeys("Verified and Okay to forward");

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK VERIFY BUTTON
        // =========================

        By vAndfBtnLocator = By.xpath("//button[normalize-space()='Verify and Forward']");

        WebElement vAndfBtn = wait.until(ExpectedConditions.elementToBeClickable(vAndfBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", vAndfBtn);
        Thread.sleep(500);

        try {
            vAndfBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", vAndfBtn);
        }

        logger.info("Final Verify and Forward clicked");
    }

    // =====================================================================
    // STEP 10: TAKE ACTION APPROVE CONNECTION
    // =====================================================================

    private void takeActionAndApproveConnection(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Approve Connection");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVE CLICK

        By approveLocator = By.xpath("//p[normalize-space()='Approve Connection']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        logger.info("Verify and forward clicked");
    }

    // =====================================================================
    // STEP 11: POP UP APPROVE CONNECTION
    // =====================================================================

    private void handleApproveConnectionPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Approve Popup");

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

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK VERIFY BUTTON
        // =========================

        By approveBtnLocator = By.xpath("//button[normalize-space()='Approve Connection']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        logger.info("Final Approved clicked");
    }

    // =====================================================================
    // STEP 12: TAKE ACTION COLLECT
    // =====================================================================

    private void takeActionCollect(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Approve Connection");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // COLLECT CLICK

        By collectLocator = By.xpath("//p[normalize-space()='Collect']");

        WebElement collectBtn = wait.until(ExpectedConditions.elementToBeClickable(collectLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", collectBtn);
        Thread.sleep(500);

        try {
            collectBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", collectBtn);
        }

        logger.info("Verify and forward clicked");
    }

    // =====================================================================
    // STEP 13: COLLECT PAYMENT
    // =====================================================================

    private void fillPaymentAndCollect(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Payment Details");


        // PAYER MOBILE

        By mobileLocator = By.name("payerMobile");

        WebElement mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(mobileLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", mobileInput);
        Thread.sleep(300);

        mobileInput.clear();
        mobileInput.sendKeys("9999999999");

        logger.info("Payer Mobile entered");



        // RECEIPT NUMBER

        By receiptNoLocator = By.name("instrumentNumber");

        WebElement receiptInput = wait.until(ExpectedConditions.visibilityOfElementLocated(receiptNoLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", receiptInput);
        Thread.sleep(300);

        receiptInput.clear();
        receiptInput.sendKeys("GEN123456");

        logger.info("Receipt Number entered");



        // RECEIPT DATE

        String currentDate = java.time.LocalDate.now().toString();

        By dateLocator = By.xpath("//input[@type='date']");

        WebElement dateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(dateLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dateInput);
        Thread.sleep(300);

        // Set current date
        js.executeScript(
                "arguments[0].value='" + currentDate + "';" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateInput
        );

        logger.info("Date entered: " + currentDate);

        // CLICK COLLECT PAYMENT

        By collectBtnLocator = By.xpath("//button[.//header[normalize-space()='Collect Payment']]");

        WebElement collectBtn = wait.until(ExpectedConditions.elementToBeClickable(collectBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", collectBtn);
        Thread.sleep(500);

        try {
            collectBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", collectBtn);
        }

        logger.info("Collect Payment clicked");
    }

    // =====================================================================
    // STEP 14: TAKE ACTION GO TO HOME
    // =====================================================================

    private void takeActionGoToHome (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        logger.info("Go To Home clicked");

    }

    // =====================================================================
    // STEP 15: SEARCH APPLICATION AGAIN
    // =====================================================================

    private void navigateToSearchApplicationAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Navigating to Search Water Tanker Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='Water']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        logger.info("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 16: SEARCH APPLICATION BY APPLICATION NO. AGAIN
    // =====================================================================

    private void searchByApplicationNoAgain(WebDriver driver, WebDriverWait wait,
                                       JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Application No. in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String waterId = applicationNumber.trim();
        logger.info("Using Application No.: " + waterId);

        WebElement waterInput = null;

        try {
            waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.name("applicationNumber")
            ));
            logger.info("Found using name locator");

        } catch (Exception e1) {

            try {
                waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[normalize-space()='Application No.']/following::input[1]")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                waterInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
                ));
                logger.info("Found using index fallback locator");
            }
        }
        Thread.sleep(2000);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", waterInput);
        Thread.sleep(500);

        waterInput.clear();
        waterInput.sendKeys(waterId);

        logger.info("Booking No entered");

        //SEARCH BUTTON

        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        try {
            searchBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", searchBtn);
        }

        logger.info("Search button clicked");

        // WAIT FOR TABLE

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//table//tbody//tr")
        ));
        Thread.sleep(1500);

        // CLICK RESULT

        By applicationLinkLocator = By.xpath("//table//tbody//tr//a");

        WebElement applicationLink = wait.until(
                ExpectedConditions.elementToBeClickable(applicationLinkLocator)
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        logger.info("Application clicked");
    }

    // =====================================================================
    // STEP 17: TAKE ACTION ACTIVATE CONNECTION
    // =====================================================================

    private void takeActionAndActivateConnection(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Activate Connection");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ACTIVATE CONNECTION CLICK

        By activateConnectionLocator = By.xpath("//p[normalize-space()='Activate Connection']");

        WebElement activateConnectionBtn = wait.until(ExpectedConditions.elementToBeClickable(activateConnectionLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", activateConnectionBtn);
        Thread.sleep(500);

        try {
            activateConnectionBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", activateConnectionBtn);
        }

        logger.info("Activate Connection clicked");
    }

    // =====================================================================
    // STEP 18: ACTIVATE CONNECTION PAGE
    // =====================================================================

    private void handleActivateConnectionEmployee(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Activate Connection Page");

        // SCROLL START (IMPORTANT)
        js.executeScript("window.scrollTo(0,0)");
        Thread.sleep(1000);

        // =========================
        // Plumber Details
        // =========================

        selectDropdownByIndex(driver, wait, js, 4, 0);


        fillInputSecondH2(driver, wait, js,"Plumber License Number", "7898765");

        fillInputSecondH2(driver, wait, js,"Plumber Name", "Amit Kumar");

        fillInputSecondH2(driver, wait, js,"Plumber Mobile Number", "9876543210");
        Thread.sleep(500);

        // =========================
        // Activation Details
        // =========================

        // Meter ID
        fillNumberInputH2(driver, wait, js,"Meter ID", "87655446");

        // Initials Reading
        fillNumberInputH2(driver, wait, js,"Initial Meter Reading", "78987655446");

        // Dates- Installation Date and Connection Date
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

// ================= INSTALLATION DATE =================
        WebElement installationInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'Meter Installation Date')]/following::input[1]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", installationInput);

        installationInput.click();
        installationInput.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        installationInput.sendKeys(Keys.DELETE);
        installationInput.sendKeys(date);

// ================= CONNECTION DATE =================
        WebElement connectionInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'Connection Date')]/following::input[1]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", connectionInput);

        connectionInput.click();
        connectionInput.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        connectionInput.sendKeys(Keys.DELETE);
        connectionInput.sendKeys(date);

        logger.info("Dates entered: " + date);


        // =========================
        // Submit
        // =========================

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//header[text()='Activate Connection']]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", submitBtn);

        logger.info("Activate Collection Page Completed");
    }


    // =====================================================================
    // UTILITY METHODS
    // =====================================================================

    private void fillInput (WebDriverWait wait, String fieldName, String value){
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    private void fillInputH2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                             String labelText, String value)
            throws InterruptedException{

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'" + labelText + "')]/following::input[1]")
        ));

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});" +
                        "window.scrollBy(0, -150);",
                input
        );

        Thread.sleep(300);

        // FORCE CLEAR + SET VALUE + TRIGGER EVENT (IMPORTANT)
        js.executeScript(
                "arguments[0].value='';" +
                        "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                        "arguments[0].value='" + value + "';" +
                        "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                input
        );
    }

    private void fillNumberInputH2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                   String labelText, String value)
            throws InterruptedException{

        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[contains(text(),'" + labelText + "')]/following::input[1]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        Thread.sleep(300);

        // CLICK FIRST (VERY IMPORTANT)
        input.click();
        Thread.sleep(200);

        // SELECT ALL + DELETE (REAL CLEAR)
        input.sendKeys(Keys.chord(Keys.COMMAND, "a")); // Mac
        input.sendKeys(Keys.DELETE);
        Thread.sleep(200);

        // TYPE NEW VALUE
        input.sendKeys(value);
        Thread.sleep(200);

        // TRIGGER BLUR (IMPORTANT FOR REACT)
        input.sendKeys(Keys.TAB);
    }

    private void fillAutoFilledInput(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                     String label, String value) throws InterruptedException {

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'" + label + "')]/following::input[1]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        Thread.sleep(300);

        // STEP 1: Click (focus)
        input.click();
        Thread.sleep(200);


        // STEP 3: Type new value
        input.sendKeys(value);
        Thread.sleep(200);
    }

    /**
     * Utility method to click buttons with XPath
     */
    private void clickButton (WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].scrollIntoView(true);", button);
        Thread.sleep(300);
        button.click();
    }

    /**
     * Selects city dropdown during login
     */
    private void selectCityDropdown (WebDriver driver, WebDriverWait wait, Actions actions) throws
            InterruptedException {
        WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
        WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
        actions.moveToElement(cityDropdownArrow).click().perform();

        WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card")));
        WebElement firstCityOption = dropdownOptions.findElement(By.cssSelector(".profile-dropdown--item:first-child"));
        actions.moveToElement(firstCityOption).click().perform();
    }

    /**
     * Clicks the TAKE ACTION button
     */
    private void clickTakeActionButton (WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='TAKE ACTION']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        logger.info("Clicked TAKE ACTION button");
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
        logger.info("Clicked GO Back To Home button");
    }

    private void fillInputSecondH2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                   String labelText, String value) {

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h2[contains(text(),'" + labelText + "')]/following::input[1]")
        ));

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'}); window.scrollBy(0,-150);",
                input
        );

        input.clear();
        input.sendKeys(value);
    }


    /**
     * Handles the take action menu and selects appropriate action
     */
    private void handleTakeActionMenu (WebDriver driver, WebDriverWait wait) throws InterruptedException {
        try {
            WebElement menuWrap = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.menu-wrap")));
            List<WebElement> actionOptions = menuWrap.findElements(By.tagName("p"));

            for (WebElement option : actionOptions) {
                String text = option.getText().trim().toUpperCase();
                if (text.equals("VERIFY")) {
                    option.click();
                    logger.info("Clicked VERIFY");
                    handlePopupAndSubmit(driver, wait, "Automated verification comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("FORWARD")) {
                    option.click();
                    logger.info("Clicked FORWARD");
                    handlePopupAndSubmit(driver, wait, "Automated forward comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("APPROVE")) {
                    option.click();
                    logger.info("Clicked APPROVE");
                    handlePopupAndSubmit(driver, wait, "Automated approval comment.",
                            ConfigReader.get("document.identity.proof"));
                    break;
                } else if (text.equals("PAY")) {
                    option.click();
                    logger.info("Clicked PAY");
                    break;
                } else if (text.equals("REJECT")) {
                    logger.info("Application Rejected");
                    break;
                }
            }
        } catch (Exception e) {
            logger.info("Take Action Menu not found or no valid option present: " + e.getMessage());
        }
    }

    /**
     * Handles popup submission with comment and document upload
     */
    private void handlePopupAndSubmit (WebDriver driver, WebDriverWait wait, String comment, String filePath) throws
            InterruptedException {
        // Enter comment
        WebElement commentField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("comments")));
        commentField.clear();
        commentField.sendKeys(comment);

        // Upload document
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-doc")));
        fileInput.sendKeys(filePath);
        logger.info("Document uploaded");

        // Click Verify or Approve button
        List<WebElement> verifyButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Verify']]"));
        List<WebElement> approveButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Approve']]"));

        WebElement actionButton = null;
        if (!verifyButtons.isEmpty()) {
            actionButton = verifyButtons.get(0);
            logger.info("Clicking Verify button");
        } else if (!approveButtons.isEmpty()) {
            actionButton = approveButtons.get(0);
            logger.info("Clicking Approve button");
        } else {
            throw new RuntimeException("Neither Verify nor Approve button found!");
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", actionButton);
        Thread.sleep(300);
        actionButton.click();
    }
    private void selectDropdownByIndex(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                       int dropdownIndex,
                                       int optionIndex) throws InterruptedException {

        List<WebElement> dropdowns = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("div.select")
                )
        );

        WebElement dropdown = dropdowns.get(dropdownIndex);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);
        Thread.sleep(300);

        dropdown.click(); // normal click (no JS needed)

        Thread.sleep(1000);

        List<WebElement> allOptions = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("div.profile-dropdown--item")
                )
        );

        List<WebElement> visibleOptions = new ArrayList<>();

        for (WebElement opt : allOptions) {
            if (opt.isDisplayed()) {
                visibleOptions.add(opt);
            }
        }

        WebElement option = visibleOptions.get(optionIndex);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
        Thread.sleep(200);

        option.click(); // normal click

        Thread.sleep(500);
    }

    private void selectRadioButtonByLabel (WebDriver driver, String labelText){
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
                logger.info("Selected radio button: " + labelText);
            }
        } catch (Exception e) {
            logger.info("Error selecting radio button '" + labelText + "': " + e.getMessage());
            throw new RuntimeException("Failed to select radio button: " + labelText, e);
        }}

    private void uploadFile(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                            int index, String filePath) throws InterruptedException {

        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));

        if (index >= fileInputs.size()) {
            logger.info("ERROR: File input index " + index + " not found");
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        js.executeScript(
                "arguments[0].style.cssText = 'display:block !important; visibility:visible !important; opacity:1 !important;';",
                fileInput
        );
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        logger.info("✓ Uploaded file at index " + index);

        js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fileInput);
        Thread.sleep(500);
    }

    private void scrollToLabel(WebDriver driver, JavascriptExecutor js, String labelText) {
        try {
            WebElement section = driver.findElement(
                    By.xpath("//*[contains(text(),'" + labelText + "')]")
            );
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", section);
            Thread.sleep(800);
        } catch (Exception e) {
            logger.info("Scroll failed for: " + labelText);
        }
    }

}
