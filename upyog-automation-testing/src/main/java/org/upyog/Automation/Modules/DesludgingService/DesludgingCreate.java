package org.upyog.Automation.Modules.DesludgingService;

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

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DesludgingCreate {

    private static final Logger logger= LoggerFactory.getLogger(DesludgingCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;
    //@PostConstruct

    public void desludgingReg() {
        desludgingReg(ConfigReader.get("citizen.base.url"),
                "Desludging Service",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
    }

    public void desludgingReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Community Hall Booking by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Community Hall Booking
            navigateToCommunityHallBooking(driver, wait, js);

            // STEP 3: Service request
            serviceRequest(driver, wait, js);

            // STEP 4: Search Property
            searchProperty(driver, wait, js);

            // STEP 5: Select Property
            selectSearchedProperty(driver, wait, js);

            // STEP 6: Choose Property
            chooseType(driver, wait, js);

            // STEP 7: Choose Sub Property
            chooseSubType(driver, wait, js);

            // STEP 8: Pin Property
            clickPinPropertyLocation(wait, js);

            // STEP 9: Property Pin-code
            fillPincodeDetail(driver, wait, js);

            // STEP 10: Property Address Details
            fillPropertyAddressDetail(driver, wait, js);

            // STEP 11: Property Address Details 2
            fillPropertyAddressDetail2(driver, wait, js);

            // STEP 12: Slum Area
            slumArea(driver, wait, js);

            // STEP 13: Provide Details
            providePropertyDetails(driver, wait, js);

            // STEP 14: Landmark
            fillLandMarkDetail(driver, wait,js);

            // STEP 15: Choose Pit type
            choosePitType(driver, wait, js);

            // STEP 16: Road Width
            roadWidth(driver, wait, js);

            // STEP 17: Upload Image
            uploadComplaintPhoto(driver, wait, js);

            // STEP 18: Payment Details
            paymentDetails(driver, wait, js);

            // STEP 19: Summary Page
            summaryPage(driver, wait, js);


        } catch (Exception e) {
            logger.info("Exception in Desludging Service: " + e.getMessage());
            e.printStackTrace();
        }finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    //=====================================================================
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
    }


    // =====================================================================
    // STEP 2: NAVIGATE TO COMMUNITY HALL BOOKING MODULE
    //=====================================================================


    private void navigateToCommunityHallBooking(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Community Hall Booking");

        // Sidebar Desludging Service link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/fsm-home']"))));

        Thread.sleep(2000);
        logger.info("Reached Desludging Service home page");

        // "Apply for Emptying of Septic Tank / Pit" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/fsm/new-application']"))));

        logger.info("Clicked Apply for Emptying of Septic Tank / Pit link");
    }

    // =====================================================================
    // STEP 3: SERVICE REQUEST
    //=====================================================================

    private void serviceRequest(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Selecting the service");
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js,0,1);
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js,1,0);
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 4: SEARCH PROPERTY
    //=====================================================================


    private void searchProperty(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{
        logger.info("Search Property by Mobile Number");

        try {
            selectDropdownByIndex(driver, wait, js, 0, 0);
            Thread.sleep(1000);
            logger.info("Selected city");
        } catch (Exception e) {
            logger.info("City dropdown not found: " + e.getMessage());
        }

        Thread.sleep(1000);

        WebElement mobileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h2[contains(.,\"Owner's mobile number\")]/following::input[contains(@class,'employee-card-input')][1]")
                )
        );

        js.executeScript(
                "let input = arguments[0];" +
                        "let lastValue = input.value;" +
                        "input.value = '9999999999';" +
                        "let event = new Event('input', { bubbles: true });" +
                        "event.simulated = true;" +
                        "let tracker = input._valueTracker;" +
                        "if (tracker) { tracker.setValue(lastValue); }" +
                        "input.dispatchEvent(event);" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                mobileInput
        );

        WebElement searchBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(@class,'submit-bar') and @type='submit']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", searchBtn);
        Thread.sleep(300);

        searchBtn.click();Thread.sleep(1000);

        logger.info("Clicked Search button");
    }

    // =====================================================================
    // STEP 5: SELECT PROPERTY
    //=====================================================================


    private void selectSearchedProperty(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Selecting Searched Property");
        WebElement selectBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("(//button[normalize-space()='Select'])[1]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", selectBtn);
        Thread.sleep(300);

        js.executeScript("arguments[0].click();", selectBtn);

        logger.info("Clicked Select button");
    }

    // =====================================================================
    // STEP 6: CHOOSE TYPE PROPERTY
    //=====================================================================

    private void chooseType(WebDriver driver,WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Select Type of Property ");
        Thread.sleep(500);

        selectRadioButtonByLabel(driver, "Residential");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);
        logger.info("Type of Property Selected");
    }

    // =====================================================================
    // STEP 7: CHOOSE TYPE PROPERTY
    //=====================================================================

    private void chooseSubType(WebDriver driver,WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Select Sub Type of Property ");
        Thread.sleep(500);

        selectRadioButtonByLabel(driver, "Independent house");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);
        logger.info("Type of Sub Property Selected");
    }

    // =====================================================================
    // STEP 8: PIN PROPERTY
    //=====================================================================

    private void clickPinPropertyLocation(WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking Skip and Continue");

        WebElement skipLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[contains(@class,'card-link cp undefined') and normalize-space()='Skip and Continue']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", skipLink);
        Thread.sleep(300);

        js.executeScript("arguments[0].click();", skipLink);

        Thread.sleep(1000);
        logger.info("Skip and Continue clicked successfully");
    }

    // =====================================================================
    // STEP 9: PIN-CODE PROPERTY
    //=====================================================================

    private void fillPincodeDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Pincode");
        Thread.sleep(1000);

        // Try multiple selectors for pincode input
        By[] pincodeSelectors = {
                By.xpath("//input[contains(@name,'pincode')]"),
                By.xpath("//input[contains(@id,'pincode')]"),
                By.cssSelector("input.employee-card-input")
        };

        WebElement pincodeInput = null;
        for (By selector : pincodeSelectors) {
            try {
                pincodeInput = wait.until(ExpectedConditions.elementToBeClickable(selector));
                break;
            } catch (Exception e) {
                logger.info("Pincode selector failed: " + selector);
            }
        }

        if (pincodeInput != null) {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", pincodeInput);
            pincodeInput.click();
            pincodeInput.clear();
            pincodeInput.sendKeys("143001");

        } else {
            logger.info("Pincode input not found");
        }

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

    // =====================================================================
    // STEP 10: PROVIDE PROPERTY ADDRESS
    //=====================================================================

    private void fillPropertyAddressDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Selecting City and Locality");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Within Ulb Limits");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "City A");
        Thread.sleep(1000);


        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 11: PROVIDE PROPERTY ADDRESS 2
    //=====================================================================

    private void fillPropertyAddressDetail2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Provide Property Details");

        selectDropdownByIndex(driver, wait, js,0,0);
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

    // =====================================================================
    // STEP 12: SLUM AREA
    //=====================================================================

    private void slumArea(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Selecting Slum Area or Not");
        Thread.sleep(500);

        selectRadioButtonByLabel(driver, "NO");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

        logger.info("Selected Slum Or Not");

    }

    // =====================================================================
    // STEP 13: PROVIDE PROPERTY  ADDRESS
    //=====================================================================

    private void providePropertyDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Filling Street name and House No.");
        Thread.sleep(500);

        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        if (inputs.size() >= 2)

            fillInputField(js, inputs.get(0), "Test Street");
        Thread.sleep(500);

        fillInputField(js, inputs.get(1), "89");
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);
    }

    // =====================================================================
    // STEP 14: LANDMARK
    //=====================================================================


    private void fillLandMarkDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Landmark");

        // textarea (NOT input)
        WebElement landmarkTextarea = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("textarea.card-textarea")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", landmarkTextarea);
        Thread.sleep(300);

        landmarkTextarea.click();
        landmarkTextarea.clear();
        landmarkTextarea.sendKeys("Test landmark near main road");

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur',{bubbles:true}));", landmarkTextarea);

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);

        logger.info("Landmark submitted successfully");
    }

    // =====================================================================
    // STEP 15: CHOOSE PIT
    //=====================================================================

    private void choosePitType(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Choose Septic Tank Type");
        Thread.sleep(500);

        selectRadioButtonByLabel(driver, "Conventional septic tank");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);

        logger.info("Selected the Septic Tank Type");

    }

    // =====================================================================
    // STEP 16: ROAD WIDTH
    //=====================================================================

    private void roadWidth(WebDriver driver,WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Filling Road Details");
        Thread.sleep(500);

        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        if (inputs.size() >= 2)

            fillInputField(js, inputs.get(0), "400");
        Thread.sleep(500);

        fillInputField(js, inputs.get(1), "500");
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);

    }

    // =====================================================================
    // STEP 17: UPLOAD IMAGE
    //=====================================================================

    private void uploadComplaintPhoto(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Uploading complaint photo");

        String photoPath = ConfigReader.get("document.pitPhoto.proof");
        logger.info("PHOTO PATH: " + photoPath);

        File f = new File(photoPath);
        if (!f.exists()) {
            throw new RuntimeException("Photo file NOT found: " + photoPath);
        }

        // Find file input
        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@type='file']")
                )
        );

        // Make input usable
        js.executeScript(
                "arguments[0].style.display='block'; arguments[0].style.opacity=1;",
                fileInput
        );
        Thread.sleep(1000);

        // Upload
        fileInput.sendKeys(f.getAbsolutePath());
        logger.info("Complaint photo uploaded");

        Thread.sleep(2000);

        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 18: PAYMENT PAGE
    //=====================================================================

    private void paymentDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Payment Details Page");
        Thread.sleep(1000);

        // Just ensure field visible (optional)
        WebElement amountInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='number']")
        ));

        logger.info("Amount auto-filled: " + amountInput.getAttribute("value"));

        // ================================
        // CLICK NEXT
        // ================================

        WebElement nextBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("button.submit-bar")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
        Thread.sleep(500);

        // If disabled -> enable manually (React hack)
        js.executeScript("arguments[0].removeAttribute('disabled');", nextBtn);

        // React-safe click
        js.executeScript(
                "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles:true}));",
                nextBtn
        );

        logger.info("Clicked Next on Payment Page");

        Thread.sleep(1500);
    }

    // =====================================================================
    // STEP 19: SUMMARY PAGE
    //=====================================================================

    private void summaryPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("Property tax application: Submit clicked");
        Thread.sleep(5000);
    }

 /*
             =====================================================================
             UTILITY METHODS
             =====================================================================
            */

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

    private void fillInputField(JavascriptExecutor js, WebElement input, String value)
            throws InterruptedException {
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        input.click();
        input.clear();
        input.sendKeys(value);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", input);
        Thread.sleep(200);
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

    private boolean tryClickWithRetries(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, By locator,
                                        int timeoutSeconds, int retries, long retryDelayMs)
            throws InterruptedException {
        WebDriverWait localWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(timeoutSeconds));

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                waitForNoOverlay(driver, wait);
                WebElement el = localWait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(locator));
                try {
                    el.click();
                    logger.info("Clicked element " + locator + " (attempt " + attempt + ")");
                    return true;
                } catch (Exception clickEx) {
                    // fallback to JS click
                    try {
                        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                        Thread.sleep(150);
                        js.executeScript("arguments[0].click();", el);
                        logger.info("JS-clicked element " + locator + " (attempt " + attempt + ")");
                        return true;
                    } catch (Exception jsEx) {
                        logger.info("Click failed attempt " + attempt + " for " + locator + " : " + jsEx.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.info("Element not clickable yet (" + locator + ") attempt " + attempt + " : " + e.getMessage());
            }
            Thread.sleep(retryDelayMs);
        }
        return false;
    }

    private void waitForNoOverlay(WebDriver driver, WebDriverWait wait) {
        try {
            java.util.List<By> loaderSelectors = java.util.Arrays.asList(
                    By.cssSelector(".loading"),
                    By.cssSelector(".overlay"),
                    By.cssSelector(".loader"),
                    By.cssSelector(".submit-bar-disabled"),
                    By.cssSelector(".is-loading"),
                    By.cssSelector(".ant-modal-root .ant-spin")
            );
            for (By sel : loaderSelectors) {
                try {
                    wait.until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(sel));
                } catch (Exception ignored) {
                    // not present / timed out -> continue
                }
            }
        } catch (Exception ignored) {}
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

    private void selectRadioByLabel(WebDriver driver,
                                    WebDriverWait wait,
                                    JavascriptExecutor js,
                                    String labelText)
            throws InterruptedException {

        WebElement label = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[normalize-space()='" + labelText + "']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", label);
        Thread.sleep(200);

        try {
            label.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", label);
        }

        Thread.sleep(400);
        logger.info("Selected radio: " + labelText);
    }

    private void fillInputStable(JavascriptExecutor js, WebElement input, String value)
            throws InterruptedException {

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        input.click();
        input.clear();

        for (char c : value.toCharArray()) {
            input.sendKeys(String.valueOf(c));
            Thread.sleep(80);
        }

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", input);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", input);

        Thread.sleep(300);
    }
    private void clickRadioByIndex(List<WebElement> radios,
                                   int index,
                                   JavascriptExecutor js)
            throws InterruptedException {

        WebElement radio = radios.get(index);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", radio);
        Thread.sleep(200);

        // CLICK PARENT, NOT INPUT
        WebElement clickable = radio.findElement(By.xpath(".."));
        js.executeScript("arguments[0].click();", clickable);

        logger.info("Selected radio index: " + index);
        Thread.sleep(500);
    }

    private void selectDateRange(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Selecting Date Range via Calendar Icon");

        // CLICK CALENDAR ICON
        WebElement calendarIcon = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("svg.calendar-icon, svg.cursorPointer")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", calendarIcon);
        Thread.sleep(500);

        // React safe click
        js.executeScript(
                "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles:true}));",
                calendarIcon
        );

        logger.info("Calendar icon clicked");

        Thread.sleep(2000); // wait for calendar UI

        WebElement threeDays = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[text()='Three Days']")
        ));
        js.executeScript("arguments[0].click();", threeDays);

        logger.info("Three Days selected");
    }
}