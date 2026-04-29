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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component

public class CnDRequest {

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct

    public void cndReg() {
        cndReg(ConfigReader.get("citizen.base.url"),
                "CnD",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
    }

    public void cndReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        System.out.println("Construction and Demolition Booking");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to CnD
            navigateToCnD(driver, wait, js);

            // STEP 3 Info page
            infoPage(driver, wait, js);

            // STEP 4 Fill New Details
            selectFillNewDetails(driver, wait, js);

            // STEP 5 Select Pick Up
            selectPickupAndProceed(driver, wait, js);

            // STEP 6 Fill Applicant Details
            fillApplicantDetails(driver, wait, js);

            // STEP 7 Fill Property Nature
            fillPropertyNature(driver, wait, js);

            // STEP 8 Fill Waste Type
            fillWasteType(driver, wait, js);

            // STEP 9 Select Address
            selectAddress(driver, wait, js);

            // STEP 10 Summary Page
            submitApplication(driver, wait, js);



        } catch (Exception e) {
            System.out.println("Exception in Construction and Demolition Booking: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }}
    }

    // =====================================================================
    // STEP 1: CITIZEN LOGIN
    // =====================================================================

    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
            throws InterruptedException {

        driver.get(baseUrl);
        System.out.println("Open the Citizen Login Portal");

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

        // Select city`1
        selectCity(driver, wait, js, cityName);

        // Continue
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
        actions.moveToElement(continueBtn).click().perform();
    }

    // =====================================================================
    // STEP 2: NAVIGATE TO CONSTRUCTION AND DEMOLITION MODULE
    // =====================================================================

    private void navigateToCnD(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to Construction and Demolition");

        // Sidebar CnD link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/cnd-ui/citizen/cnd-home']"))));

        Thread.sleep(2000);
        System.out.println("Reached CnD home page");

        // "C&D Waste Pickup Request" link
        WebElement cndRequest = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/cnd-ui/citizen/cnd/apply']")
        ));

        cndRequest.click();

        System.out.println("Clicked C&D Waste Pickup Request");
    }

    //=====================================================================
    // STEP 3: INFO PAGE DETAILS
    // =====================================================================


    private void infoPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Construction and Demolition Info Page - Clicking Next");
        Thread.sleep(2000);

        // Try multiple Next button selectors for info page
        By[] nextSelectors = {
                By.xpath("//button[contains(.,'Next')]"),

        };

        for (By selector : nextSelectors) {
            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(selector));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", nextBtn);
                System.out.println("Clicked Next on info page");
                return;
            } catch (Exception e) {
                System.out.println("Next selector failed: " + selector);
            }
        }

        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 4: POP-UP – FILL NEW DETAILS
    // =====================================================================

    private void selectFillNewDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling address details – Fill New Details");

        // WAIT for popup
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'popup-module')]")
        ));

        Thread.sleep(1000);

        // Correct locator (header, not button)
        WebElement fillNewBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//header[contains(text(),'FILL_NEW_DETAILS')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fillNewBtn);
        Thread.sleep(500);

        // React safe click
        js.executeScript("arguments[0].click();", fillNewBtn);

        System.out.println("Clicked FILL_NEW_DETAILS");

        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 5: SELECT PICKUP
    // =====================================================================

    private void selectPickupAndProceed(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{
        Thread.sleep(1000);

        System.out.println("Selecting Request for Pick-up");

        selectRadioButtonByLabel(driver, "Request for pick-up");
        Thread.sleep(1000);// second option

        clickSaveAndNextButton(driver, wait, js);

    }

    // =====================================================================
    // STEP 6: FILL APPLICANT DETAILS
    // =====================================================================

    private void fillApplicantDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Applicant Details");

        fillInputClear(driver, wait, js, "applicantName", "Arpit Rao");
        fillInputClear(driver, wait, js, "emailId", "arpit@gmail.com");

        // mobile optional
        try {
            fillInputClear(driver, wait, js, "mobileNumber", "9999999999");
        } catch (Exception e) {
            System.out.println("Mobile skipped");
        }

        Thread.sleep(1500);

        // IMPORTANT: wait until button ENABLED
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(@class,'submit-bar')]")
        ));

        wait.until(driver1 -> btn.isEnabled());

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", btn);

        System.out.println("Clicked Save & Next");
    }

    // =====================================================================
    // STEP 7: FILL PROPERTY NATURE
    // =====================================================================

    private void fillPropertyNature(WebDriver driver,WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        System.out.println("Filling Property Nature");
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 0,1);
        Thread.sleep(500);

        fillInput(wait, "houseArea", "1600");

// ===============================
// TIME PERIOD DATE HANDLING
// ===============================

// ===== FROM DATE (Fixed: 02/06/1996) =====
        WebElement fromDate = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[contains(text(),'Time Period')]/following::input[1]")
        ));

        fromDate.click();
        fromDate.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        fromDate.sendKeys("02/06/1996");

// React trigger
        js.executeScript(
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }))",
                fromDate
        );


// ===== TO DATE (Current Date) =====
        WebElement toDate = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[contains(text(),'Time Period')]/following::input[2]")
        ));

// Get today's date
        String todayDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        toDate.click();
        toDate.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        toDate.sendKeys(todayDate);

// React trigger
        js.executeScript(
                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }))",
                toDate
        );


// ===== DEBUG (optional but useful) =====
        System.out.println("From Date set: 02/06/1996");
        System.out.println("To Date set: " + todayDate);

        selectDropdownByIndex(driver, wait, js, 1,3);
        Thread.sleep(500);

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(@class,'submit-bar')]")
        ));

        wait.until(driver1 -> btn.isEnabled());

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", btn);

        System.out.println("Clicked Save & Next");

    }

    // =====================================================================
    // STEP 8: FILL PROPERTY NATURE
    // =====================================================================

    private void fillWasteType(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        System.out.println("Filling Waste Type");
        Thread.sleep(1000);

        // Multiple selection dropdown

        selectWasteType(driver, wait, js);
        Thread.sleep(1000);
        System.out.println(driver.getPageSource());

        fillInput(wait, "wasteQuantity", "10");

        List<WebElement> dateInputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input[type='date']")
                )
        );

        if (dateInputs.size() >= 1) {

            WebElement requestedDate = dateInputs.get(0);

            // Get future date (5 days from today)
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(5);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            requestedDate.clear();
            requestedDate.sendKeys(futureDate.format(formatter));

            System.out.println("Delivery Date: " + futureDate.format(formatter));

        } else {
            System.out.println("Delivery Date input not found");
        }

        Thread.sleep(1000);

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.siteMediaPhoto.proof"));
        Thread.sleep(1000);
        System.out.println("Finished Upload Documents step");

        uploadFile(driver, wait, js, 1, ConfigReader.get("document.stackPhoto.proof"));
        Thread.sleep(1000);
        System.out.println("Finished Upload Documents step");

        clickSaveAndNextButton(driver, wait, js);
        Thread.sleep(500);

    }

    // =====================================================================
    // STEP 9: SELECT ADDRESS
    // =====================================================================

    private void selectAddress(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Address Selection");

        selectAddressCard(driver, wait, js);

        clickSaveNextButton(driver, wait, js);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//header[contains(text(),'Summary')]")),
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//button//header[text()='Submit Application']"))
        ));

        System.out.println("Navigated to Summary Page");
    }

    // =====================================================================
    // STEP 10: SUMMARY PAGE
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Submitting Property Tax Application - Summary Page");

        // WAIT for checkbox
        WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='checkbox']")
        ));

        if (!checkbox.isSelected()) {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", checkbox);
            js.executeScript("arguments[0].click();", checkbox);
            System.out.println("Checked declaration checkbox");
        }

        // WAIT for submit button
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//header[text()='Submit Application']]")
        ));

        // SCROLL BEFORE CLICK
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitButton);

        // SINGLE CLICK ONLY
        js.executeScript("arguments[0].click();", submitButton);

        System.out.println("Property tax application: Submit clicked");

        // WAIT FOR NEXT STEP (VERY IMPORTANT)
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("acknowledgement"),
                ExpectedConditions.urlContains("payment"),
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Application Submitted')]"))
        ));

        System.out.println("Application submitted successfully");
    }

    // =====================================================================
    // UTILITY METHODS
    // =====================================================================

    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }
    private void fillInputClear(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                                String nameAttr, String value) throws InterruptedException {

        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.name(nameAttr)
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        Thread.sleep(300);

        // clear properly
        js.executeScript("arguments[0].value='';", input);

        // set value + trigger React events
        js.executeScript(
                "arguments[0].value=arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input, value
        );

        System.out.println("Filled (React-safe): " + nameAttr);
    }

    // optional field – do not fail if missing
    private void fillOptionalInput(WebDriver driver, WebDriverWait wait, String fieldName, String value) {
        try {
            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
            if (input.isDisplayed() && input.isEnabled()) {
                input.clear();
                input.sendKeys(value);
                System.out.println("Filled optional field: " + fieldName);
            } else {
                System.out.println("Optional field " + fieldName + " not interactable, skipping");
            }
        } catch (Exception e) {
            System.out.println("Optional field " + fieldName + " not found, skipping");
        }
    }

    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].scrollIntoView(true);", button);
        Thread.sleep(300);
        button.click();
    }

    private void clickButtonByHeader(WebDriver driver, WebDriverWait wait, String headerText)
            throws InterruptedException {

        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[contains(text(),'" + headerText + "')]]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        Thread.sleep(500);
    }

    private void clickNextButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'submit-bar') and .//header[text()='Next']]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextButton);
        Thread.sleep(200);
        nextButton.click();
        System.out.println("Clicked Next");
    }

    private void clickSaveAndNextButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'submit-bar') and .//header[text()='Save & Next']]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextButton);
        Thread.sleep(200);
        nextButton.click();
        System.out.println("Clicked Save & Next");
    }
    private void clickSaveNextButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Clicking Save & Next");

        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[normalize-space()='Save & Next']")
        ));

        // Scroll
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(500);

        // React safe click
        js.executeScript("arguments[0].click();", btn);

        System.out.println(" Clicked Save & Next");
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

    private void selectDropdownOption(WebDriver driver,
                                      WebDriverWait wait,
                                      JavascriptExecutor js,
                                      int dropdownIndex) throws InterruptedException {

        // get all dropdown arrow svgs on the page
        java.util.List<WebElement> dropdownSvgs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("div.select svg.cp"))
        );

        if (dropdownIndex < 0 || dropdownIndex >= dropdownSvgs.size()) {
            System.out.println("Dropdown index " + dropdownIndex + " not found. Total: " + dropdownSvgs.size());
            return;
        }

        WebElement svg = dropdownSvgs.get(dropdownIndex);

        // scroll into view
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", svg);
        Thread.sleep(200);

        // 1st try: normal Selenium click
        try {
            svg.click();
        } catch (ElementClickInterceptedException e) {
            System.out.println("Normal click intercepted on dropdown svg, using JS dispatch. Reason: " + e.getMessage());

            // 2nd try: dispatch a click event manually (no arguments[0].click())
            js.executeScript(
                    "var ev = document.createEvent('MouseEvents');" +
                            "ev.initEvent('click', true, true);" +
                            "arguments[0].dispatchEvent(ev);",
                    svg
            );
        }

        // wait for options panel
        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.options-card"))
        );

        java.util.List<WebElement> options = optionsContainer.findElements(
                By.cssSelector("div.profile-dropdown--item")
        );

        if (!options.isEmpty()) {
            WebElement firstOption = options.get(0);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", firstOption);
            Thread.sleep(150);

            try {
                firstOption.click();
            } catch (ElementClickInterceptedException e) {
                js.executeScript(
                        "var ev = document.createEvent('MouseEvents');" +
                                "ev.initEvent('click', true, true);" +
                                "arguments[0].dispatchEvent(ev);",
                        firstOption
                );
            }
        } else {
            System.out.println("No options found in dropdown for index " + dropdownIndex);
        }
    }

    /**
     * For document page: we already have the list of svg dropdown icons.
     */
    private void selectDropdownOptionByIndex(WebDriver driver,
                                             WebDriverWait wait,
                                             JavascriptExecutor js,
                                             List<WebElement> dropdownSvgs,
                                             int dropdownIndex,
                                             int optionIndex) throws InterruptedException {

        if (dropdownIndex >= dropdownSvgs.size()) {
            System.out.println("Dropdown index " + dropdownIndex + " out of range");
            return;
        }

        WebElement svg = dropdownSvgs.get(dropdownIndex);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", svg);
        Thread.sleep(200);

        js.executeScript("arguments[0].click();", svg);

        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card"))
        );

        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));

        if (!options.isEmpty() && optionIndex < options.size()) {
            js.executeScript("arguments[0].click();", options.get(optionIndex));
        }
    }

    private void selectWasteType(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Selecting Waste Type");

        //  Locate dropdown
        WebElement dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'multi-select-dropdown-wrap')]//div[contains(@class,'master')]")
        ));

        //  OPEN dropdown
        js.executeScript("arguments[0].click();", dropdown);

        //  WAIT options visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'option-item')]")
        ));

        //  SELECT required options
        clickCustomCheckbox(driver, js, "Mixed Waste");
        clickCustomCheckbox(driver, js, "Cement Bags");
        //  CLOSE dropdown (IMPORTANT)
        js.executeScript("arguments[0].click();", dropdown);

        System.out.println("Waste Type selected and dropdown closed");
    }
    private void openWasteDropdown(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        System.out.println("Opening Waste Dropdown");

        WebElement dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'multi-select-dropdown-wrap')]//div[contains(@class,'master')]")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);
        js.executeScript("arguments[0].click();", dropdown);

        System.out.println("Dropdown clicked");
    }

    private void clickCustomCheckbox(WebDriver driver, JavascriptExecutor js, String text) {
        WebElement option = driver.findElement(
                By.xpath("//div[contains(@class,'option-item')][.//p[contains(text(),'" + text + "')]]")
        );

        WebElement input = option.findElement(By.xpath(".//input[@type='checkbox']"));

        js.executeScript("arguments[0].click();", input);
    }


    private void uploadFileToInput(JavascriptExecutor js, List<WebElement> fileInputs, int index, String filePath)
            throws InterruptedException {

        if (index >= fileInputs.size()) {
            System.out.println("No file input at index " + index + " for " + filePath);
            return;
        }

        java.io.File f = new java.io.File(filePath);
        System.out.println("Attempting upload from: " + f.getAbsolutePath() + "  exists? " + f.exists());

        if (!f.exists()) {
            System.out.println("File does NOT exist on disk. Skipping this input.");
            return;
        }

        WebElement input = fileInputs.get(index);

        // Make sure Selenium can interact (visibility & scroll)
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", input);
        Thread.sleep(300);

        input.sendKeys(f.getAbsolutePath());
        System.out.println("Uploaded document into input index " + index);
    }


    private void uploadFile(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                            int index, String filePath) throws InterruptedException {

        // All file inputs on the page – match your screenshot (hidden absolute-position input)
        List<WebElement> fileInputs = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("input[type='file'].input-mirror-selector-button"))
        );

        if (index >= fileInputs.size()) {
            System.out.println("File input index " + index + " not found for path: " + filePath);
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        // Make sure Selenium can interact with the hidden input
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fileInput);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", fileInput);
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        System.out.println("Uploaded file at index " + index + ": " + filePath);
        Thread.sleep(500);
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

    private void selectAddressCard(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        System.out.println("Selecting Address Card");

        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//div[contains(@class,'card')])[1]")
        ));

        //  IMPORTANT: inner clickable div
        WebElement clickableDiv = card.findElement(By.xpath(".//div[@style[contains(.,'cursor: pointer')]]"));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", clickableDiv);

        //  React safe click
        js.executeScript("arguments[0].click();", clickableDiv);

        System.out.println(" Address card selected");
    }
}
