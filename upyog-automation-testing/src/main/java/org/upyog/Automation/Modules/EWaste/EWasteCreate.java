package org.upyog.Automation.Modules.EWaste;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.stereotype.Component;

import org.upyog.Automation.engine.TestEngine;

@Component
public class EWasteCreate {

    public void eWasteReg(
            WebDriver driver,
            WebDriverWait wait,
            JavascriptExecutor js) {

        try {

            TestEngine engine =
                    new TestEngine(
                            driver,
                            "src/main/resources/dev.properties"
                    );

            engine.executeModule(
                    "src/main/resources/test-config/ewaste/ewaste_citizen_module.json"
            );

        }
        catch(Exception e) {

            throw new RuntimeException(
                    "EWaste Citizen Flow Failed",
                    e
            );
        }
    }
}
//package org.upyog.Automation.Modules.EWaste;
//
//import org.openqa.selenium.*;
//import org.openqa.selenium.interactions.Actions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.upyog.Automation.Utils.ConfigReader;
//import org.upyog.Automation.config.WebDriverFactory;
//import org.upyog.Automation.Utils.TestDataUtil;
//import org.upyog.Automation.Utils.CommonActions;
//
//import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
//
//@Component
//public class EWasteCreate {
//
//    private static final Logger logger = LoggerFactory.getLogger(EWasteCreate.class);
//
//    @Autowired
//    private WebDriverFactory webDriverFactory;
//
//    //@PostConstruct2
//
//    public void eWasteReg(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
//        logger.info("EWaste Management Booking");
//
////        WebDriver driver = webDriverFactory.createDriver();
////        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
////        JavascriptExecutor js = (JavascriptExecutor) driver;
////        Actions actions = new Actions(driver);
//
//        try {
//            // STEP 1: Citizen Login
////            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);
//
//            // STEP 2: Navigate to EWaste
//            navigateToEWaste(driver, wait, js);
//
//            // STEP 3: Selection of Product
//            searchProduct(driver, wait, js);
//
//            //STEP 4: Upload Image
//            uploadImage(driver, wait, js);
//
//            //STEP 5: Applicant Details
//            fillApplicantDetails(driver, wait, js);
//
//            //STEP 6: Address Details
//            addressPincode(driver, wait, js);
//
//            //STEP 7: Select City
//            selectCity(driver, wait, js);
//
//            //STEP 8: Address Details
//            addressDetails(driver, wait, js);
//
//            //STEP 9: Summary Page
//            submitApplication(driver, wait, js);
//            logger.info("Advertisement Booking completed successfully!");
//            Thread.sleep(100000);
//
//        } catch (Exception e) {
//
//            logger.error(
//                    "EWaste failed at current step", e
//            );
//            throw new RuntimeException(e);
//        }
////        finally {
////            if (driver != null) {
////                driver.quit();
////            }
////        }
//    }
//
//    // =====================================================================
//    // STEP 1: CITIZEN LOGIN
//    // =====================================================================
//
////    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
////            throws InterruptedException {
////
////        driver.get(baseUrl);
////        logger.info("Open the Citizen Login Portal");
////
////        // Mobile number
////        CommonActions.fillInput(wait, "mobileNumber", TestDataUtil.getMobileNo());
////
////        // Accept terms checkbox
////        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
////                By.cssSelector("input[type='checkbox'].form-field")));
////        if (!checkbox.isSelected()) {
////            js.executeScript("arguments[0].click();", checkbox);
////            Thread.sleep(1000);
////        }
////
////        // Next
////        CommonActions.clickButtonByText(driver, wait, js, "Next");
////
////        // OTP
////        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.input-otp-wrap")));
////        List<WebElement> otpInputs = driver.findElements(By.cssSelector("input.input-otp"));
////        for (int i = 0; i < otp.length() && i < otpInputs.size(); i++) {
////            otpInputs.get(i).sendKeys(String.valueOf(otp.charAt(i)));
////        }
////
////        // Submit OTP
////        CommonActions.clickButtonByText(driver, wait, js, "Next");
////
////        // Select city`1
////        selectCity(driver, wait, js, cityName);
////
////        // Continue
////        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
////                By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
////        js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
////        actions.moveToElement(continueBtn).click().perform();
////    }
//
//    // =====================================================================
//    // STEP 2: NAVIGATE TO E-WASTE MODULE
//    // =====================================================================
//
//    private void navigateToEWaste(WebDriver driver,
//                                  WebDriverWait wait,
//                                  JavascriptExecutor js) {
//
//        logger.info("Navigating to E-WASTE Booking");
//
//        // Sidebar click
//        WebElement ewasteLink = wait.until(
//                ExpectedConditions.elementToBeClickable(
//                        By.xpath("//a[@href='" +
//                                ConfigReader.get("module.ewaste.url") +
//                                "']")
//                )
//        );
//
//        js.executeScript("arguments[0].click();", ewasteLink);
//        logger.info("Clicked EWaste sidebar");
//
//        //WAIT FOR REAL NEXT ELEMENT
//        wait.until(ExpectedConditions.urlContains("ew-home"));
//
//        WebElement createRequest = wait.until(
//                ExpectedConditions.elementToBeClickable(
//                        By.xpath("//*[contains(text(),'Create Ewaste')]")
//                )
//        );
//
//        js.executeScript(
//                "arguments[0].scrollIntoView({block:'center'});",
//                createRequest
//        );
//
//        js.executeScript("arguments[0].click();", createRequest);
//
//        logger.info("Clicked Create Ewaste Request");
//
//        // Optional: next page wait
//        wait.until(
//                ExpectedConditions.visibilityOfElementLocated(
//                        By.name("productQuantity")
//                )
//        );
//    }
//
//    // =====================================================================
//    // STEP 3: SELECTION OF PRODUCT
//    // =====================================================================
//
//    private void searchProduct(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
//
//        logger.info("Search Product Page");
//
//        try {
//
//            // Read dropdown config
//            int dropdownIndex = Integer.parseInt(ConfigReader.get("ewaste.product.dropdown.index"));
//
//            int optionIndex = Integer.parseInt(ConfigReader.get("ewaste.product.option.index"));
//
//            // Select Product
//            CommonActions.selectDropdownByIndex(driver, wait, js, dropdownIndex, optionIndex);
//            logger.info("Product selected");
//
//        } catch (Exception e) {
//            logger.info("Product selection failed: " + e.getMessage());
//        }
//
//        // Quantity field
//        WebElement quantity = wait.until(
//                ExpectedConditions.elementToBeClickable(
//                        By.name(ConfigReader.get("ewaste.input.product.quantity.name"))));
//        quantity.click();
//
//        // Clear existing value
//        quantity.sendKeys(Keys.chord(Keys.CONTROL, "a"));
//        quantity.sendKeys(Keys.BACK_SPACE);
//
//        // Enter quantity
//        quantity.sendKeys(
//                TestDataUtil.getProductQuantity()
//        );
//
//        logger.info("Entered product quantity");
//
//        // Add Product button
//        WebElement addProduct = wait.until(
//                ExpectedConditions.elementToBeClickable(
//                        By.xpath(ConfigReader.get("ewaste.button.addProduct.xpath"))));
//
//        js.executeScript(
//                "arguments[0].click();",
//                addProduct
//        );
//
//        logger.info("Clicked Add Product");
//
//        // Next button
//        CommonActions.clickButton(
//                wait,
//                js,
//                ConfigReader.get("ewaste.button.next.xpath"));
//
//        logger.info("Clicked Next");
//
//    }
//
//    // =====================================================================
//    // STEP 4: UPLOAD IMAGE
//    // =====================================================================
//
//    private void uploadImage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//        throws InterruptedException {
//        logger.info( "Selecting the image");
//
//        uploadFile(driver, wait, js, 0, ConfigReader.get("document.EwasteImage.proof"));
//        Thread.sleep(3000);
//        logger.info("Finished Uploading Image step");
//
//        CommonActions.clickButton(wait, js,
//                "//button[contains(@class,'submit-bar') and .//header[text()='Next']]");
//    }
//
//    // =====================================================================
//    // STEP 5: APPLICANT DETAILS
//    // =====================================================================
//
//    private void fillApplicantDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//            throws InterruptedException {
//
//        logger.info("Filling Applicant Details");
//
//        CommonActions.fillInput(wait, "applicantName", TestDataUtil.getApplicantName());
//        CommonActions.fillInput(wait, "emailId", TestDataUtil.getApplicantEmail());
//
//        Thread.sleep(500);
//        CommonActions.clickButton(wait, js,
//                "//button[contains(@class,'submit-bar') and .//header[text()='Next']]");
//    }
//
//    // =====================================================================
//    // STEP 6: PIN-CODE DETAILS
//    // =====================================================================
//
//    private void addressPincode(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//        throws InterruptedException{
//
//        logger.info("Entering the Address Pincode");
//        Thread.sleep(500);
//        CommonActions.fillInput(wait, "pincode", TestDataUtil.getPincode());
//        Thread.sleep(1000);
//
//        CommonActions.clickButton(wait, js,
//                "//button[contains(@class,'submit-bar') and .//header[text()='Next']]");
//    }
//
//    // =====================================================================
//    // STEP 7: SELECT CITY
//    // =====================================================================
//
//    private void selectCity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//            throws InterruptedException {
//
//        logger.info("Selecting City and Locality");
//        Thread.sleep(1000);
//
//        String city = ConfigReader.get("city.name");
//        String locality = ConfigReader.get("locality.name");
//
//        System.out.println("CITY = " + city);
//        System.out.println("LOCALITY = " + locality);
//
//        selectRadioButtonByLabel(driver, city);
//        selectRadioButtonByLabel(driver, locality);
//
//        CommonActions.clickButton(wait, js,
//                "//button[contains(@class,'submit-bar') and .//header[text()='Next']]");
//        Thread.sleep(1000);
//    }
//
//    // =====================================================================
//    // STEP 8: ADDRESS DETAILS
//    // =====================================================================
//
//    private void addressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//            throws InterruptedException{
//
//        logger.info("Entering the Address Details");
//        Thread.sleep(500);
//
//        CommonActions.fillInput(wait, "street", TestDataUtil.getStreet());
//        Thread.sleep(500);
//
//        //fillInput(wait, "doorNo", "201");
//        CommonActions.fillInput(wait, "doorNo", TestDataUtil.getDoorNo());
//        Thread.sleep(500);
//
//        //fillInput(wait, "buildingName", "Jagbir Bhawan");
//        CommonActions.fillInput(wait, "buildingName", TestDataUtil.getBuilding());
//        Thread.sleep(500);
//
//        //fillInput(wait, "addressLine1", "test address 1");
//        CommonActions.fillInput(wait, "addressLine1", TestDataUtil.getAddressLine1());
//        Thread.sleep(500);
//
//        //fillInput(wait, "addressLine2", "test address 2");
//        CommonActions.fillInput(wait, "addressLine2", TestDataUtil.getAddressLine2());
//        Thread.sleep(500);
//
//        //fillInput(wait, "landmark", "test landmark");
//        CommonActions.fillInput(wait, "landmark", TestDataUtil.getLandmark());
//        Thread.sleep(500);
//
//
//        CommonActions.clickButton(wait, js,
//                "//button[contains(@class,'submit-bar') and .//header[text()='Next']]");
//    }
//
//    // =====================================================================
//    // STEP 9: SUBMIT APPLICATION
//    // =====================================================================
//
//    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//            throws InterruptedException {
//
//        logger.info("Submitting Ewaste Application - Summary Page");
//
//        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
//        if (!checkboxes.isEmpty()) {
//            WebElement lastCheckbox = checkboxes.get(checkboxes.size() - 1);
//            try {
//                if (!lastCheckbox.isSelected()) {
//                    js.executeScript("arguments[0].scrollIntoView(true);", lastCheckbox);
//                    Thread.sleep(300);
//                    js.executeScript("arguments[0].click();", lastCheckbox);
//                    logger.info("Checked declaration checkbox");
//                }
//            } catch (Exception ex) {
//                logger.info("Could not click declaration checkbox: " + ex.getMessage());
//            }
//        }
//
//        CommonActions.clickButton(wait, js,
//                "//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit']]");
//
//        logger.info("EWaste application: Submit clicked");
//
//        wait.until(
//                ExpectedConditions.visibilityOfElementLocated(
//                        By.xpath("//*[contains(text(),'Application')]")));
//    }
//
//    // =====================================================================
//    // UTILITY METHODS
//    // =====================================================================
//
////    private void fillInput(WebDriverWait wait, String fieldName, String value) {
////        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
////        input.clear();
////        input.sendKeys(value);
////    }
//
//    // optional field – do not fail if missing
////    private void fillOptionalInput(WebDriver driver, WebDriverWait wait, String fieldName, String value) {
////        try {
////            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
////            if (input.isDisplayed() && input.isEnabled()) {
////                input.clear();
////                input.sendKeys(value);
////                logger.info("Filled optional field: " + fieldName);
////            } else {
////                logger.info("Optional field " + fieldName + " not interactable, skipping");
////            }
////        } catch (Exception e) {
////            logger.info("Optional field " + fieldName + " not found, skipping");
////        }
////    }
//
////    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
////        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
////        js.executeScript("arguments[0].scrollIntoView(true);", button);
////        Thread.sleep(300);
////        button.click();
////    }
//
////    private void clickButtonByHeader(WebDriver driver, WebDriverWait wait, String headerText)
////            throws InterruptedException {
////
////        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
////                By.xpath("//button[contains(@class, 'submit-bar') and .//header[contains(text(),'" + headerText + "')]]")));
////        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
////        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
////        Thread.sleep(500);
////    }
//
//    private void clickNextButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
//            throws InterruptedException {
//
//        WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
//                By.xpath("//button[contains(@class,'submit-bar') and .//header[text()='Next']]")));
//        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextButton);
//        Thread.sleep(200);
//        nextButton.click();
//        logger.info("Clicked Next");
//    }
//
//    private void selectCity(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String cityName)
//            throws InterruptedException {
//
//        wait.until(ExpectedConditions.visibilityOfElementLocated(
//                By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper")));
//
//        List<WebElement> cityOptions = driver.findElements(
//                By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper div"));
//
//        for (WebElement option : cityOptions) {
//            WebElement label = option.findElement(By.tagName("label"));
//            if (label.getText().trim().equals(cityName)) {
//                WebElement radioInput = option.findElement(By.cssSelector("input[type='radio']"));
//                if (!radioInput.isSelected()) {
//                    js.executeScript("arguments[0].click();", radioInput);
//                    Thread.sleep(1000);
//                }
//                return;
//            }
//        }
//        throw new RuntimeException("Failed to select city: " + cityName);
//    }
//
//    private void selectRadioButtonByLabel(WebDriver driver, String text) {
//        try {
//            WebElement element = driver.findElement(
//                    By.xpath("//*[normalize-space()='" + text + "']")
//            );
//
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to select: " + text, e);
//        }
//    }
//
//    private void selectDropdownOption(WebDriver driver,
//                                      WebDriverWait wait,
//                                      JavascriptExecutor js,
//                                      int dropdownIndex) throws InterruptedException {
//
//        // get all dropdown arrow svgs on the page
//        java.util.List<WebElement> dropdownSvgs = wait.until(
//                ExpectedConditions.visibilityOfAllElementsLocatedBy(
//                        By.cssSelector("div.select svg.cp"))
//        );
//
//        if (dropdownIndex < 0 || dropdownIndex >= dropdownSvgs.size()) {
//            logger.info("Dropdown index " + dropdownIndex + " not found. Total: " + dropdownSvgs.size());
//            return;
//        }
//
//        WebElement svg = dropdownSvgs.get(dropdownIndex);
//
//        // scroll into view
//        js.executeScript("arguments[0].scrollIntoView({block:'center'});", svg);
//        Thread.sleep(200);
//
//        // 1st try: normal Selenium click
//        try {
//            svg.click();
//        } catch (ElementClickInterceptedException e) {
//            logger.info("Normal click intercepted on dropdown svg, using JS dispatch. Reason: " + e.getMessage());
//
//            // 2nd try: dispatch a click event manually (no arguments[0].click())
//            js.executeScript(
//                    "var ev = document.createEvent('MouseEvents');" +
//                            "ev.initEvent('click', true, true);" +
//                            "arguments[0].dispatchEvent(ev);",
//                    svg
//            );
//        }
//
//        // wait for options panel
//        WebElement optionsContainer = wait.until(
//                ExpectedConditions.visibilityOfElementLocated(
//                        By.cssSelector("div.options-card"))
//        );
//
//        java.util.List<WebElement> options = optionsContainer.findElements(
//                By.cssSelector("div.profile-dropdown--item")
//        );
//
//        if (!options.isEmpty()) {
//            WebElement firstOption = options.get(0);
//            js.executeScript("arguments[0].scrollIntoView({block:'center'});", firstOption);
//            Thread.sleep(150);
//
//            try {
//                firstOption.click();
//            } catch (ElementClickInterceptedException e) {
//                js.executeScript(
//                        "var ev = document.createEvent('MouseEvents');" +
//                                "ev.initEvent('click', true, true);" +
//                                "arguments[0].dispatchEvent(ev);",
//                        firstOption
//                );
//            }
//        } else {
//            logger.info("No options found in dropdown for index " + dropdownIndex);
//        }
//    }
//
//    /**
//     * For document page: we already have the list of svg dropdown icons.
//     */
//    private void selectDropdownOptionByIndex(WebDriver driver,
//                                             WebDriverWait wait,
//                                             JavascriptExecutor js,
//                                             List<WebElement> dropdownSvgs,
//                                             int dropdownIndex,
//                                             int optionIndex) throws InterruptedException {
//
//        if (dropdownIndex >= dropdownSvgs.size()) {
//            logger.info("Dropdown index " + dropdownIndex + " out of range");
//            return;
//        }
//
//        WebElement svg = dropdownSvgs.get(dropdownIndex);
//        js.executeScript("arguments[0].scrollIntoView({block:'center'});", svg);
//        Thread.sleep(200);
//
//        js.executeScript("arguments[0].click();", svg);
//
//        WebElement optionsContainer = wait.until(
//                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card"))
//        );
//
//        List<WebElement> options = optionsContainer.findElements(By.cssSelector("div.profile-dropdown--item"));
//
//        if (!options.isEmpty() && optionIndex < options.size()) {
//            js.executeScript("arguments[0].click();", options.get(optionIndex));
//        }
//    }
//
//
//    private void uploadFileToInput(JavascriptExecutor js, List<WebElement> fileInputs, int index, String filePath)
//            throws InterruptedException {
//
//        if (index >= fileInputs.size()) {
//            logger.info("⚠ No file input at index " + index + " for " + filePath);
//            return;
//        }
//
//        java.io.File f = new java.io.File(filePath);
//        logger.info("Attempting upload from: " + f.getAbsolutePath() + "  exists? " + f.exists());
//
//        if (!f.exists()) {
//            logger.info("⚠ File does NOT exist on disk. Skipping this input.");
//            return;
//        }
//
//        WebElement input = fileInputs.get(index);
//
//        // Make sure Selenium can interact (visibility & scroll)
//        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
//        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", input);
//        Thread.sleep(300);
//
//        input.sendKeys(f.getAbsolutePath());
//        logger.info(" Uploaded document into input index " + index);
//    }
//
//
//    private void uploadFile(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
//                            int index, String filePath) throws InterruptedException {
//
//        // All file inputs on the page – match your screenshot (hidden absolute-position input)
//        List<WebElement> fileInputs = wait.until(
//                ExpectedConditions.presenceOfAllElementsLocatedBy(
//                        By.cssSelector("input[type='file'].input-mirror-selector-button"))
//        );
//
//        if (index >= fileInputs.size()) {
//            logger.info("File input index " + index + " not found for path: " + filePath);
//            return;
//        }
//
//        WebElement fileInput = fileInputs.get(index);
//
//        // Make sure Selenium can interact with the hidden input
//        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fileInput);
//        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", fileInput);
//        Thread.sleep(300);
//
//        fileInput.sendKeys(filePath);
//        logger.info("Uploaded file at index " + index + ": " + filePath);
//        Thread.sleep(500);
//    }
//
//}