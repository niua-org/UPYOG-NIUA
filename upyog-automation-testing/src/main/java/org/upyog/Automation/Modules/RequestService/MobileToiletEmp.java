package org.upyog.Automation.Modules.RequestService;

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
public class MobileToiletEmp {

    private static final Logger logger = LoggerFactory.getLogger(MobileToiletEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void mobileToiletInbox() {
        mobileToiletInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("mt.login.username"),
                ConfigReader.get("mt.login.password"),
                ConfigReader.get("mobileToilet.application.number"));
    }

    public void mobileToiletInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Mobile Toilet Application Employee Workflow");

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
            searchByBookingNo(driver, wait, js, applicationNumber);

            // STEP 4: Take Action Approve
            takeActionAndApprove(driver, wait, js);

            // STEP 5: Pop Up Approve
            handleApprovePopup(driver, wait, js);

            // STEP 6: Take Action Pay
            takeActionAndPay(driver, wait, js);

            // STEP 7: Collect payment
            fillPaymentAndCollect(driver, wait, js);

            // STEP 8: Go to Home Page
            takeActionGoToHome(driver, wait, js);

            // STEP 9; Inbox Again
            navigateToSearchApplicationAgain(driver, wait, js);

            // STEP 10: Search Application by Booking Number Again
            searchByBookingNo1(driver, wait, js, applicationNumber);

            // STEP 11: Assign Vendor
            takeActionAssignVendor(driver, wait, js);

            // STEP 12: Assign Vendor PopUp
            handleAssignPopup(driver, wait, js);





            logger.info("Mobile Toilet Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.info("Exception in Mobile Toilet Application Employee Workflow: " + e.getMessage());
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

    private void navigateToSearchApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Search Mobile Toilet Application");

        Thread.sleep(2000);

        // Direct anchor click
        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'/wt/mt/my-bookings')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", link);  // JS click (important)

        logger.info("Clicked Search Application via href");
    }


    // =====================================================================
    // STEP 3: SEARCH APPLICATION BY BOOKING NO.
    // =====================================================================


    private void searchByBookingNo(WebDriver driver, WebDriverWait wait,
                                   JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Booking in Application Search");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//header[contains(text(),'Search Bookings')]")
        ));
        Thread.sleep(1000);

        String mobileId = applicationNumber.trim();
        logger.info("Using Booking No.: " + mobileId);

        WebElement mobileInput = null;

        try {
            mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.name("bookingNo")
            ));
            logger.info("Found using name locator");

        } catch (Exception e1) {

            try {
                mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Booking No.']/parent::span//input")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
                ));
                logger.info("Found using index fallback locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", mobileInput);
        Thread.sleep(500);

        mobileInput.clear();
        mobileInput.sendKeys(mobileId);

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + mobileId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        logger.info("Application clicked: " + mobileId);
    }

    // =====================================================================
    // STEP 4: TAKE ACTION APPROVE
    // =====================================================================

    private void takeActionAndApprove(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify");


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

        logger.info("Verify clicked");
    }

    // =====================================================================
    // STEP 5: POP UP APPROVE
    // =====================================================================

    private void handleApprovePopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
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
        By approveBtnLocator = By.xpath("//button[normalize-space()='Approve']");

        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(approveBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});",approveBtn);
        Thread.sleep(500);

        try {
            approveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", approveBtn);
        }

        logger.info("Final Approve clicked");
    }

    // =====================================================================
    // STEP 6: TAKE ACTION FORWARD
    // =====================================================================

    private void takeActionAndPay (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Pay");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);


        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // PAY CLICK

        By payLocator = By.xpath("//p[normalize-space()='PAY']");

        WebElement payBtn = wait.until(ExpectedConditions.elementToBeClickable(payLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", payBtn);
        Thread.sleep(500);

        try {
            payBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", payBtn);
        }

        logger.info("Forward clicked");
    }


    // =====================================================================
    // STEP 7: COLLECT PAYMENT
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
    // STEP 8: TAKE ACTION GO TO HOME
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
    // STEP 9: SEARCH APPLICATION 2
    // =====================================================================


    private void navigateToSearchApplicationAgain (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Navigating to Search Mobile Toilet Application");

        Thread.sleep(2000);

        // Direct anchor click
        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'/wt/mt/my-bookings')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", link);  //  JS click
        logger.info("Clicked Search Application via href");
    }

    // =====================================================================
    // STEP 10: SEARCH APPLICATION BY BOOKING NO. 2
    // =====================================================================


    private void searchByBookingNo1(WebDriver driver, WebDriverWait wait,
                                    JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Booking in Application Search");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//header[contains(text(),'Search Bookings')]")
        ));
        Thread.sleep(1000);

        String mobileId = applicationNumber.trim();
        logger.info("Using Booking No.: " + mobileId);

        WebElement mobileInput = null;

        try {
            mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.name("bookingNo")
            ));
            logger.info("Found using name locator");

        } catch (Exception e1) {

            try {
                mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Booking No.']/parent::span//input")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
                ));
                logger.info("Found using index fallback locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", mobileInput);
        Thread.sleep(500);

        mobileInput.clear();
        mobileInput.sendKeys(mobileId);

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + mobileId + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        logger.info("Application clicked: " + mobileId);
    }

    // =====================================================================
    // STEP 11: TAKE ACTION ASSIGN VENDOR
    // =====================================================================

    private void takeActionAssignVendor (WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // APPROVE CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vendor']");

        WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(assignLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", assignBtn);
        Thread.sleep(500);

        try {
            assignBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", assignBtn);
        }

        logger.info("Verify clicked");
    }

    // =====================================================================
    // STEP 12: POP UP ASSIGN VENDOR
    // =====================================================================

    private void handleAssignPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Approve Popup");

        // =========================
        // STEP 1: WAIT FOR POPUP
        // =========================
        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(2000);

        // =========================
        // STEP 2: DROPDOWN
        // =========================

        selectDropdownByIndex(driver, wait, js,0,0);
        Thread.sleep(1000);
        logger.info("sdgag");

        // =========================
        // STEP 4: ENTER COMMENT
        // =========================

        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        commentBox.clear();
        commentBox.sendKeys("Assigned");

        logger.info("Comment entered");

        // =========================
        // STEP 5: CLICK ASSIGN VENDOR BUTTON
        // =========================
        By assignBtnLocator = By.xpath("//button[normalize-space()='Assign Vendor']");

        WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(assignBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", assignBtn);
        Thread.sleep(500);

        try {
            assignBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", assignBtn);
        }

        logger.info("Final Approve clicked");
    }


    // =====================================================================
    // UTILITY METHODS
    // =====================================================================

    private void fillInput (WebDriverWait wait, String fieldName, String value){
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
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
     * Clicks the GO TO HOME button
     */
    private void clickGoBackToHomeButton (WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement goBackToHomeButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Go back to home page']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", goBackToHomeButton);
        Thread.sleep(300);
        goBackToHomeButton.click();
        logger.info("Clicked GO Back To Home button");
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
        }}}
