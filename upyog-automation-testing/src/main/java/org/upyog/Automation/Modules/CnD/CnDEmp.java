package org.upyog.Automation.Modules.CnD;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.Utils.DriverFactory;
import org.upyog.Automation.config.WebDriverFactory;

import java.util.List;

@Component
public class CnDEmp {

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void CnDInbox() {
        CnDInboxEmp(ConfigReader.get("cnd.employee.base.url"),
                ConfigReader.get("cnd.login.username"),
                ConfigReader.get("cnd.login.password"),
                ConfigReader.get("cnd.application.number"));
    }

    public void CnDInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        System.out.println("CnD Application Employee Workflow");

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

            // STEP 3: Search CnD Application by Application Number
            searchByCndApplication(driver, wait, js, applicationNumber);

            // STEP 4: Take Action Verify
            takeActionAndAssignFI(driver, wait, js);

            // STEP 5: Pop Up Verify
            handleAssignFIPopup(driver, wait, js);

            // STEP 6: Take Action Approve
            takeActionAndApprove(driver, wait, js);

            // STEP 7: Approve Page
            handleApprovePage(driver, wait, js);

            // STEP 8: Go To Home Page
            clickHomePage(driver, wait, js);

            // STEP 9; Inbox Again
            navigateToSearchApplicationAgain(driver, wait, js);

            // STEP 10: Search Application by Booking Number Again
            searchByBookingNo1(driver, wait, js, applicationNumber);

            // STEP 11: Take Action Pay
            takeActionAndCollectFee(driver, wait, js);

            // STEP 12: Collect payment
            fillPaymentAndCollect(driver, wait, js);

            // STEP 13: Go to Home Page
            takeActionGoToHome(driver, wait, js);

            // STEP 14; Inbox Again 2
            navigateToSearchApplicationAgain1(driver, wait, js);

            // STEP 15: Search Application by Booking Number Again 1
            searchByBookingNo2(driver, wait, js, applicationNumber);

            // STEP 16: Assign Vendor
            takeActionAssignVendor(driver, wait, js);

            // STEP 17: Assign Vendor PopUp- Cancel
            handleAssignCancelPopup(driver, wait, js);

            // STEP 18: Assign Vendor 1
            takeActionAssignVendor1(driver, wait, js);

            // STEP 19: Assign Vendor PopUp
            handleAssignPopup(driver, wait, js);



            System.out.println("CnD Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            System.out.println("Exception in CnD Application Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        }finally {
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

    }
    // =====================================================================
    // STEP 2: SEARCH APPLICATION
    // =====================================================================


    private void navigateToSearchApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to Search CnD Application");

        // Wait for page load
        Thread.sleep(2000);

        // Direct stable locator using href
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/cnd/inbox')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        js.executeScript("arguments[0].click();", inboxLink);

        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 3: SEARCH CND APPLICATION BY APPLICATION NUMBER
    // =====================================================================


    private void searchByCndApplication(WebDriver driver, WebDriverWait wait,
                                    JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching CnD Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String cndId = applicationNumber.trim();
        System.out.println("Using CND ID: " + cndId);

        WebElement cndInput = null;

        try {
            cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number ']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNumber")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cndInput);
        Thread.sleep(500);

        cndInput.clear();
        cndInput.sendKeys(cndId);

        System.out.println("CnD Application Number entered");

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + cndId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + cndId);
    }

    // =====================================================================
    // STEP 4: TAKE ACTION VERIFY
    // =====================================================================

    private void takeActionAndAssignFI(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Assign Field Inspector");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // VERIFY CLICK

        By fieldInspectorLocator = By.xpath("//p[normalize-space()='Assign Field Inpector']");

        WebElement fieldInspectorBtn = wait.until(ExpectedConditions.elementToBeClickable(fieldInspectorLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fieldInspectorBtn);
        Thread.sleep(500);

        try {
            fieldInspectorBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", fieldInspectorBtn);
        }

        System.out.println("Assign Field Inspector clicked");
    }

    // =====================================================================
    // STEP 5: POP UP VERIFY
    // =====================================================================

    private void handleAssignFIPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Verify Popup");
        Thread.sleep(2000);

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(2000);

        // =========================
        // STEP 2: DROPDOWN
        // =========================

        Thread.sleep(1000);
        selectDropdownByIndex(driver, wait, js, 0,0 );
        Thread.sleep(1000);
        System.out.println("khalid cnd");

        // =========================
        // STEP 3: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Assigned");

        System.out.println("Comment entered");

        // =========================
        // STEP 4: CLICK VERIFY BUTTON
        // =========================
        By verifyBtnLocator = By.xpath("//button[normalize-space()='SUBMIT']");

        WebElement verifyBtn = wait.until(ExpectedConditions.elementToBeClickable(verifyBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", verifyBtn);
        Thread.sleep(500);

        try {
            verifyBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", verifyBtn);
        }

        System.out.println("Final Verify clicked");
    }

    // =====================================================================
    // STEP 6: TAKE ACTION APPROVE
    // =====================================================================

    private void takeActionAndApprove(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Approve");


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

        System.out.println("Approve clicked");
    }

    // =====================================================================
    // STEP 7: APPROVE PAGE
    // =====================================================================

    private void handleApprovePage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Waste Type Fields");

        // Get all rows
        List<WebElement> rows = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.xpath("//div[contains(@class,'waste-type-row')]")
        ));

        int count = 1;

        for (WebElement row : rows) {

            try {
                // Get label (Mixed Waste, Cement Bags, etc.)
                String label = row.findElement(By.xpath(".//div[1]")).getText();

                WebElement input = row.findElement(By.xpath(".//input"));

                js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);

                input.clear();
                input.sendKeys(String.valueOf(10 * count));

                // React trigger
                input.sendKeys(Keys.TAB);

                System.out.println("Filled: " + label);

                count++;

            } catch (Exception e) {
                System.out.println("Skipping one row");
            }
        }

        clickSubmitButton(driver, wait);
        Thread.sleep(1000);
        System.out.println("Approved and Submitted");
    }

    // =====================================================================
    // STEP 8: GO BACK TO HOME
    // =====================================================================

    private void clickHomePage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {

        System.out.println("Going back to Home Page");

        clickHomeButton(driver, wait);
        Thread.sleep(1000);
        System.out.println("Home Page Opened");
    }

    // =====================================================================
    // STEP 9: SEARCH APPLICATION 2
    // =====================================================================

    private void navigateToSearchApplicationAgain (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to Search CnD Application");

        // Wait for page load
        Thread.sleep(2000);

        // Direct stable locator using href
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/cnd/inbox')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        js.executeScript("arguments[0].click();", inboxLink);

        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 10: SEARCH APPLICATION BY BOOKING NO. 2
    // =====================================================================

    private void searchByBookingNo1(WebDriver driver, WebDriverWait wait,
                                    JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching CnD Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String cndId = applicationNumber.trim();
        System.out.println("Using CND ID: " + cndId);

        WebElement cndInput = null;

        try {
            cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number ']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNumber")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cndInput);
        Thread.sleep(500);

        cndInput.clear();
        cndInput.sendKeys(cndId);

        System.out.println("CnD Application Number entered");

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + cndId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + cndId);
    }

    // =====================================================================
    // STEP 11: TAKE ACTION PAY
    // =====================================================================

    private void takeActionAndCollectFee(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Pay");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // COLLECT FEE CLICK

        By payLocator = By.xpath("//p[normalize-space()='Collect Fees']");

        WebElement payBtn = wait.until(ExpectedConditions.elementToBeClickable(payLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", payBtn);
        Thread.sleep(500);

        try {
            payBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", payBtn);
        }

        System.out.println("Forward clicked");
    }


    // =====================================================================
    // STEP 12: COLLECT PAYMENT
    // =====================================================================

    private void fillPaymentAndCollect(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Payment Details");


        // PAYER NAME

        By OwnerLocator = By.name("payerName");

        WebElement ownerInput = wait.until(ExpectedConditions.visibilityOfElementLocated(OwnerLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", ownerInput);
        Thread.sleep(300);

        ownerInput.clear();
        ownerInput.sendKeys("Arpit Rao");

        System.out.println("Payer Mobile entered");

        // PAYER MOBILE

        By mobileLocator = By.name("payerMobile");

        WebElement mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(mobileLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", mobileInput);
        Thread.sleep(300);

        mobileInput.clear();
        mobileInput.sendKeys("9999999999");

        System.out.println("Payer Mobile entered");



        // RECEIPT NUMBER

        By receiptNoLocator = By.name("instrumentNumber");

        WebElement receiptInput = wait.until(ExpectedConditions.visibilityOfElementLocated(receiptNoLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", receiptInput);
        Thread.sleep(300);

        receiptInput.clear();
        receiptInput.sendKeys("GEN123456");

        System.out.println("Receipt Number entered");



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

        System.out.println("Date entered: " + currentDate);



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

        System.out.println("Collect Payment clicked");
    }

    // =====================================================================
    // STEP 13: TAKE ACTION GO TO HOME
    // =====================================================================

    private void takeActionGoToHome (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Go To Home Page");


        // TAKE ACTION

        clickGoBackToHomeButton(driver, wait);
        Thread.sleep(500);

        System.out.println("Go To Home clicked");

    }

    // =====================================================================
    // STEP 14: SEARCH APPLICATION 3
    // =====================================================================

    private void navigateToSearchApplicationAgain1 (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to Search CnD Application");

        // Wait for page load
        Thread.sleep(2000);

        // Direct stable locator using href
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/cnd/inbox')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        js.executeScript("arguments[0].click();", inboxLink);

        System.out.println("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 15: SEARCH APPLICATION BY BOOKING NO. 3
    // =====================================================================

    private void searchByBookingNo2(WebDriver driver, WebDriverWait wait,
                                    JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching CnD Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String cndId = applicationNumber.trim();
        System.out.println("Using CND ID: " + cndId);

        WebElement cndInput = null;

        try {
            cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            System.out.println("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number ']/parent::span//input")
                ));
                System.out.println("Found using label-based locator");

            } catch (Exception e2) {

                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNumber")
                ));
                System.out.println("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cndInput);
        Thread.sleep(500);

        cndInput.clear();
        cndInput.sendKeys(cndId);

        System.out.println("CnD Application Number entered");

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + cndId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        System.out.println("Application clicked: " + cndId);
    }

    // =====================================================================
    // STEP 16: TAKE ACTION ASSIGN VENDOR
    // =====================================================================

    private void takeActionAssignVendor (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Verify");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ASSIGN VENDOR CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vendor']");

        WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(assignLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", assignBtn);
        Thread.sleep(500);

        try {
            assignBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", assignBtn);
        }

        System.out.println("Assign Vendor clicked");
    }

    // =====================================================================
    // STEP 17: POP UP ASSIGN VENDOR - CANCEL
    // =====================================================================

    private void handleAssignCancelPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Cancel First");
        Thread.sleep(2000);

        WebElement cancelBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'popup-module')]//button[contains(@class,'selector-button-border')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cancelBtn);
        Thread.sleep(2000);

        try {
            cancelBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", cancelBtn);
        }

        System.out.println("Cancel clicked");
        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 18: TAKE ACTION ASSIGN VENDOR 1
    // =====================================================================

    private void takeActionAssignVendor1 (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Take Action → Verify");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ASSIGN VENDOR CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vendor']");

        WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(assignLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", assignBtn);
        Thread.sleep(500);

        try {
            assignBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", assignBtn);
        }

        System.out.println("Assign Vendor clicked");
    }


    // =====================================================================
    // STEP 19: POP UP ASSIGN VENDOR
    // =====================================================================

    private void handleAssignPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Assign Popup");
        Thread.sleep(2000);

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(5000);

        // =========================
        // STEP 2: DROPDOWN
        // =========================

        Thread.sleep(2000);
        selectDropdownByIndex(driver, wait, js, 0,0 );
        Thread.sleep(1000);
        System.out.println("Khalid cnd");


        // =========================
        // STEP 3:RECEIPT DATE
        // =========================

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

        System.out.println("Date entered: " + currentDate);

        // =========================
        // STEP 4: ENTER COMMENT
        // =========================

        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Assigned");

        System.out.println("Comment entered");

        // =========================
        // STEP 5: CLICK ASSIGN VENDOR BUTTON
        // =========================
        By submitBtnLocator = By.xpath("//button[normalize-space()='SUBMIT']");

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(submitBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(500);

        try {
            submitBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", submitBtn);
        }

        System.out.println("Final Approve clicked");
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
     * Clicks the SUBMIT button
     */
    private void clickSubmitButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Submit']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        System.out.println("Clicked TAKE ACTION button");

    }

    /**
     * Clicks the GO BACK TO HOME button
     */
    private void clickHomeButton (WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement goBackToHomeButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Home']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", goBackToHomeButton);
        Thread.sleep(300);
        goBackToHomeButton.click();
        System.out.println("Clicked GO Back To Home button");
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
        Thread.sleep(1000);

        try {
            dropdown.click();
            Thread.sleep(2000);
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
}

