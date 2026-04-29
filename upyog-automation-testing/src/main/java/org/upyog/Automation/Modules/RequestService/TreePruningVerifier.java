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
public class TreePruningVerifier {

    private static final Logger logger = LoggerFactory.getLogger(TreePruningVerifier.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void treePruningInbox1() {
        treePruningInboxVerifier(ConfigReader.get("employee.base.url"),
                ConfigReader.get("app.login.username"),
                ConfigReader.get("app.login.password"),
                ConfigReader.get("treePruning.application.number"));
    }

    public void treePruningInboxVerifier(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Tree Pruning Application Verifier Workflow");

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

            // STEP 4: Take Action Verify
            takeActionAndVerify(driver, wait, js);

            // STEP 5: Pop Up Verify
            handleVerifyPopup(driver, wait, js);

            // STEP 6: Take Action Execute
            takeActionAndExecute(driver, wait, js);

            // STEP 7: Pop Up Execute
            handleExecutePopup(driver, wait, js);

            logger.info("Tree Pruning Application Verifier Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.info("Exception in Tree Pruning Application Verifier Workflow: " + e.getMessage());
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
        logger.info("Navigating to Search Tree Pruning Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Application link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='Tree Pruning']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Inbox']")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        logger.info("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 3: SEARCH APPLICATION BY BOOKING NO.
    // =====================================================================


    private void searchByBookingNo(WebDriver driver, WebDriverWait wait,
                                   JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Booking in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String tree1Id = applicationNumber.trim();
        logger.info("Using Booking No.: " + tree1Id);

        WebElement tree1Input = null;

        try {
            tree1Input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            logger.info("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                tree1Input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Booking No.']/parent::span//input")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                tree1Input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("bookingNo")
                ));
                logger.info("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", tree1Input);
        Thread.sleep(500);

        tree1Input.clear();
        tree1Input.sendKeys(tree1Id);

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

        By applicationLinkLocator = By.xpath("//a[contains(text(),'" + tree1Id + "')]");

        // CLICK RESULT

        WebElement applicationLink = wait.until(ExpectedConditions.visibilityOfElementLocated(applicationLinkLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);

        try {
            applicationLink.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", applicationLink);
        }

        logger.info("Application clicked: " + tree1Id);
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

        By verifyLocator = By.xpath("//p[normalize-space()='Verify']");

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
        commentBox.sendKeys("Verified");

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK VERIFY BUTTON
        // =========================
        By verifyBtnLocator = By.xpath("//button[normalize-space()='Verify']");

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
    // STEP 6: TAKE ACTION EXECUTE
    // =====================================================================

    private void takeActionAndExecute(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Execute");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // EXECUTE CLICK

        By executeLocator = By.xpath("//p[normalize-space()='Execute']");

        WebElement executeBtn = wait.until(ExpectedConditions.elementToBeClickable(executeLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", executeBtn);
        Thread.sleep(500);

        try {
            executeBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", executeBtn);
        }

        logger.info("Execute clicked");
    }

    // =====================================================================
    // STEP 7: POP UP EXECUTE
    // =====================================================================

    private void handleExecutePopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
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
        commentBox.sendKeys("Executed");

        logger.info("Comment entered");

        // =========================
        // STEP 3: CLICK EXECUTE BUTTON
        // =========================
        By executeBtnLocator = By.xpath("//button[normalize-space()='Execute']");

        WebElement executeBtn = wait.until(ExpectedConditions.elementToBeClickable(executeBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", executeBtn);
        Thread.sleep(500);

        try {
            executeBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();",executeBtn);
        }

        logger.info("Final Execute clicked");
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
}

