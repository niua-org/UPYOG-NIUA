package org.upyog.Automation.Modules.Adv;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.Utils.DriverFactory;
import org.upyog.Automation.config.WebDriverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdvEmp {

    private static final Logger logger = LoggerFactory.getLogger(AdvEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;


    //@PostConstruct
    public void advInbox() {
        advInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("adv.login.username"),
                ConfigReader.get("adv.login.password"),
                ConfigReader.get("adv.application.number"));
    }

    public void advInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Advertisement Application Employee Workflow");

        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to Advertisement Search Bookings
            navigateToSearchBookings(driver, wait, js);

            // STEP 3: Searching the Application By Booking Number
            searchByApplicationNumber(driver, wait, js,applicationNumber);

            // STEP 4: Click Take Action
            clickTakeAction(driver, wait, js);

            // STEP 5: Pop up Cancel Advertisement Booking
            handleCancelPopup(driver, wait, js);



            logger.info("Advertisement Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.info("Exception in Advertisement Application Employee Workflow: " + e.getMessage());
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

    // =====================================================================
    // STEP 2: NAVIGATES TO ADVERTISEMENT SEARCH BOOKINGS
    // =====================================================================

        private void navigateToSearchBookings(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
                throws InterruptedException {
            logger.info("Navigating to Search Bookings");

            // Wait for page to load after login
            Thread.sleep(2000);

            // Click Search Bookings link
            WebElement searchBookingLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[text()='Advertisement']" +
                            "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                            "//div[contains(@class,'employee-card-banner')]" +
                            "//*[normalize-space()='Search Bookings']")));
            js.executeScript("arguments[0].scrollIntoView(true);", searchBookingLink);
            searchBookingLink.click();
            logger.info("Clicked Search Bookings link");
        }

    // =====================================================================
    // STEP 3: NAVIGATES TO ADVERTISEMENT SEARCH APPLICATION
    // =====================================================================


    private void searchByApplicationNumber(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Application");

        Thread.sleep(1000);

        String bookingNumber;

        // =========================
        // STEP 1: USE INPUT OR FALLBACK
        // =========================
        if (applicationNumber != null && !applicationNumber.trim().isEmpty()
                && !applicationNumber.contains("@")) {

            bookingNumber = applicationNumber.trim();
            logger.info("Using Application Number from UI: " + bookingNumber);

        } else {

            logger.info("No valid input → fetching from table");

            WebElement firstBooking = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//table//tbody//tr[1]//td[1])")
            ));

            bookingNumber = firstBooking.getText().trim();

            logger.info("Captured Booking Number: " + bookingNumber);
        }

        // =========================
        // STEP 2: ENTER INTO INPUT
        // =========================
        By inputLocator = By.name("bookingNo");

        WebElement bookingInput = wait.until(ExpectedConditions.presenceOfElementLocated(inputLocator));

        // re-fetch (stale fix)
        bookingInput = driver.findElement(inputLocator);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", bookingInput);
        Thread.sleep(500);

        bookingInput.clear();
        bookingInput.sendKeys(bookingNumber);

        logger.info("Booking number entered");

        // =========================
        // STEP 3: CLICK SEARCH
        // =========================
        By searchLocator = By.xpath("//button[normalize-space()='Search']");

        WebElement searchBtn = wait.until(ExpectedConditions.presenceOfElementLocated(searchLocator));

        searchBtn = driver.findElement(searchLocator);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", searchBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", searchBtn);

        logger.info("Search button clicked");
    }

    // =====================================================================
    // STEP 4:TAKE ACTION
    // =====================================================================

    private void clickTakeAction(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking TAKE ACTION");

        // =========================
        // STEP 1: CLICK TAKE ACTION
        // =========================
        By takeActionLocator = By.xpath("//button[normalize-space()='TAKE ACTION']");

        WebElement takeActionBtn = wait.until(ExpectedConditions.elementToBeClickable(takeActionLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", takeActionBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", takeActionBtn);

        logger.info("TAKE ACTION clicked");

        // =========================
        // STEP 2: WAIT & CLICK CANCEL
        // =========================
        By cancelLocator = By.xpath("//*[contains(text(),'Cancel Advertisement Booking')]");

        WebElement cancelOption = wait.until(ExpectedConditions.visibilityOfElementLocated(cancelLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cancelOption);
        Thread.sleep(500);

        try {
            cancelOption.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", cancelOption);
        }

        logger.info("Cancel Advertisement Booking clicked");
    }

    // =====================================================================
    // STEP 5: HANDLE CANCEL POPUP
    // =====================================================================

    private void handleCancelPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Cancel Popup");

        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
        if (!checkboxes.isEmpty()) {
            WebElement lastCheckbox = checkboxes.get(checkboxes.size() - 1);
            try {
                if (!lastCheckbox.isSelected()) {
                    js.executeScript("arguments[0].scrollIntoView(true);", lastCheckbox);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].click();", lastCheckbox);
                    logger.info("Checked declaration checkbox");
                }
            } catch (Exception ex) {
                logger.info("Could not click declaration checkbox: " + ex.getMessage());
            }


            //Cancel Button
            By cancelBtnLocator = By.xpath("//button[normalize-space()='Cancel Advertisement Booking']");

            WebElement cancelBtn = wait.until(ExpectedConditions.elementToBeClickable(cancelBtnLocator));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", cancelBtn);
            Thread.sleep(500);

            try {
                cancelBtn.click();
            } catch (Exception e) {
                js.executeScript("arguments[0].click();", cancelBtn);
            }

            logger.info("Final Cancel clicked");

            // backend process ke liye wait
            Thread.sleep(2000);
            driver.navigate().refresh();

            logger.info("Page refreshed after cancel");
        }
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
}
