package org.upyog.Automation.Modules.Asset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.config.WebDriverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;

@Component
public class AssetEmp {

    private static final Logger logger = LoggerFactory.getLogger(AssetEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void assetEmpReg() {
        assetInboxEmp(ConfigReader.get("employee.base.url"),
                ConfigReader.get("ast.login.username"),
                ConfigReader.get("ast.login.password"),
                ConfigReader.get("ast.application.number"));
    }
    public void assetInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Trade License Employee Workflow");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to Register New Asset
            clickRegisterAsset(driver, wait, js);

            // STEP 3: Fill Top Section
            fillForm(driver, wait, js);

            // STEP 4: Submit Application
            submitApplication(driver, wait, js);





        } catch (Exception e) {
            logger.info("Exception in Asset Management Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // =====================================================================
    // STEP 1: EMPLOYEE LOGIN
    // =====================================================================

    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        logger.info("Open the Employee Login Portal");

        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        logger.info("Filled username and password");

        selectCityDropdown(driver, wait, actions);
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");
    }

    // =====================================================================
    // STEP 2: REGISTER ASSET
    // =====================================================================

    private void clickRegisterAsset(WebDriver driver,
                                       WebDriverWait wait,
                                       JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Clicking Register Asset under Asset MCD...");

        By registerAsset = By.xpath(
                "//span[text()='Asset MCD']" +
                        "/ancestor::div[contains(@class,'employeeCustomCard')]" +
                        "//div[contains(@class,'employee-card-banner')]" +
                        "//*[normalize-space()='Register Asset']"
        );

        WebElement element = wait.until(
                ExpectedConditions.presenceOfElementLocated(registerAsset)
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", element);

        logger.info("Clicked Register Asset under Asset MCD");
    }


    // =====================================================================
// STEP 3: FILL FORM
// =====================================================================

    private void fillForm(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Start filling Asset Details form");

        // TOP SECTION
        selectDropdownByIndex(driver, wait, js, 0, 0); // Financial Year
        Thread.sleep(1000);
        selectDropdownByIndex(driver, wait, js, 1, 34); // Department
        Thread.sleep(1000);

        // GENERAL DETAILS
        selectDropdownByIndex(driver, wait, js, 2, 4); // Parent Category
        Thread.sleep(1000);
        selectDropdownByIndex(driver, wait, js, 3, 0); // Category
        Thread.sleep(1000);

        fillInputByIndex(driver, js, 4, "MacBook");
        Thread.sleep(1000);

// verify asset id
        WebElement assetIdField = driver.findElement(By.xpath("//input[@name='assetId']"));
        String assetId = assetIdField.getAttribute("value");
        logger.info("Generated Asset ID: " + assetId);

// next dropdown
        selectDropdownByIndex(driver, wait, js, 4, 0); // Acquisition Mode
        Thread.sleep(1000);

        fillInputByIndex(driver, js, 7, "28.590086610421395, 77.2256714858255");
        Thread.sleep(1000);
        fillInputByIndex(driver, js, 8, "21");
        Thread.sleep(1000);
        fillInputByIndex(driver, js, 9, "Test Line 1");
        Thread.sleep(1000);
        fillInputByIndex(driver, js, 10, "Test Line 2");
        Thread.sleep(1000);

        fillInputByLabel(driver, wait, js, "Pincode", "143001");
        Thread.sleep(1000);

        selectDropdownByLabel(driver, wait, "Locality", "Main Road Abadpura");
        Thread.sleep(1000);

        fillDateByLabel(driver, wait, js, "Purchase Date", "20/02/2024");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Purchase Order Number", "PO123");
        Thread.sleep(1000);
        selectDropdownByLabel(driver, wait, "Source of Finance", "Municipal Funds");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Purchase Cost", "400000");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Acquisition Cost", "400000");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Market value as per the Evaluation", "300000");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Market value as per the Circle rate", "300000");
        Thread.sleep(1000);
        fillDateByLabel(driver, wait, js, "Invoice Date", "28/02/2024");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Invoice Number", "INV123");
        Thread.sleep(1000);
        fillInputByLabel(driver, wait, js, "Life of Asset", "10");
        Thread.sleep(1000);

        uploadFile(driver, wait, js, 0, ConfigReader.get("document.asset.proof"));
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.asset1.proof"));
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 3, ConfigReader.get("document.asset2.proof"));
        Thread.sleep(3000);

        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 4: SUBMIT APPLICATION
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Submitting Asset Application - Summary Page");
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
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit Grievance']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("Asset Managenent application: Submit clicked");
Thread.sleep(3000);
    }



// UTILITY METHODS

        private void fillInput(WebDriverWait wait, String fieldName, String value) {
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
            input.clear();
            input.sendKeys(value);
        }

    private void fillInputByPlaceholder(WebDriver driver, String placeholder, String value) throws InterruptedException {

        WebElement input = driver.findElement(
                By.xpath("//input[@placeholder='" + placeholder + "']")
        );

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(500);

        input.click();
        input.clear();
        input.sendKeys(value);

        Thread.sleep(800);
    }

    private void fillInputByLabel(WebDriver driver,
                                  WebDriverWait wait,
                                  JavascriptExecutor js,
                                  String labelText,
                                  String value) throws InterruptedException {

        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(normalize-space(),'" + labelText + "')]/following::input[1]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        Thread.sleep(400);

        if(!input.isEnabled() || input.getAttribute("readonly") != null){
            logger.info("Skipping readonly field: " + labelText);
            return;
        }

        input.click();
        input.clear();
        input.sendKeys(value);

        Thread.sleep(500);

        logger.info("Filled " + labelText);
    }

    private void fillDateByLabel(WebDriver driver,
                                 WebDriverWait wait,
                                 JavascriptExecutor js,
                                 String label,
                                 String value) throws InterruptedException {

        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(text(),'" + label + "')]/following::input[1]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(400);

        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL,"a"));
        input.sendKeys(value);

        Thread.sleep(500);
    }


    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
            js.executeScript("arguments[0].scrollIntoView(true);", button);
            Thread.sleep(300);
            button.click();
        }

        private void selectCityDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
            WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
            WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
            actions.moveToElement(cityDropdownArrow).click().perform();

            WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card")));
            WebElement firstCityOption = dropdownOptions.findElement(By.cssSelector(".profile-dropdown--item:first-child"));
            actions.moveToElement(firstCityOption).click().perform();
        }

        private void clickTakeActionButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
            WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='TAKE ACTION']]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
            Thread.sleep(300);
            takeActionButton.click();
            logger.info("Clicked TAKE ACTION button");
        }

        private void handleTakeActionMenu(WebDriver driver, WebDriverWait wait) throws InterruptedException {
            try {
                WebElement menuWrap = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.menu-wrap")));
                List<WebElement> actionOptions = menuWrap.findElements(By.tagName("p"));

                for (WebElement option : actionOptions) {
                    String text = option.getText().trim().toUpperCase();
                    if (text.equals("VERIFY")) {
                        option.click();
                        logger.info("Clicked VERIFY");
                        handlePopupAndSubmit(driver, wait, "Automated verification comment.",
                                ConfigReader.get("document.identity.proof"));
                        break;
                    } else if (text.equals("APPROVE")) {
                        option.click();
                        logger.info("Clicked APPROVE");
                        handlePopupAndSubmit(driver, wait, "Automated approval comment.",
                                ConfigReader.get("document.identity.proof"));
                        break;
                    } else if (text.equals("PAY")) {
                        option.click();
                        logger.info("Clicked PAY");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.info("Take Action Menu not found or no valid option present: " + e.getMessage());
            }
        }

        private void handlePopupAndSubmit(WebDriver driver, WebDriverWait wait, String comment, String filePath) throws InterruptedException {
            WebElement commentField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("comments")));
            commentField.clear();
            commentField.sendKeys(comment);

            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-doc")));
            fileInput.sendKeys(filePath);
            logger.info("Document uploaded");

            List<WebElement> verifyButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Verify']]"));
            List<WebElement> approveButtons = driver.findElements(By.xpath("//button[contains(@class, 'selector-button-primary') and .//h2[normalize-space()='Approve']]"));

            WebElement actionButton = null;
            if (!verifyButtons.isEmpty()) {
                actionButton = verifyButtons.get(0);
                logger.info("Clicking Verify button");
            } else if (!approveButtons.isEmpty()) {
                actionButton = approveButtons.get(0);
                logger.info("Clicking Approve button");
            } else {
                throw new RuntimeException("Neither Verify nor Approve button found!");
            }

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", actionButton);
            Thread.sleep(300);
            actionButton.click();
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

    private void selectDropdownByIndex(WebDriver driver,
                                       WebDriverWait wait,
                                       JavascriptExecutor js,
                                       int dropdownIndex,
                                       int optionIndex) throws InterruptedException {

        for(int attempt = 0; attempt < 3; attempt++) {

            try {

                List<WebElement> dropdowns = wait.until(
                        ExpectedConditions.presenceOfAllElementsLocatedBy(
                                By.cssSelector("div.employee-select-wrap svg.cp")
                        )
                );

                WebElement dropdown = dropdowns.get(dropdownIndex);

                js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);
                Thread.sleep(400);

                dropdown.click();

                List<WebElement> options = wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                By.cssSelector("div.profile-dropdown--item")
                        )
                );

                WebElement option = options.get(optionIndex);

                js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                option.click();

                Thread.sleep(700);

                return;

            } catch (StaleElementReferenceException e) {

                logger.info("Retrying dropdown due to React re-render...");
                Thread.sleep(500);

            }
        }

        throw new RuntimeException("Dropdown selection failed after retries");
    }

    private void selectAssetDropdown(WebDriver driver, int index, String value) throws InterruptedException {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        List<WebElement> inputs = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("div.employee-select-wrap input")
                )
        );

        WebElement input = inputs.get(index);

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(1000);  // Pause before click (1 sec)

        input.click();

        Thread.sleep(2000);  // Wait 2 sec after dropdown opens

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")
        ));

        List<WebElement> options = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("div.profile-dropdown--item")
                )
        );

        for (WebElement option : options) {
            if (option.getText().trim().equalsIgnoreCase(value)) {

                Thread.sleep(1500);  //  Pause before selecting option

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", option);

                Thread.sleep(2000);  //  Pause after selection (before next field)

                return;
            }
        }

        throw new RuntimeException("Option not found: " + value);
    }



    private void uploadFileByLabel(WebDriver driver,
                                   WebDriverWait wait,
                                   JavascriptExecutor js,
                                   String labelText,
                                   String filePath)
            throws InterruptedException {

        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//label[contains(normalize-space(),'" + labelText + "')]/following::input[@type='file'][1]")
                )
        );

        js.executeScript(
                "arguments[0].style.cssText='display:block !important; visibility:visible !important; opacity:1 !important;';",
                fileInput
        );

        Thread.sleep(300);

        fileInput.sendKeys(filePath);

        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", fileInput);

        Thread.sleep(500);

        logger.info("Uploaded file for " + labelText);
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

    private void selectDropdownByLabel(WebDriver driver,
                                       WebDriverWait wait,
                                       String labelText,
                                       String value) throws InterruptedException {

        By inputLocator = By.xpath(
                "//*[contains(normalize-space(),'" + labelText + "')]/following::input[1]"
        );

        // Retry mechanism for React re-render
        for (int attempt = 0; attempt < 3; attempt++) {

            try {
                WebElement input = wait.until(
                        ExpectedConditions.elementToBeClickable(inputLocator)
                );

                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].scrollIntoView({block:'center'});", input);

                Thread.sleep(300);

                input.click();

                WebElement dropdown = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("div.options-card")
                        )
                );

                wait.until(d ->
                        dropdown.findElements(By.cssSelector("div.profile-dropdown--item"))
                                .stream()
                                .anyMatch(el -> el.getText().trim().equalsIgnoreCase(value))
                );

                List<WebElement> options =
                        dropdown.findElements(By.cssSelector("div.profile-dropdown--item"));

                for (WebElement option : options) {
                    if (option.getText().trim().equalsIgnoreCase(value)) {
                        option.click();
                        Thread.sleep(800);
                        return;
                    }
                }

            } catch (StaleElementReferenceException e) {
                logger.info("Stale detected. Retrying dropdown: " + labelText);
                Thread.sleep(500);
            }
        }

        throw new RuntimeException("Option not found: " + value);
    }

    private void fillInputByLabel(WebDriver driver,
                                  WebDriverWait wait,
                                  String labelText,
                                  String value) throws InterruptedException {

        WebElement input = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//label[contains(text(),'" + labelText + "')]/following::input[1]")
                )
        );

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(400);

        input.click();
        input.clear();
        input.sendKeys(value);

        Thread.sleep(400);
    }

    private void fillDateByLabel(WebDriver driver,
                                 WebDriverWait wait,
                                 String labelText,
                                 String value) throws InterruptedException {

        WebElement input = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//label[contains(normalize-space(),'" + labelText + "')]/following::input[1]")
                )
        );

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(300);

        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(value);

        Thread.sleep(500);
    }
    private void fillInputByIndex(WebDriver driver,
                                  JavascriptExecutor js,
                                  int index,
                                  String value) throws InterruptedException {

        List<WebElement> inputs = driver.findElements(By.cssSelector("div.employeeCard input[type='text']"));

        if(index >= inputs.size()){
            throw new RuntimeException("Input index not found: " + index +
                    " | Total inputs: " + inputs.size());
        }

        WebElement input = inputs.get(index);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);

        Thread.sleep(400);

        if(!input.isEnabled() || input.getAttribute("readonly") != null){
            logger.info("Skipping readonly field index: " + index);
            return;
        }

        input.click();
        input.clear();
        input.sendKeys(value);
        driver.switchTo().activeElement().sendKeys(Keys.TAB);
        Thread.sleep(400);
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
}