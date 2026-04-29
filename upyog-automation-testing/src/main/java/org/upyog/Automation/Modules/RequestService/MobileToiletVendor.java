package org.upyog.Automation.Modules.RequestService;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
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

import java.util.List;

@Component
public class MobileToiletVendor {

    private static final Logger logger = LoggerFactory.getLogger(MobileToiletVendor.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void mobileToiletVCreate() {
        mobileToiletVCreate(ConfigReader.get("citizen.base.url"),
                "Request Service",
                ConfigReader.get("toilet.vendor.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"),
                ConfigReader.get("mobileToilet.application.number"));
    }

    public void mobileToiletVCreate(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String applicationNumber) {
        logger.info("Mobile Toilet Vendor Application");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Mobile Toilet Vendor
            navigateToMobileToiletVendor(driver, wait, js);

            // STEP 3: Navigate to Search Bookings
            navigateToSearchApplication(driver, wait, js);

            // STEP 4: Search By Booking Number
            searchByBookingNoVendor(driver, wait, js, applicationNumber);

            // STEP 5: Take Action Assign
            takeActionAndAssign(driver, wait, js);

            // STEP 6: Assign Vehicle
            handleAssignVehiclePopup(driver, wait, js);


        } catch (Exception e) {
            logger.info("Exception in Mobile Toilet: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // =====================================================================
    // STEP 1: CITIZEN LOGIN
    // =====================================================================

    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
            throws InterruptedException {

        driver.get(baseUrl);
        logger.info("Open the Citizen Login Portal");

        // Mobile number
        fillInput(wait, "mobileNumber", mobileNumber);

        // Accept terms checkbox
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='checkbox'].form-field")));
        if (!checkbox.isSelected()) {
            js.executeScript("arguments[0].click();", checkbox);
            Thread.sleep(1000);
        }

        // Next
        clickButton(wait, js, "//button[@type='submit']//header[text()='Next']/..");

        // OTP
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.input-otp-wrap")));
        List<WebElement> otpInputs = driver.findElements(By.cssSelector("input.input-otp"));
        for (int i = 0; i < otp.length() && i < otpInputs.size(); i++) {
            otpInputs.get(i).sendKeys(String.valueOf(otp.charAt(i)));
        }

        // Submit OTP
        clickButton(wait, js, "//button[@type='submit']//header[text()='Next']/..");

        // Select city
        selectCity(driver, wait, js, cityName);

        // Continue
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
        actions.moveToElement(continueBtn).click().perform();
        Thread.sleep(3000);
    }

    // =====================================================================
    // STEP 2: NAVIGATE TO MOBILE TOILET VENDOR
    // =====================================================================

    private void navigateToMobileToiletVendor(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Request Service");

        // Sidebar Request Service link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/wt-home']"))));
        Thread.sleep(3000);
        // "Request Service" card
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'CitizenHomeCard')]//a[text()='Mobile Toilet Vendor Login']"))));

        Thread.sleep(3000);
    }

    // =====================================================================
    // STEP 3: NAVIGATE TO SEARCH BOOKINGS OF MOBILE TOILET VENDOR
    // =====================================================================

    private void navigateToSearchApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Navigating to Search Application");

        // Wait for page to load after login
        Thread.sleep(2000);

        logger.info("Navigating to Search Application");

// Click using anchor (NOT div)
        WebElement searchApplicationLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[contains(@href,'my-bookings')]")));

        js.executeScript("arguments[0].scrollIntoView(true);", searchApplicationLink);
        js.executeScript("arguments[0].click();", searchApplicationLink);

        logger.info("Clicked Search Application");

// Correct URL wait
        wait.until(ExpectedConditions.urlContains("my-bookings"));

        logger.info("Navigation successful");

    }

    // =====================================================================
    // STEP 4: SEARCH BOOKING OF MOBILE TOILET VENDOR
    // =====================================================================

    private void searchByBookingNoVendor(WebDriver driver, WebDriverWait wait,
                                         JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching Booking in Search Application");

        wait.until(ExpectedConditions.urlContains("my-bookings"));
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
                        By.xpath("(//div[contains(@class,'search-form-wrapper')]//input[@type='text'])[1]")
                ));
                logger.info("Found using index fallback locator");

            } catch (Exception e2) {

                mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[text()='Booking No.']/following::input[1]")
                ));
                logger.info("Found using label-based locator");
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
    // STEP 5: TAKE ACTION ASSIGN
    // =====================================================================

    private void takeActionAndAssign(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Verify");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ASSIGN DRIVER CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vehicle Driver']");

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
    // STEP 6: POP UP ASSIGN VEHICLE
    // =====================================================================

    private void handleAssignVehiclePopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Assign Vehicle Popup");


        // STEP 1: WAIT FOR POPUP

        By popupLocator = By.xpath("//div[contains(@class,'popup-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(popupLocator));

        Thread.sleep(2000);


        // STEP 2: DROPDOWN


        selectDropdownByIndex(driver, wait, js, 0,0 );
        Thread.sleep(2000);
        logger.info("PN 45 HU 8485");
        WebElement input = driver.findElement(By.xpath("//input[@type='text']"));

        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input,
                "PN 45 HU 8485"   // exact selected value
        );


        // STEP 4: ENTER COMMENT


        By commentLocator = By.xpath("//textarea[@name='comments']");

        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(commentLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(2000);

        commentBox.clear();
        commentBox.sendKeys("Assigned");

        logger.info("Comment entered");
        Thread.sleep(1000);


        // STEP 5: CLICK ASSIGN VEHICLE BUTTON

        By assignBtnLocator = By.xpath("//button[normalize-space()='Assign Vehicle Driver']");

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

    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    private void fillInputFast(WebDriver driver, WebDriverWait wait, String fieldName, String value) {
        WebDriverWait fastWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(3));
        WebElement input = fastWait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].scrollIntoView(true);", button);
        Thread.sleep(300);
        button.click();
    }

    private void selectCity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String cityName)
            throws InterruptedException {

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper")));

        List<WebElement> cityOptions = driver.findElements(
                By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper div"));

        for (WebElement option : cityOptions) {
            WebElement label = option.findElement(By.tagName("label"));
            if (label.getText().trim().equals(cityName)) {
                WebElement radioInput = option.findElement(By.cssSelector("input[type='radio']"));
                if (!radioInput.isSelected()) {
                    js.executeScript("arguments[0].click();", radioInput);
                    Thread.sleep(1000);
                }
                return;
            }
        }
        throw new RuntimeException("Failed to select city: " + cityName);
    }

    private void clickSaveAndNext(WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(.,'Save & Next')]")));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        js.executeScript("arguments[0].click();", btn);
        Thread.sleep(1000);
    }

    private void selectDropdownOption(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, int dropdownIndex)
            throws InterruptedException {

        List<WebElement> dropdownSvgs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("div.select svg.cp")));

        if (dropdownIndex >= dropdownSvgs.size()) {
            logger.info("Dropdown index " + dropdownIndex + " not found. Total: " + dropdownSvgs.size());
            return;
        }

        WebElement svg = dropdownSvgs.get(dropdownIndex);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", svg);
        Thread.sleep(200);

        try {
            svg.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript(
                    "var ev = document.createEvent('MouseEvents');" +
                            "ev.initEvent('click', true, true);" +
                            "arguments[0].dispatchEvent(ev);",
                    svg);
        }

        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.options-card")));

        List<WebElement> options = optionsContainer.findElements(
                By.cssSelector("div.profile-dropdown--item"));

        if (!options.isEmpty()) {
            WebElement firstOption = options.get(0);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", firstOption);
            Thread.sleep(150);
            js.executeScript("arguments[0].click();", firstOption);
        }
    }
    private void selectRadioByLabel(WebDriver driver, String labelText) {

        WebElement radio = driver.findElement(
                By.xpath("//label[normalize-space()='" + labelText + "']/preceding-sibling::input")
        );

        if (!radio.isSelected()) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", radio);
        }
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
