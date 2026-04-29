package org.upyog.Automation.Modules.EWaste;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.config.WebDriverFactory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component
public class EWasteCreate {

    private static final Logger logger = LoggerFactory.getLogger(EWasteCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct

    public void eWasteReg() {
        eWasteReg(ConfigReader.get("citizen.base.url"),
                "E-Waste",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
    }

    public void eWasteReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("EWaste Management Booking");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to EWaste
            navigateToEWaste(driver, wait, js);

            // STEP 3: Selection of Product
            searchProduct(driver, wait, js);

            //STEP 4: Upload Image
            uploadImage(driver, wait, js);

            //STEP 5: Applicant Details
            fillApplicantDetails(driver, wait, js);

            //STEP 6: Address Details
            addressPincode(driver, wait, js);

            //STEP 7: Select City
            selectCity(driver, wait, js);

            //STEP 8: Address Details
            addressDetails(driver, wait, js);

            //STEP 9: Summary Page
            submitApplication(driver, wait, js);

        } catch (Exception e) {
            logger.info("Exception in EWaste Management Booking: " + e.getMessage());
            e.printStackTrace();
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

        // Select city`1
        selectCity(driver, wait, js, cityName);

        // Continue
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
        actions.moveToElement(continueBtn).click().perform();
    }

    // =====================================================================
    // STEP 2: NAVIGATE TO E-WASTE MODULE
    // =====================================================================

    private void navigateToEWaste(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to E-WASTE Booking");

        // Sidebar E-Waste link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/ew-home']"))));

        Thread.sleep(2000);
        logger.info("Reached EWaste home page");

        // "Create E-Waste Request" link
        WebElement newConstruction = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(),'Create Ewaste Request')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", newConstruction);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", newConstruction);

        logger.info("Clicked Create Ewaste Request");
        Thread.sleep(2000);

    }

    // =====================================================================
    // STEP 3: SELECTION OF PRODUCT
    // =====================================================================

    private void searchProduct(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException{

        logger.info("Search Product Page");

        // Product 1

        try {
            selectDropdownByIndex(driver, wait, js, 0, 1);
            Thread.sleep(1000);
            logger.info("Searched Product");
        } catch (Exception e) {
            logger.info("Search Product not found: " + e.getMessage());
        }

        Thread.sleep(1000);

        WebElement quantity = wait.until(
                ExpectedConditions.elementToBeClickable(By.name("productQuantity"))
        );

        quantity.click();
        Thread.sleep(200);

// Clear properly
        quantity.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        quantity.sendKeys(Keys.BACK_SPACE);

        Thread.sleep(200);

// Type value normally
        quantity.sendKeys("10");
        Thread.sleep(500);

// Add Product Button
        WebElement addProduct = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(.,'Add Product')]")
                )
        );

        js.executeScript("arguments[0].click();", addProduct);
        logger.info("Clicked Add Product");

        Thread.sleep(1500);



        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 4: UPLOAD IMAGE
    // =====================================================================

    private void uploadImage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {
        logger.info( "Selecting the image");

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.EwasteImage.proof"));
        Thread.sleep(3000);
        logger.info("Finished Uploading Image step");

        clickNextButton(driver, wait, js);

    }

    // =====================================================================
    // STEP 5: APPLICANT DETAILS
    // =====================================================================

    private void fillApplicantDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Applicant Details");

        fillInput(wait, "applicantName", "Arpit Rao");
        fillInput(wait, "emailId", "arpit@gmail.com");

        Thread.sleep(500);
        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 6: PIN-CODE DETAILS
    // =====================================================================

    private void addressPincode(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException{

        logger.info("Entering the Address Pincode");
        Thread.sleep(500);
        fillInput(wait, "pincode", "143001");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 7: SELECT CITY
    // =====================================================================

    private void selectCity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Selecting City and Locality");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Delhi");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Main Road Abadpura");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 8: ADDRESS DETAILS
    // =====================================================================

    private void addressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Entering the Address Details");
        Thread.sleep(500);

        fillInput(wait, "street", "Raj Nagar");
        Thread.sleep(500);

        fillInput(wait, "doorNo", "201");
        Thread.sleep(500);

        fillInput(wait, "buildingName", "Jagbir Bhawan");
        Thread.sleep(500);

        fillInput(wait, "addressLine1", "test address 1");
        Thread.sleep(500);

        fillInput(wait, "addressLine2", "test address 2");
        Thread.sleep(500);

        fillInput(wait, "landmark", "test landmark");
        Thread.sleep(500);


        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 9: SUBMIT APPLICATION
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Submitting Ewaste Application - Summary Page");

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
        }

        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("EWaste application: Submit clicked");
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

    // optional field – do not fail if missing
    private void fillOptionalInput(WebDriver driver, WebDriverWait wait, String fieldName, String value) {
        try {
            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
            if (input.isDisplayed() && input.isEnabled()) {
                input.clear();
                input.sendKeys(value);
                logger.info("Filled optional field: " + fieldName);
            } else {
                logger.info("Optional field " + fieldName + " not interactable, skipping");
            }
        } catch (Exception e) {
            logger.info("Optional field " + fieldName + " not found, skipping");
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
        logger.info("Clicked Next");
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
            logger.info("Dropdown index " + dropdownIndex + " not found. Total: " + dropdownSvgs.size());
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
            logger.info("Normal click intercepted on dropdown svg, using JS dispatch. Reason: " + e.getMessage());

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
            logger.info("No options found in dropdown for index " + dropdownIndex);
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
            logger.info("Dropdown index " + dropdownIndex + " out of range");
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


    private void uploadFileToInput(JavascriptExecutor js, List<WebElement> fileInputs, int index, String filePath)
            throws InterruptedException {

        if (index >= fileInputs.size()) {
            logger.info("⚠ No file input at index " + index + " for " + filePath);
            return;
        }

        java.io.File f = new java.io.File(filePath);
        logger.info("Attempting upload from: " + f.getAbsolutePath() + "  exists? " + f.exists());

        if (!f.exists()) {
            logger.info("⚠ File does NOT exist on disk. Skipping this input.");
            return;
        }

        WebElement input = fileInputs.get(index);

        // Make sure Selenium can interact (visibility & scroll)
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", input);
        Thread.sleep(300);

        input.sendKeys(f.getAbsolutePath());
        logger.info(" Uploaded document into input index " + index);
    }


    private void uploadFile(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                            int index, String filePath) throws InterruptedException {

        // All file inputs on the page – match your screenshot (hidden absolute-position input)
        List<WebElement> fileInputs = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("input[type='file'].input-mirror-selector-button"))
        );

        if (index >= fileInputs.size()) {
            logger.info("File input index " + index + " not found for path: " + filePath);
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        // Make sure Selenium can interact with the hidden input
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fileInput);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", fileInput);
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        logger.info("Uploaded file at index " + index + ": " + filePath);
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
}
