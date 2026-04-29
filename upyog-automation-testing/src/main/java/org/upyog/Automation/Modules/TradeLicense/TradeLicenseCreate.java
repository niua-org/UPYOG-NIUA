package org.upyog.Automation.Modules.TradeLicense;

import java.io.File;
import java.util.List;
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
import org.upyog.Automation.config.WebDriverFactory;
import java.time.Duration;

/**
 * Automated test class for UPYOG Trade License Registration
 * This class handles the complete citizen-side trade license application workflow including:
 * - Citizen login with OTP verification
 * - Trade license application form filling
 * - Business details and property information
 * - Owner details and document uploads
 * - Application submission
 */
@Component
public class TradeLicenseCreate {

    private static final Logger logger = LoggerFactory.getLogger(TradeLicenseCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    /**
     * Main test method for trade license registration workflow
     * Runs automatically when Spring context is initialized
     */
    //@PostConstruct
    public void tradeLicenseReg() {
        tradeLicenceCitizenReg(ConfigReader.get("citizen.base.url"),
                               "Trade License",
                               ConfigReader.get("citizen.mobile.number"),
                               ConfigReader.get("test.otp"),
                               ConfigReader.get("test.city.name"));
    }

    public void tradeLicenceCitizenReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Trade license by Citizen");

        // Initialize WebDriver using WebDriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);
            
            // STEP 2: Navigate to Trade License Application
            navigateToTradeLicense(driver, wait, js);
            
            // STEP 3: Fill Business Details
            fillBusinessDetails(driver, wait, js);
            
            // STEP 4: Fill Trade Details
            fillTradeDetails(driver, wait, js);
            
            // STEP 5: Fill Property Details
            fillPropertyDetails(driver, wait, js, actions);
            
            // STEP 6: Fill Owner Details
            fillOwnerDetails(driver, wait, js, actions);
            
            // STEP 7: Fill Institutional Details
            fillInstitutionalDetails(driver, wait, js, actions);
            
            // STEP 8: Upload Documents
            uploadDocuments(driver, wait, js);
            
            // STEP 9: Submit Application
            submitApplication(driver, wait, js);
            
            logger.info("Trade License Registration completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation
            
        } catch (Exception e) {
            logger.info("Exception in Trade License Registration: " + e.getMessage());
            e.printStackTrace();
        }finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Handles citizen login process
     */
    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName) throws InterruptedException {
        driver.get(baseUrl);
        logger.info("Open the Citizen Login Portal");

        // Enter mobile number
        fillInput(wait, "mobileNumber", mobileNumber);

        // Accept terms and conditions checkbox
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='checkbox'].form-field")));
        if (!checkbox.isSelected()) {
            js.executeScript("arguments[0].click();", checkbox);
            Thread.sleep(1000);
        }

        // Click Next
        clickButton(wait, js, "//button[@type='submit']//header[text()='Next']/..");

        // Fill OTP
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.input-otp-wrap")));
        List<WebElement> otpInputs = driver.findElements(By.cssSelector("input.input-otp"));
        for (int i = 0; i < otp.length() && i < otpInputs.size(); i++) {
            otpInputs.get(i).sendKeys(String.valueOf(otp.charAt(i)));
        }

        // Submit OTP
        clickButton(wait, js, "//button[@type='submit']//header[text()='Next']/..");

        // Select city
        selectCity(driver, wait, js, cityName);

        // Continue to home page
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
        actions.moveToElement(continueBtn).click().perform();
    }

    /**
     * Navigates to Trade License application
     */
    private void navigateToTradeLicense(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Navigating to Trade License Application");
        
        // Click Trade License sidebar link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/tl-home']"))));

        // Click Apply for Trade License
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'CitizenHomeCard')]//a[text()='Apply for Trade License']"))));

        // Click Next on document requirements page
        WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span/button[contains(@class,'submit-bar')]//header[normalize-space()='Next']/..")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", nextBtn);
    }

    /**
     * Fills business details
     */
    private void fillBusinessDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Filling Business Details");
        
        // Business name
        fillInput(wait, "TradeName", "Institute");
        clickButtonByHeader(driver, wait, "Next");

        // Select Yes for business question
        selectRadioButtonByLabel(driver, "Yes");
        clickButtonByHeader(driver, wait, "Next");

        // Select Kutcha
        Thread.sleep(3000);
        selectRadioButtonByLabel(driver, "Kutcha");
        clickButtonByHeader(driver, wait, "Next");

        // Fill date
        WebElement dateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input.employee-card-input[type='date']")));
        dateInput.clear();
        dateInput.sendKeys("20-05-2025");
        clickButtonByHeader(driver, wait, "Next");
    }

    /**
     * Fills trade details
     */
    private void fillTradeDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Filling Trade Details");
        
        // Select Goods
        selectRadioButtonByLabel(driver, "Goods");
        Thread.sleep(5000);

        // Select first dropdown option
        selectDropdownOption(driver, wait, 0);
        
        // Select second dropdown option
        selectDropdownOption(driver, wait, 1);

        // Fill UOM value
        fillInput(wait, "UomValue", "6");
        clickButtonByHeader(driver, wait, "Next");

        // Select No for additional question
        selectRadioButtonByLabel(driver, "No");
        clickButtonByHeader(driver, wait, "Next");

        // Fill GST and other details
        fillInput(wait, "TradeGSTNumber", "10SEIJI3622O0Z4");
        fillInput(wait, "OperationalSqFtArea", "454");
        fillInput(wait, "NumberOfEmployees", "10");
        clickButtonByHeader(driver, wait, "Next");

        // Select No for another question
        selectRadioButtonByLabel(driver, "No");
        clickButtonByHeader(driver, wait, "Next");
    }

    /**
     * Fills property details
     */
    private void fillPropertyDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Property Details");
        
        // Select property type
        selectDropdownOption(driver, wait, 0);
        
        // Fill land area and construction area
        fillInput(wait, "totLandArea", "345");
        fillInput(wait, "totConstructionArea", "87");
        
        Thread.sleep(1000);
        
        // Select usage dropdown
        selectDropdownOption(driver, wait, 1);
        
        // Select city
        selectCityFromDropdown(driver, wait, js, 2, "Delhi");
        
        // Select locality
        selectDropdownOption(driver, wait, 3);
        
        // Fill address details
        fillInput(wait, "houseDoorNo", "445");
        fillInput(wait, "buildingColonyName", "chruch street");
        fillInput(wait, "landmarkName", "cirle point");
        
        // Select ownership type
        selectDropdownOptionByIndex(driver, wait, actions, 4, 3);
        
        Thread.sleep(1000);
        
        // Fill owner details
        fillInput(wait, "mobileNumber0", "9494848897");
        fillInput(wait, "name0", "atul");
        
        // Select gender
        selectRadioButtonByLabel(driver, "Male");
        
        // Fill father name
        fillInput(wait, "fatherOrHusbandName0", "father");
        
        // Select relationship
        selectRadioButtonByLabel(driver, "Husband");
        
        // Select category
        selectDropdownOptionByText(driver, wait, actions, 5, "BPL");
        
        // Fill address
        fillTextArea(wait, "address0", "nsdkfnsdfsf");
        
        // Submit and proceed
        clickSubmitButton(driver, wait, js);
        clickProceedButton(driver, wait, js);
        clickNextButton(driver, wait, js);
    }

    /**
     * Fills owner details
     */
    private void fillOwnerDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Owner Details");
        
        // Select ownership type
        selectRadioButtonByLabel(driver, "Institutional - Government");
        clickButtonByHeader(driver, wait, "Next");
    }

    /**
     * Fills institutional details
     */
    private void fillInstitutionalDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Institutional Details");
        
        // Fill institution name
        fillInput(wait, "institutionName", "kunal gaurav");
        
        // Select government type
        selectDropdownOptionByText(driver, wait, actions, 0, "Central Government");
        
        // Fill contact details
        fillInput(wait, "name", "sourabh");
        fillInput(wait, "designation", "officer");
        fillInput(wait, "mobilenumber", "8474857362");
        
        clickButtonByHeader(driver, wait, "Next");

        logger.info("for checkbox");
        // Select same address checkbox
        Thread.sleep(3000);
        WebElement checkbox = driver.findElement(By.cssSelector("input[type='checkbox'][value=\"Same as Trade's Address\"]"));
        if (!checkbox.isSelected()) {
            js.executeScript("arguments[0].scrollIntoView(true);", checkbox);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", checkbox);
            logger.info("Checkbox selected");
        }
        
        clickButtonByHeader(driver, wait, "Next");
    }

    /**
     * Uploads required documents
     */
    private void uploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Uploading Documents");
        
        String filePath = ConfigReader.get("tl.document.identity.proof");

        String filePathforimage = ConfigReader.get("tl.document.image");
        
        // Upload first two documents with next clicks
        uploadDocumentAndClickNext(driver, wait, filePath);
        uploadDocumentAndClickNext(driver, wait, filePath);
        
        // Upload third document
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tl-doc")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", fileInput);
        Thread.sleep(200);
        fileInput.sendKeys(getAbsolutePath(filePathforimage));
        logger.info("Third document uploaded");
        
        clickButtonByHeader(driver, wait, "Next");
    }

    /**
     * Submits the application
     */
    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Submitting Application");
        
        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit Application']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("Application submitted successfully");
    }

    // UTILITY METHODS

    /**
     * Utility method to fill input fields
     */
    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    /**
     * Utility method to fill textarea fields
     */
    private void fillTextArea(WebDriverWait wait, String fieldName, String value) {
        WebElement textarea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(fieldName)));
        textarea.clear();
        textarea.sendKeys(value);
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
     * Clicks button by header text
     */
    private void clickButtonByHeader(WebDriver driver, WebDriverWait wait, String headerText) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[contains(text(),'" + headerText + "')]]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        Thread.sleep(500);
    }

    /**
     * Selects radio button by label text
     */
    private void selectRadioButtonByLabel(WebDriver driver, String labelText) {
        try {
            // Debug: Print available radio buttons
            debugRadioButtons(driver);

            WebElement radio = null;

            // Try multiple selectors for radio button
            try {
                // Strategy 1: Standard structure
                radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/preceding-sibling::span/input"));
            } catch (Exception e1) {
                try {
                    // Strategy 2: Direct sibling
                    radio = driver.findElement(By.xpath("//label[contains(text(),'" + labelText + "')]/preceding-sibling::input"));
                } catch (Exception e2) {
                    try {
                        // Strategy 3: Parent div approach
                        radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/..//input[@type='radio']"));
                    } catch (Exception e3) {
                        try {
                            // Strategy 4: Following sibling
                            radio = driver.findElement(By.xpath("//label[text()='" + labelText + "']/following-sibling::input[@type='radio']"));
                        } catch (Exception e4) {
                            // Strategy 5: Generic radio with value
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

    /**
     * Debug method to print available radio buttons
     */
    private void debugRadioButtons(WebDriver driver) {
        try {
            logger.info("=== DEBUG: Available radio buttons ===");
            List<WebElement> radios = driver.findElements(By.xpath("//input[@type='radio']"));
            for (int i = 0; i < Math.min(radios.size(), 10); i++) {
                WebElement radio = radios.get(i);
                String value = radio.getAttribute("value");
                String name = radio.getAttribute("name");
                logger.info("Radio " + (i+1) + ": value='" + value + "', name='" + name + "'");
            }

            List<WebElement> labels = driver.findElements(By.tagName("label"));
            for (int i = 0; i < Math.min(labels.size(), 10); i++) {
                WebElement label = labels.get(i);
                String text = label.getText().trim();
                if (!text.isEmpty()) {
                    logger.info("Label " + (i+1) + ": '" + text + "'");
                }
            }
            logger.info("=== END DEBUG ===");
        } catch (Exception e) {
            logger.info("Debug failed: " + e.getMessage());
        }
    }

    /**
     * Selects city during login
     */
    private void selectCity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String cityName) throws InterruptedException {
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
        throw new RuntimeException("Failed to select " + cityName);
    }

    /**
     * Selects dropdown option by index
     */
    private void selectDropdownOption(WebDriver driver, WebDriverWait wait, int dropdownIndex) throws InterruptedException {
        List<WebElement> dropdownSvgs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div.select svg.cp")));
        
        WebElement dropdownSvg = dropdownSvgs.get(dropdownIndex);
        
        // Scroll into view
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", dropdownSvg);
        Thread.sleep(300);
        
        // Try regular click first, fallback to JS if intercepted
        try {
            dropdownSvg.click();
        } catch (Exception e) {
            // Use dispatchEvent instead of .click()
            ((JavascriptExecutor) driver).executeScript(
                "var evt = new MouseEvent('click', {bubbles: true, cancelable: true, view: window}); arguments[0].dispatchEvent(evt);", 
                dropdownSvg);
        }
        
        Thread.sleep(500);
        
        WebElement optionsContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));
        
        if (!options.isEmpty()) {
            try {
                options.get(0).click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript(
                    "var evt = new MouseEvent('click', {bubbles: true, cancelable: true, view: window}); arguments[0].dispatchEvent(evt);", 
                    options.get(0));
            }
        }
    }

    /**
     * Selects dropdown option by index with Actions
     */
    private void selectDropdownOptionByIndex(WebDriver driver, WebDriverWait wait, Actions actions, int dropdownIndex, int optionIndex) throws InterruptedException {
        List<WebElement> dropdownSvgs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div.select svg.cp")));
        
        WebElement ownerDropdownSvg = dropdownSvgs.get(dropdownIndex);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", ownerDropdownSvg);
        Thread.sleep(300);
        actions.moveToElement(ownerDropdownSvg).click().perform();
        
        WebElement optionsContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));
        
        if (options.size() > optionIndex) {
            options.get(optionIndex).click();
        }
    }

    /**
     * Selects dropdown option by text
     */
    private void selectDropdownOptionByText(WebDriver driver, WebDriverWait wait, Actions actions, int dropdownIndex, String optionText) throws InterruptedException {
        List<WebElement> dropdownDivs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div.select")));
        
        WebElement dropdownDiv = dropdownDivs.get(dropdownIndex);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", dropdownDiv);
        Thread.sleep(300);
        
        WebElement svgIcon = dropdownDiv.findElement(By.cssSelector("svg.cp"));
        actions.moveToElement(svgIcon).click().perform();
        
        WebElement optionsContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));
        
        for (WebElement option : options) {
            if (option.getText().trim().equalsIgnoreCase(optionText)) {
                option.click();
                break;
            }
        }
    }

    /**
     * Selects city from dropdown by text
     */
    private void selectCityFromDropdown(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, int dropdownIndex, String cityName) throws InterruptedException {
        List<WebElement> dropdownSvgs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div.select svg.cp")));
        
        WebElement cityDropdownSvg = dropdownSvgs.get(dropdownIndex);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", cityDropdownSvg);
        Thread.sleep(300);
        cityDropdownSvg.click();
        Thread.sleep(500);
        
        WebElement optionsContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));
        
        for (WebElement option : options) {
            if (option.getText().trim().equals(cityName)) {
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", option);
                Thread.sleep(200);
                js.executeScript("arguments[0].click();", option);
                logger.info("Selected " + cityName + " from dropdown");
                break;
            }
        }
    }

    /**
     * Uploads document and clicks next
     */
    private void uploadDocumentAndClickNext(WebDriver driver, WebDriverWait wait, String filePath) throws InterruptedException {
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tl-doc")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", fileInput);
        Thread.sleep(200);
        fileInput.sendKeys(getAbsolutePath(filePath));
        logger.info("Document uploaded: " + filePath);

        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@class='submit-bar ' and @type='submit'][.//header[text()='Next']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", nextButton);
        Thread.sleep(200);
        nextButton.click();
    }

    /**
     * Clicks submit button
     */
    private void clickSubmitButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        Thread.sleep(1000);
        
        // Try multiple selectors for submit button
        WebElement submitButton = null;
        
        try {
            submitButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("button.submit-bar[type='submit']")));
        } catch (Exception e1) {
            try {
                submitButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//button[contains(@class,'submit-bar') and @type='submit']")));
            } catch (Exception e2) {
                try {
                    submitButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//button[contains(@class,'submit-bar')]")));
                } catch (Exception e3) {
                    submitButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//button[@type='submit']")));
                }
            }
        }
        
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        
        try {
            submitButton.click();
        } catch (Exception e) {
            js.executeScript(
                "var evt = new MouseEvent('click', {bubbles: true, cancelable: true, view: window}); arguments[0].dispatchEvent(evt);", 
                submitButton);
        }
    }

    /**
     * Clicks proceed button
     */
    private void clickProceedButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement proceedButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("button.submit-bar[type='button']")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", proceedButton);
        Thread.sleep(200);
        proceedButton.click();
    }

    /**
     * Clicks next button
     */
    private void clickNextButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        WebElement nextButton = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button' and header='Next'] | //button[@class='submit-bar ' and @type='button'][.//header[text()='Next']]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextButton);
        Thread.sleep(200);
        nextButton.click();
    }

    /**
     * Converts relative path to absolute path for file uploads
     * Selenium requires absolute paths for sendKeys() on file inputs
     */
    private String getAbsolutePath(String relativePath) {
        File file = new File(relativePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + relativePath);
        }
        return file.getAbsolutePath();
    }
}