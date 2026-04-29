package org.upyog.Automation.Modules.WaterAndSewerage;

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
public class WAndSCreate {

    private static final Logger logger = LoggerFactory.getLogger(WAndSCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;
    //@PostConstruct

    public void wandSReg() {
        wandSReg(ConfigReader.get("citizen.base.url"),
                "Water And Sewerage",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
    }

    public void wandSReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Community Hall Booking by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Water and Sewerage
            navigateToWaterAndSewerage(driver, wait, js);

            // STEP 3: Search Property
            searchProperty(driver, wait, js);

            // STEP 4: Select Property
            selectSearchedProperty(driver, wait, js);

            // STEP 5: Info Page
            infoPage(driver, wait, js);

            // STEP 6: Property Details
            propertyDetail(driver, wait, js);

            // STEP 7: Common Connection Details
            commonConnectionDetail(driver, wait, js);

            // STEP 8: Choose Service
            chooseServiceType(driver, wait, js);

            // STEP 9: Connection Details
            connectionDetail(driver, wait, js);

            // STEP 10: Sewerage Connection Details
            sewerageDetails(driver, wait, js);

            // STEP 11: Upload Document
            uploadDocumentPage(driver, wait, js);

            // STEP 12: Summary Page
            submitApplication(driver, wait,js);



        } catch (Exception e) {
            logger.info("Exception in Water and Sewerage Connection: " + e.getMessage());
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
    // STEP 2: NAVIGATE TO WATER AND SEWERAGE MODULE
    // =====================================================================


    private void navigateToWaterAndSewerage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Water And Sewerage");

        // Sidebar Water And Sewerage link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/ws-home']"))));

        Thread.sleep(2000);
        logger.info("Reached Water And Sewerage home page");

        // "New Connection" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/ws/create-application/docs-required']"))));
        logger.info("Clicked New Connection link");
    }

    // =====================================================================
    // STEP 3: SEARCH PROPERTY
    // =====================================================================


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
                        By.xpath("//h2[contains(.,\"Mobile number\")]/following::input[contains(@class,'employee-card-input')][1]")
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
    // STEP 4: SELECT PROPERTY
    // =====================================================================


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
    // STEP 5: INFO PAGE DETAILS
    // =====================================================================


    private void infoPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Construction and Demolition Info Page - Clicking Next");
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
                logger.info("Clicked Next on info page");
                return;
            } catch (Exception e) {
                logger.info("Next selector failed: " + selector);
            }
        }

        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 6: PROPERTY DETAILS
    // =====================================================================

    private void propertyDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Selecting Property");

        clickNextButton(driver, wait, js);
        Thread.sleep(500);

        logger.info("Selected Property Details");
    }

    // =====================================================================
    // STEP 7: COMMON CONNECTION DETAILS
    // =====================================================================

    private void commonConnectionDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Selecting Property");

        clickNextButton(driver, wait, js);
        Thread.sleep(500);

        logger.info("Selected Property Details");
    }

    // =====================================================================
    // STEP 8: CHOOSE SERVICE TYPE
    // =====================================================================

    private void chooseServiceType(WebDriver driver,WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Select Service Name ");
        Thread.sleep(500);

        selectRadioButtonByLabel(driver, "Both Water and Sewerage");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(500);
        logger.info("Selected Service Name");
    }

    // =====================================================================
    // STEP 9: CONNECTION DETAILS
    // =====================================================================

    private void connectionDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Filling Connection Details");

        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        if (inputs.size() >= 1)

            fillInputField(js, inputs.get(0), "10");
        Thread.sleep(500);


        selectDropdownByIndex(driver, wait, js,0,0);
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

    // =====================================================================
    // STEP 10: SEWERAGE CONNECTION DETAILS
    // =====================================================================

    private void sewerageDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Sewerage Connection Details");

        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        if (inputs.size() >= 2)

            fillInputField(js, inputs.get(0), "6");
        Thread.sleep(500);

        fillInputField(js, inputs.get(1), "4");
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 11: DOCUMENTS UPLOAD
    // =====================================================================

    private void uploadDocumentPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        logger.info("Uploading Documents");
        Thread.sleep(2000);

        // Document 0: Identity proof
        selectDropdownByIndex(driver, wait, js, 0, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.identity1.proof"));
        Thread.sleep(2000);

        // Document 1: Address proof
        selectDropdownByIndex(driver, wait, js, 1, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.address1.proof"));
        Thread.sleep(2000);

        // Document 2: Land Tax Receipt
        selectDropdownByIndex(driver, wait, js, 2, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.electricityBill.proof"));
        Thread.sleep(2000);

        // Document 3: Title deed
        selectDropdownByIndex(driver, wait, js, 3, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 3, ConfigReader.get("document.plumberReportDrawing.proof"));
        Thread.sleep(2000);

        // Wait for Next button to be enabled
        wait.until(driver1 -> {
            WebElement btn = driver.findElement(By.xpath("//button[contains(.,'Next')]"));
            return btn.isEnabled() && !btn.getAttribute("class").contains("disabled");
        });

        WebElement nextBtn = driver.findElement(By.xpath("//button[contains(., 'Next')]"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", nextBtn);
        logger.info("Clicked Next button");
        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 12: SUMMARY PAGE
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Submitting Water And Sewerage Application - Summary Page");
        Thread.sleep(3000);


        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='SUBMIT']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("Water And Sewerage application: Submit clicked");
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

        // 🔥 CLICK PARENT, NOT INPUT
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