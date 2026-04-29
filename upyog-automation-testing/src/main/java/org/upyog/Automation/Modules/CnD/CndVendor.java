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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class CndVendor {
    
    private static final Logger logger= LoggerFactory.getLogger(CndVendor.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void cndVReg() {
        cndVReg(ConfigReader.get("citizen.base.url"),
                "CnD",
                ConfigReader.get("cnd.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"),
                ConfigReader.get("cnd.application.number"));
    }

    public void cndVReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String applicationNumber) {
        logger.info("CnD Application");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to CnD Vendor
            navigateToCndVendor(driver, wait, js);

            // STEP 3: Navigate to Search Application
            navigateToSearchApplication(driver, wait, js);

            // STEP 4: Search CnD Application by Application Number
            searchByCndApplication(driver, wait, js, applicationNumber);

            // STEP 5: Take Action Assign
            takeActionAndAssign(driver, wait, js);

            // STEP 6: Assign Vehicle Popup Cancel
            handleAssignCancelPopup(driver, wait, js);

            // STEP 7: Assign Vehicle 1
            takeActionAndAssign1(driver, wait, js);

            // STEP 8: Assign Vehicle Popup
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
    // STEP 2: NAVIGATE TO CND VENDOR
    // =====================================================================

    private void navigateToCndVendor(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Construction and Demolition");

        // Sidebar CnD link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/cnd-ui/citizen/cnd-home']"))));

        Thread.sleep(2000);
        logger.info("Reached CnD home page");

        // "C&D Waste Pickup Request" link
        WebElement cndRequest = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/cnd-ui/citizen/cnd/cnd-vendor']")
        ));

        cndRequest.click();

        logger.info("Clicked C&D Waste Pickup Request");
    }

    // =====================================================================
    // STEP 3: SEARCH APPLICATION
    // =====================================================================

    private void navigateToSearchApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Search CnD Application");

        // Wait for page load
        Thread.sleep(2000);

        // Direct stable locator using href
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href,'/cnd/inbox')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        js.executeScript("arguments[0].click();", inboxLink);

        logger.info("Clicked Inbox link");
    }

    // =====================================================================
    // STEP 4: SEARCH CND APPLICATION BY APPLICATION NUMBER
    // =====================================================================

    private void searchByCndApplication(WebDriver driver, WebDriverWait wait,
                                        JavascriptExecutor js, String applicationNumber)
            throws InterruptedException {

        logger.info("Searching CnD Application in Inbox");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);

        String cndId = applicationNumber.trim();
        logger.info("Using CND ID: " + cndId);

        WebElement cndInput = null;

        try {
            cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'search-complaint-container')]//input[@type='text'])[1]")
            ));
            logger.info("Found using index fallback locator");


        } catch (Exception e1) {

            try {
                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h4[text()='Application Number ']/parent::span//input")
                ));
                logger.info("Found using label-based locator");

            } catch (Exception e2) {

                cndInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.name("applicationNumber")
                ));
                logger.info("Found using name locator");
            }
        }

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", cndInput);
        Thread.sleep(500);

        cndInput.clear();
        cndInput.sendKeys(cndId);

        logger.info("CnD Application Number entered");

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

        logger.info("Application clicked: " + cndId);
    }

    // =====================================================================
    // STEP 5: TAKE ACTION ASSIGN
    // =====================================================================

    private void takeActionAndAssign(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Assign");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ASSIGN DRIVER CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vehicle']");

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
    // STEP 6: POP UP ASSIGN VENDOR - CANCEL
    // =====================================================================

    private void handleAssignCancelPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Cancel First");
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

        logger.info("Cancel clicked");
        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 7: TAKE ACTION ASSIGN
    // =====================================================================

    private void takeActionAndAssign1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Starting Take Action → Assign");


        // TAKE ACTION

        clickTakeActionButton(driver, wait);
        Thread.sleep(500);

        //  WAIT FOR DROPDOWN

        By dropdownLocator = By.xpath("//div[contains(@class,'menu-wrap')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dropdownLocator));

        Thread.sleep(500);


        // ASSIGN DRIVER CLICK

        By assignLocator = By.xpath("//p[normalize-space()='Assign Vehicle']");

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
    // STEP 8: POP UP ASSIGN VEHICLE
    // =====================================================================

    private void handleAssignVehiclePopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling Assign Vehicle Popup");
        Thread.sleep(1000);


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


        // STEP 5: CLICK SUBMIT BUTTON

        By assignBtnLocator = By.xpath("//button[normalize-space()='SUBMIT']");

        WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(assignBtnLocator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", assignBtn);
        Thread.sleep(500);

        try {
            assignBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", assignBtn);
        }

        logger.info("Final Submit clicked");
        Thread.sleep(5000);
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
