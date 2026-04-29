package org.upyog.Automation.Modules.PropertyTax;

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
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.upyog.Automation.config.WebDriverFactory;

@Component
public class PropertyTaxCreate {

    private static final Logger logger = LoggerFactory.getLogger(PropertyTaxCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct

    public void newPropertyReg() {
        newPropertyReg(ConfigReader.get("citizen.base.url"),
                "Property Tax",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
    }

    public void newPropertyReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Property Registration by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Property Tax Booking
            navigateToPropertyTax(driver, wait, js);

            // STEP 3: Property Details Page
            infoPage(driver, wait, js);

            // STEP 4: Type of Property
            selectTypeOfProperty(driver, wait, js);

            // STEP 5: Electricity Number
            fillElectricityNo(driver, wait, js);

            // STEP 6: Property Structure Details
            fillPropertyStructureDetails(driver, wait, js);

            // STEP 7: Unique ID
            fillUniqueID(driver, wait, js);

            // STEP 8: Area in Sq/Ft
            fillLandAreaDetails(driver, wait, js);

            // STEP 9: Number of Basement
            fillBasementDetails(driver, wait, js);

            // STEP 10: Number of Floors
            fillFloorDetails(driver, wait, js);

            // STEP 11:Ground Floor Details
            fillGroundFloorDetails(driver, wait, js);

            // STEP 12: Pin Property Details
            clickPinPropertyLocation(wait, js);

            // STEP 13: Pin code Details
            fillPincodeDetail(driver, wait, js);

            // STEP 14: Provide Property address Details
            fillPropertyAddressDetail(driver, wait, js);

            // STEP 15: Provide Property address Details 2
            fillPropertyAddressDetail2(driver, wait, js);

            // STEP 16: Landmark
            fillLandMarkDetail(driver, wait, js);

            // STEP 17: Upload Documents
            uploadDocuments(driver, wait, js);

            // STEP 18: Provide Ownership details
            provideOwnershipDetails(driver, wait, js);

            // STEP 19: Owner Details
            fillOwnerDetails(driver, wait, js);

            // STEP: 20 Special Owner category
            selectSpecialOwnerCategory(driver, wait, js);

            // STEP: 21 Owner Address
            fillOwnerAddress(driver, wait, js);

            // STEP: 22 Proof Of Identity
            selectProofOfIdentity(driver, wait, js);

            // STEP: 23 Submit Application
            submitApplication(driver, wait, js);

        } catch (Exception e) {
            logger.info("Exception in Property Registration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

            /*
             =====================================================================
             STEP 1: CITIZEN LOGIN
             =====================================================================
            */

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

            /*
             =====================================================================
             STEP 2: NAVIGATE TO PROPERTY TAX MODULE
             =====================================================================
            */

    private void navigateToPropertyTax(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Property Tax");

        // Sidebar Property Tax link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/pt-home']"))));

        Thread.sleep(2000);
        logger.info("Reached Property Tax home page");

        // "Property Tax" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/pt/property/new-application/info']"))));

        logger.info("Clicked Create Property link");
    }

            /*
             =====================================================================
             STEP 3: INFO PAGE DETAILS
             =====================================================================
            */

    private void infoPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Property Tax Info Page - Clicking Next");
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

            /*
             =====================================================================
             STEP 4: TYPE OF PROPERTY
             =====================================================================
            */

    private void selectTypeOfProperty(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Type of Property Selected");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Independent Building");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

            /*
             =====================================================================
             STEP 5: ELECTRICITY NUMBER
             =====================================================================
            */

    private void fillElectricityNo(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Electricity Number");

        WebElement electricityInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[contains(@placeholder,'electricity')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", electricityInput);
        Thread.sleep(200);

        electricityInput.clear();
        electricityInput.sendKeys("1234567890");

        // Single blur is enough for DIGIT
        electricityInput.sendKeys(org.openqa.selenium.Keys.TAB);

        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

            /*
             =====================================================================
             STEP 6: PROPERTY STRUCTURE DETAILS
             =====================================================================
            */

    private void fillPropertyStructureDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Property Structure Details");
        Thread.sleep(500);

        // -------------------------
        // Structure Type
        // -------------------------
        selectDropdownByIndex(driver, wait, js, 0, 0);
        logger.info("Selected Structure Type");

        Thread.sleep(1000);

        // -------------------------
        // Age of Property
        // -------------------------
        selectDropdownByIndex(driver, wait, js, 1, 0);
        logger.info("Selected Age of Property");

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

            /*
             =====================================================================
             STEP 7: UNIQUE ID
             =====================================================================
            */

    private void fillUniqueID(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Unique ID");

        WebElement UniqueIDInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[contains(@placeholder,'Enter a valid 15-digit alphanumeric characters UID')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", UniqueIDInput);
        Thread.sleep(1000);

        UniqueIDInput.clear();
        UniqueIDInput.sendKeys("ABCD1234567890E");

        // Single blur is enough for DIGIT
        UniqueIDInput.sendKeys(org.openqa.selenium.Keys.TAB);

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

            /*
            =====================================================================
            STEP 8: AREA (IN SQ/ FT)
            =====================================================================
            */

    private void fillLandAreaDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Land Area Details");

        WebElement areaInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input.employee-card-input")));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", areaInput);
        areaInput.click();
        areaInput.clear();

        String value = "1200";
        for (char c : value.toCharArray()) {
            areaInput.sendKeys(String.valueOf(c));
            Thread.sleep(120);
        }

        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", areaInput);
        Thread.sleep(500);

        clickNextButton(driver, wait, js);
        logger.info("Land Area submitted successfully");
    }

            /*
             =====================================================================
             STEP 9: NUMBER OF BASEMENTS
             =====================================================================
            */

    private void fillBasementDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Number of Basements Selected");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "No Basement");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

             /*
             =====================================================================
             STEP 10: NUMBER OF FLOORS
             =====================================================================
             */

    private void fillFloorDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Number of Floors Selected");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Ground Floor Only");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

            /*
             =====================================================================
             STEP 11: GROUND FLOOR DETAILS
             =====================================================================
            */

private void fillGroundFloorDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
                throws InterruptedException {
        logger.info("Ground Floor Details Selected");
        Thread.sleep(1000);

    // -------------------------
    // Unit Usage Type Occupancy
    // -------------------------
    selectDropdownByIndex(driver, wait, js, 0, 4);
    logger.info("Selected Unit Usage Type");

    Thread.sleep(500);

    // -------------------------
    // Occupancy
    // -------------------------

    selectDropdownByIndex(driver, wait, js, 1, 1);
    logger.info("Selected Unit Usage Type");

    Thread.sleep(500);

    // -------------------------
    // Built-up Area
    // -------------------------

    WebElement areaInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("input.employee-card-input")));

    js.executeScript("arguments[0].scrollIntoView({block:'center'});", areaInput);
    areaInput.click();
    areaInput.clear();

    String value = "1100";
    for (char c : value.toCharArray()) {
        areaInput.sendKeys(String.valueOf(c));
        Thread.sleep(120);
    }

    js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", areaInput);
    Thread.sleep(500);

    clickNextButton(driver, wait, js);
    logger.info("Land Area submitted successfully");

    }

            /*
             =====================================================================
             STEP 12: PIN PROPERTY DETAILS
             =====================================================================
            */

    private void clickPinPropertyLocation(WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking Skip and Continue");

        WebElement skipLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[contains(@class,'card-link') and normalize-space()='Skip and continue']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", skipLink);
        Thread.sleep(300);

        js.executeScript("arguments[0].click();", skipLink);

        Thread.sleep(1000);
        logger.info("Skip and Continue clicked successfully");
    }

            /*
             =====================================================================
             STEP 13: PIN PROPERTY DETAILS
             =====================================================================
            */

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

            /*
             =====================================================================
             STEP 14: PROVIDE PROPERTY ADDRESS
             =====================================================================
            */

    private void fillPropertyAddressDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {
        logger.info("Selecting City and Locality (Index Based)");
        Thread.sleep(1000);

        // GET ALL RADIOS (CITY LEVEL)


        List<WebElement> cityRadios = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//input[@type='radio']")
        ));


        // City safe method for all environment
        clickRadioByIndex(cityRadios, 0, js);
        Thread.sleep(1000);

        // Locality safe method for all environment
        List<WebElement> localityRadios = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//input[@type='radio']")
        ));


        // Locality
        clickRadioByIndex(localityRadios, 1, js);
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

            /*
             =====================================================================
             STEP 15: PROVIDE PROPERTY ADDRESS 2
             =====================================================================
            */

    private void fillPropertyAddressDetail2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Property Address Details");

        // Get ALL text inputs on address card
        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        // CONFIRMED ORDER
        // 0 → Street Name
        // 1 → House No

        WebElement streetNameInput = inputs.get(0);
        WebElement houseNoInput = inputs.get(1);

        // -------- STREET NAME --------
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", streetNameInput);
        streetNameInput.click();
        streetNameInput.clear();
        streetNameInput.sendKeys("Test Street");

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", streetNameInput);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", streetNameInput);

        Thread.sleep(1000);

        // -------- HOUSE NO --------
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", houseNoInput);
        houseNoInput.click();
        houseNoInput.clear();
        houseNoInput.sendKeys("89");

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", houseNoInput);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", houseNoInput);

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
    }


            /*
             =====================================================================
             STEP 16: LANDMARK
             =====================================================================
            */

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

            /*
             =====================================================================
             STEP 17: UPLOAD DOCUMENT
             =====================================================================
            */

    private void selectProofOfIdentity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Uploading Documents");
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 0,0 );
        Thread.sleep(1000);
        logger.info("Selecting Electricity Bill");

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.propertyidentity.proof"));
        Thread.sleep(1000);
        logger.info("Finished Upload Documents step");

        clickNextButton(driver, wait, js);
    }
             /*
             =====================================================================
             STEP 18: PROVIDE OWNERSHIP DETAILS
             =====================================================================
             */

    private void provideOwnershipDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Providing the Ownership Details");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Single Owner");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

        }

             /*
             =====================================================================
             STEP 19: OWNER DETAILS
             =====================================================================
             */

    private void fillOwnerDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Owner Details");

        List<WebElement> inputs = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.cssSelector("input.employee-card-input")
                )
        );

        if (inputs.size() >= 4) {
            // Fill inputs one by one
            fillInputField(js, inputs.get(0), "Test Owner");
            fillInputField(js, inputs.get(1), "9999999999");
            fillInputField(js, inputs.get(2), "Test Guardian");
            fillInputField(js, inputs.get(3), "test@gmail.com");
        }

        Thread.sleep(1000);

        // Select radio buttons using existing method
        try {
            selectRadioButtonByLabel(driver, "Male");
            Thread.sleep(1000);
            selectRadioButtonByLabel(driver, "Father");
            Thread.sleep(1000);
        } catch (Exception e) {
            logger.info("Radio button selection failed: " + e.getMessage());
        }

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

              /*
              =====================================================================
              STEP 20: SPECIAL OWNER CATEGORY
              =====================================================================
              */


    private void selectSpecialOwnerCategory(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {

        logger.info("Selecting Special Owner Category");
        Thread.sleep(1000);

        selectRadioButtonByLabel(driver, "Not Applicable");
        Thread.sleep(3000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

              /*
              =====================================================================
              STEP 21: FILL OWNER DETAILS
              =====================================================================
              */


    private void fillOwnerAddress(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {

        logger.info("Filling Owner Address");
        Thread.sleep(1000);

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

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

              /*
              =====================================================================
              STEP 22: PROOF OF IDENTITY
              =====================================================================
              */


    private void uploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Uploading Documents");
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js, 0,0 );
        Thread.sleep(500);
        logger.info("Selecting Electricity Bill");

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.propertyaddress.proof"));
        Thread.sleep(3000);
        logger.info("Finished Upload Documents step");

        clickNextButton(driver, wait, js);
    }

              /*
              =====================================================================
              STEP 23: SUBMIT APPLICATION
              =====================================================================
              */


    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Submitting Property Tax Application - Summary Page");
        Thread.sleep(3000);

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
        logger.info("Property tax application: Submit clicked");

        try {
            // Wait for Proceed button in popup
            WebElement proceedBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(.,'Proceed') or contains(.,'PROCEED')]")));

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", proceedBtn);
            Thread.sleep(300);
            js.executeScript("arguments[0].click();", proceedBtn);

            logger.info("Clicked Proceed on duplicate popup");
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.info("No duplicate popup found - continuing");
            Thread.sleep(5000);
        }
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
}