package org.upyog.Automation.Modules.CHB;

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

import java.util.List;

@Component
public class chbEmp {

    @Autowired
    private WebDriverFactory webDriverFactory;




    //@PostConstruct
    public void ChbInbox() {
        chbInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("chb.login.username"),
                ConfigReader.get("chb.login.password"),
                ConfigReader.get("chb.application.number"));
    }

    public void chbInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        System.out.println("Community Hall Booking Application Employee Workflow");

        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to CHB Search Bookings
            navigateToSearchBookings(driver, wait, js);

            // STEP 3: Searching the Application By Booking Number
            searchByApplicationNumber(driver, wait, js,applicationNumber);

            // STEP 4: Click Take Action
            clickTakeAction(driver, wait, js);

            // STEP 5: Pop up Cancel CHB Booking
            handleCancelPopup(driver, wait, js);



            System.out.println("Community Hall Booking Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            System.out.println("Exception in Community Hall Booking Application Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        }finally {
            if (driver != null) {
                driver.quit();
            }}
    }

    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        System.out.println("Open the Employee Login Portal");

        // Enter credentials from configuration
        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        System.out.println("Filled username and password");

        // Select city dropdown
        selectMohaliDropdown(driver, wait, actions);

        // Click Continue button
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");
    }

    /**
     * Navigates to Community Hall Booking Search Bookings
     */
    private void navigateToSearchBookings(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Navigating to Search Bookings");

        // Wait for page to load after login
        Thread.sleep(2000);

        // Click Search Bookings link
        WebElement searchBookingLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='Community Hall Booking']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Search Bookings']")));
        js.executeScript("arguments[0].scrollIntoView(true);", searchBookingLink);
        searchBookingLink.click();
        System.out.println("Clicked Search Bookings link");
    }

    /**
     * Navigates to CHB Search Application Through Booking Number
     */

    private void searchByApplicationNumber(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        System.out.println("Searching Application");

        Thread.sleep(1000);

        String bookingNumber;

        // =========================
        // STEP 1: USE INPUT OR FALLBACK
        // =========================
        if (applicationNumber != null && !applicationNumber.trim().isEmpty()
                && !applicationNumber.contains("@")) {

            bookingNumber = applicationNumber.trim();
            System.out.println("Using Application Number from UI: " + bookingNumber);

        } else {

            System.out.println("No valid input → fetching from table");

            WebElement firstBooking = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//table//tbody//tr[1]//td[1])")
            ));

            bookingNumber = firstBooking.getText().trim();

            System.out.println("Captured Booking Number: " + bookingNumber);
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

        System.out.println("Booking number entered");

        // =========================
        // STEP 3: CLICK SEARCH
        // =========================
        By searchLocator = By.xpath("//button[normalize-space()='Search']");

        WebElement searchBtn = wait.until(ExpectedConditions.presenceOfElementLocated(searchLocator));

        searchBtn = driver.findElement(searchLocator);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", searchBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", searchBtn);

        System.out.println("Search button clicked");
    }

    private void clickTakeAction(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Clicking TAKE ACTION");

        // =========================
        // STEP 1: CLICK TAKE ACTION
        // =========================
        By takeActionLocator = By.xpath("//button[normalize-space()='TAKE ACTION']");

        WebElement takeActionBtn = wait.until(ExpectedConditions.elementToBeClickable(takeActionLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", takeActionBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", takeActionBtn);

        System.out.println("TAKE ACTION clicked");

        // =========================
        // STEP 2: WAIT & CLICK CANCEL
        // =========================
        By cancelLocator = By.xpath("//*[contains(text(),'Cancel Booking')]");

        WebElement cancelOption = wait.until(ExpectedConditions.visibilityOfElementLocated(cancelLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cancelOption);
        Thread.sleep(500);

        try {
            cancelOption.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", cancelOption);
        }

        System.out.println("Cancel Community Hall Booking clicked");
    }

    private void handleCancelPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Cancel Popup");

        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
        if (!checkboxes.isEmpty()) {
            WebElement lastCheckbox = checkboxes.get(checkboxes.size() - 1);
            try {
                if (!lastCheckbox.isSelected()) {
                    js.executeScript("arguments[0].scrollIntoView(true);", lastCheckbox);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].click();", lastCheckbox);
                    System.out.println("Checked declaration checkbox");
                }
            } catch (Exception ex) {
                System.out.println("Could not click declaration checkbox: " + ex.getMessage());
            }


            //Cancel Button
            By cancelBtnLocator = By.xpath("//button[normalize-space()='Cancel Booking']");

            WebElement cancelBtn = wait.until(ExpectedConditions.elementToBeClickable(cancelBtnLocator));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", cancelBtn);
            Thread.sleep(500);

            try {
                cancelBtn.click();
            } catch (Exception e) {
                js.executeScript("arguments[0].click();", cancelBtn);
            }

            System.out.println("Final Cancel clicked");

            // backend process ke liye wait
            Thread.sleep(2000);
            driver.navigate().refresh();

            System.out.println("Page refreshed after cancel");
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

    private void selectMohaliDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
        WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
        actions.moveToElement(cityDropdownArrow).click().perform();

        WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")
        ));

        // CHANGE HERE
        WebElement mohaliOption = dropdownOptions.findElement(
                By.cssSelector(".profile-dropdown--item:nth-child(8)")
        );

        actions.moveToElement(mohaliOption).click().perform();
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
}
