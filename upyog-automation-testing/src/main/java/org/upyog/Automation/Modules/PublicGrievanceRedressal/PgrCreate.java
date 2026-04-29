package org.upyog.Automation.Modules.PublicGrievanceRedressal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.upyog.Automation.Utils.ConfigReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.upyog.Automation.config.WebDriverFactory;
import java.io.File;


@Component
public class PgrCreate {

    private static final Logger logger = LoggerFactory.getLogger(PgrCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    public void pgrReg() {
        pgrReg(ConfigReader.get("citizen.base.url"),
                "Pgr",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"));
}

    public void pgrReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Public Grievance Redressal by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Property Tax Booking
            navigateToPgr(driver, wait, js);

            // STEP 3: File New Complaint
            fileNewComplaint(driver, wait, js);

            // STEP 4: Pin Complaint Location
            pinComplaintLocation(driver, wait, js);

            // STEP 5: Location Pin-code
            fillPincodeDetail(driver, wait, js);

            //STEP 6: Provide Complaint address
            selectCityLocation(driver, wait, js);

            // STEP 7: Landmark
            fillLandMarkDetail(driver, wait, js);

            // STEP 8: Upload Complaint Photo
            uploadComplaintPhoto(driver, wait, js);

            // STEP 9: Provide Additional Details
            fillAdditionalDetail(driver, wait, js);


        } catch (Exception e) {
            logger.info("Exception in PGR Registration: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
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
             STEP 2: NAVIGATE TO PUBLIC GRIEVANCE REDRESSAL MODULE
             =====================================================================
            */

private void navigateToPgr(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException {

    logger.info("Navigating to Public Grievance Redressal");

    // Sidebar Public Grievance Redressal link
    js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//a[@href='/upyog-ui/citizen/pgr-home']"))));

    Thread.sleep(2000);
    logger.info("Reached Public Grievance Redressal home page");

    // "Public Grievance Redressal" link
    js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//a[@href='/upyog-ui/citizen/pgr/create-complaint/complaint-type']"))));

    logger.info("Clicked File a Complaint link");
}

             /*
             =====================================================================
             STEP 3: FILE NEW COMPLAINT
             =====================================================================
            */

    private void fileNewComplaint(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
        throws InterruptedException{
        logger.info("Filing New Grievance Complaint");
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js, 0,1);
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js, 1,0);
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js, 2,1);
        Thread.sleep(500);

        clickNextBottomRight(driver, wait, js);
                Thread.sleep(500);

    }

             /*
             =====================================================================
             STEP 4: PIN COMPLAINT LOCATION
             =====================================================================
             */

    private void pinComplaintLocation(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking Skip and Continue");

        WebElement skipContainer = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//span[normalize-space()='Skip and Continue']/parent::*")
                )
        );

        // Bring above overlays
        js.executeScript(
                "arguments[0].style.zIndex='9999';" +
                        "arguments[0].style.position='relative';",
                skipContainer
        );

        Thread.sleep(300);

        // JS click (MANDATORY)
        js.executeScript("arguments[0].click();", skipContainer);

        logger.info("Skip and Continue clicked");
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }


             /*
             =====================================================================
             STEP 5: PIN-CODE DETAILS
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
        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

             /*
             =====================================================================
             STEP 6: PROVIDE COMPLAINT ADDRESS
             =====================================================================
             */

    private void selectCityLocation(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
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

             /*
             =====================================================================
             STEP 7: LANDMARK
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
        landmarkTextarea.sendKeys("Park is dirty, needs to be clean soon");
        Thread.sleep(1000);

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur',{bubbles:true}));", landmarkTextarea);

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);

        logger.info("Additional Detail submitted successfully");
    }


             /*
             =====================================================================
             STEP 8: UPLOAD COMPLAINT PHOTO
             =====================================================================
             */

    private void uploadComplaintPhoto(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Uploading complaint photo");

        String photoPath = ConfigReader.get("document.complaint.proof");
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

             /*
             =====================================================================
             STEP 9: PROVIDE ADDITIONAL DETAILS
             =====================================================================
             */

    private void fillAdditionalDetail(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Additional Details");

        // textarea (NOT input)
        WebElement AdditionalDetailsTextarea = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("textarea.card-textarea")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", AdditionalDetailsTextarea);
        Thread.sleep(300);

        AdditionalDetailsTextarea.click();
        AdditionalDetailsTextarea.clear();
        AdditionalDetailsTextarea.sendKeys("The park is dirty and needs to be cleaned soon.");
        Thread.sleep(1000);

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", AdditionalDetailsTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", AdditionalDetailsTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur',{bubbles:true}));", AdditionalDetailsTextarea);

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);

        logger.info("Landmark submitted successfully");
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
    Thread.sleep(1000);

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

    Thread.sleep(500);
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

    private void clickNextBottomRight(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking Next (bottom right)");

        WebElement nextBtn = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//button[contains(@class,'submit-bar')]")
                )
        );

        // Force-enable if disabled
        js.executeScript("arguments[0].removeAttribute('disabled');", nextBtn);

        // Bring it above overlays
        js.executeScript(
                "arguments[0].style.zIndex='9999';" +
                        "arguments[0].style.position='relative';",
                nextBtn
        );

        Thread.sleep(300);

        // JS click (ONLY reliable way)
        js.executeScript("arguments[0].click();", nextBtn);

        logger.info("Clicked Next");
        Thread.sleep(800);
    }


}

