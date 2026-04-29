package org.upyog.Automation.Modules.EWaste;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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

import java.util.List;


@Component
public class EWasteEmp {

    private static final Logger logger = LoggerFactory.getLogger(EWasteEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void eWasteInbox() {
        eWasteInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("ewaste.login.username"),
                ConfigReader.get("ewaste.login.password"),
                ConfigReader.get("eWaste.application.number"));
    }

    public void eWasteInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Advertisement Application Employee Workflow");

        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to Search Application
            navigateToInbox(driver, wait, js);

            // STEP 3: Click Request ID
            searchByRequestID(driver, wait, js, applicationNumber);

            // STEP 4: Take Action Verify
            takeActionAndVerify(driver, wait, js);

            // STEP 5: Pop Up Verify
            handleVerifyPopup(driver, wait, js);

            // STEP 6: Take Action Forward
            takeActionPickup(driver, wait, js);

            // STEP 7: Pop Up Pickup
            handlePickupPopup(driver, wait, js);

            // STEP 8: Take Action Complete
            takeActionComplete(driver, wait, js);

            // STEP 9: Pop Up Complete
            handleCompleteReqPopup(driver, wait, js);



            logger.info("EWaste Management Tax Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.info("Exception in EWaste Management Application Employee Workflow: " + e.getMessage());
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


    private void navigateToInbox(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to E-Waste Inbox");

        Thread.sleep(2000);

        By inboxLocator = By.xpath(
                "//span[normalize-space()='E Waste']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//a"
        );

        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(inboxLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        Thread.sleep(300);

        try {
            inboxLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", inboxLink);
        }

        logger.info("Clicked E-Waste Inbox");
    }

    // =====================================================================
    // STEP 3: SEARCH APPLICATION BY REQUEST ID
    // =====================================================================


    private void searchByRequestID(WebDriver driver, WebDriverWait wait,
                                   JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Request in E-Waste Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String requestId = applicationNumber.trim();
        logger.info("Using Request ID: " + requestId);

        WebElement requestInput= null;

        try {
            requestInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            logger.info("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                requestInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Request ID']/parent::span//input")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                requestInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("requestId")
                ));
                logger.info("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", requestInput);
        Thread.sleep(500);

        requestInput.clear();
        requestInput.sendKeys(requestId);

        logger.info("Property ID entered");

        // SEARCH BUTTON
        WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']")
        ));

        js.executeScript("arguments[0].click();", searchBtn);

        logger.info("Search button clicked");

        // CLICK RESULT
        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + requestId + "')]");

        WebElement applicationLink = wait.until(
                ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator)
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(300);

        js.executeScript("arguments[0].click();", applicationLink);

        logger.info("Application clicked: " +requestId);
    }

    // =====================================================================
    // STEP 4: TAKE ACTION VERIFY
    // =====================================================================

    private void takeActionAndVerify(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // VERIFY CLICK

        By verifyLocator = By.xpath("//p[normalize-space()='Verify Product']");

        WebElement verifyBtn = wait.until(ExpectedConditions.elementToBeClickable(verifyLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", verifyBtn);
        Thread.sleep(500);

        try {
            verifyBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", verifyBtn);
        }

        logger.info("Verify clicked");
    }

    // =====================================================================
    // STEP 5: POP UP VERIFY
    // =====================================================================

    private void handleVerifyPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Verify Popup");

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
        commentBox.sendKeys("Product verified");

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK VERIFY BUTTON
        // =========================
        By verifyBtnLocator = By.xpath("//button[normalize-space()='Verify Product']");

        WebElement verifyBtn = wait.until(ExpectedConditions.elementToBeClickable(verifyBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", verifyBtn);
        Thread.sleep(500);

        try {
            verifyBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", verifyBtn);
        }

        logger.info("Final Verify clicked");
    }

    // =====================================================================
    // STEP 6: SEND PICKUP ALERT
    // =====================================================================

    private void takeActionPickup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Forward");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);

        // SEND PICKUP ALERT CLICK

        By pickupLocator = By.xpath("//p[normalize-space()='Send Pickup Alert']");

        WebElement pickupBtn = wait.until(ExpectedConditions.elementToBeClickable(pickupLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", pickupBtn);
        Thread.sleep(500);

        try {
            pickupBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", pickupBtn);
        }

        logger.info("Send Pickup Alert clicked");
    }

    // =====================================================================
    // STEP 7: POP UP PICKUP
    // =====================================================================

    private void handlePickupPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Forward Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);

        // =========================
        // STEP 2: ENTER DATE
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

        logger.info("Date entered: " + currentDate);

        // =========================
        // STEP 3: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Pickup Alert");

        logger.info("Comment entered");

        // =========================
        // STEP 4: CLICK SEND PICKUP ALERT BUTTON
        // =========================
        By pickupBtnLocator = By.xpath("//button[normalize-space()='Send Pickup Alert']");

        WebElement pickupBtn = wait.until(ExpectedConditions.elementToBeClickable(pickupBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", pickupBtn);
        Thread.sleep(500);

        try {
            pickupBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", pickupBtn);
        }

        logger.info("Final Forward clicked");
    }

    // =====================================================================
    // STEP 8: TAKE ACTION COMPLETE REQUEST
    // =====================================================================

    private void takeActionComplete(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Approve");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // COMPLETE REQUEST CLICK

        By completeReqLocator = By.xpath("//p[normalize-space()='Complete Request']");

        WebElement completeReqBtn = wait.until(ExpectedConditions.elementToBeClickable(completeReqLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", completeReqBtn);
        Thread.sleep(500);

        try {
            completeReqBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", completeReqBtn);
        }

        logger.info("Complete Request clicked");
    }

    // =====================================================================
    // STEP 9: POP UP COMPLETE REQUEST
    // =====================================================================

    private void handleCompleteReqPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Complete Request Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(1000);

        // =================================
        // STEP 2: ENTER TRANSACTION ID
        // =================================
        By transIdLocator = By.xpath("//input[@name='transactionId']");

        WebElement transactionIdBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(transIdLocator)
        );

        transactionIdBox.clear();
        transactionIdBox.sendKeys("9876ABCD54321");

        logger.info("Transaction ID entered");

        // =================================
        // STEP 3: ENTER FINAL AMOUNT
        // =================================
        By finalAmountLocator = By.xpath("//input[@name='finalAmount']");

        WebElement finalAmountBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(finalAmountLocator)
        );

        finalAmountBox.clear();
        finalAmountBox.sendKeys("20000");

        logger.info("Final Amount entered");

        // =========================
        // STEP 4: ENTER COMMENT
        // =========================
        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Completed");

        logger.info("Comment entered");

        // =========================
        // STEP 5: CLICK FORWARD BUTTON
        // =========================
        By completeBtnLocator = By.xpath("//button[normalize-space()='Complete request']");

        WebElement completeBtn = wait.until(ExpectedConditions.elementToBeClickable(completeBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", completeBtn);
        Thread.sleep(500);

        try {
            completeBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", completeBtn);
        }

        logger.info("Final Forward clicked");
        Thread.sleep(2000);
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
        logger.info("Clicked TAKE ACTION button");
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
        logger.info("Clicked ASSESS PROPERTY button");
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
    private void handlePopupAndSubmit(WebDriver driver, WebDriverWait wait, String comment, String filePath) throws InterruptedException {
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
                logger.info("Selected radio button: " + labelText);
            }
        } catch (Exception e) {
            logger.info("Error selecting radio button '" + labelText + "': " + e.getMessage());
            throw new RuntimeException("Failed to select radio button: " + labelText, e);
        }
    }
}
