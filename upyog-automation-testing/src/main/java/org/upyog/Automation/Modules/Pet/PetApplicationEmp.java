package org.upyog.Automation.Modules.Pet;

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
public class PetApplicationEmp {

    private static final Logger logger = LoggerFactory.getLogger(PetApplicationEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    /**
     * Main test method for pet application employee workflow
     * Runs automatically when Spring context is initialized
     */
    //@PostConstruct
    public void petInbox() {
        petInboxEmp(ConfigReader.get("employee.base.url"),
                    ConfigReader.get("app.login.username"),
                    ConfigReader.get("app.login.password"),
                    ConfigReader.get("pet.application.number"));
    }

    public void petInboxEmp(String baseUrl, String username, String password, String applicationNumber) {
        logger.info("Pet Application Employee Workflow");
        
        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);
            
            // STEP 2: Navigate to Pet Application Inbox
            navigateToInbox(driver, wait, js);
            
            // STEP 3: Search Application by Number
            selectFirstApplication(driver, wait, js, applicationNumber);
            
            // STEP 4: Process Application Workflow (Verify -> Approve -> Pay)
            processApplicationWorkflow(driver, wait);
            
            // STEP 5: Collect Payment
            collectPayment(driver, wait);
            
            // STEP 6: Download Receipts
            downloadReceipts(driver, wait, js);
            
            logger.info("Pet Application Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation
            
        } catch (Exception e) {
            logger.info("Exception in Pet Application Employee Workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Handles employee login process
     */
    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        logger.info("Open the Employee Login Portal");

        // Enter credentials from configuration
        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        logger.info("Filled username and password");

        // Select city dropdown
        selectCityDropdown(driver, wait, actions);
        
        // Click Continue button
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");
    }

    /**
     * Navigates to Pet Application Inbox
     */
    private void navigateToInbox(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Navigating to Pet Application Inbox");
        
        // Wait for page to load after login
        Thread.sleep(2000);
        
        // Click Inbox link
        WebElement inboxLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/employee/ptr/petservice/inbox' and contains(text(), 'Inbox')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", inboxLink);
        inboxLink.click();
        logger.info("Clicked Inbox link");
    }

    /**
     * Searches for application by application number
     */
    private void selectFirstApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String applicationNumber) throws InterruptedException {
        logger.info("Searching for application by number");
        
        // Wait for application number input field
        WebElement applicationInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.employee-card-input")));
        
        // Fill application number from configuration
        applicationInput.clear();
        applicationInput.sendKeys(applicationNumber);
        logger.info("Entered application number: " + applicationNumber);
        
        // Click Search button
        WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.submit-bar.submit-bar-search")));
        searchButton.click();
        logger.info("Clicked Search button");
        
        // Wait for search results to load
        Thread.sleep(2000);
        
        // Step 5: Select Application from Results
        WebElement appLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText(applicationNumber)));
        appLink.click();
        logger.info("Selected application: " + applicationNumber);
    }

    /**
     * Processes the complete application workflow: Verify -> Approve -> Pay
     */
    private void processApplicationWorkflow(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        logger.info("Processing application workflow");
        
        // Step 1: Verify
        clickTakeActionButton(driver, wait);
        handleTakeActionMenu(driver, wait);
        logger.info("Verification completed");
        
        // Step 2: Approve
        clickTakeActionButton(driver, wait);
        handleTakeActionMenu(driver, wait);
        logger.info("Approval completed");
        
        // Step 3: Pay
        clickTakeActionButton(driver, wait);
        handleTakeActionMenu(driver, wait);
        logger.info("Payment process initiated");
    }

    /**
     * Handles payment collection
     */
    private void collectPayment(WebDriver driver, WebDriverWait wait) {
        logger.info("Collecting payment");
        
        // Enter mobile number for payment
        WebElement mobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("payerMobile")));
        mobileInput.clear();
        mobileInput.sendKeys("9847584944");
        
        // Click Collect Payment button
        WebElement collectPaymentButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Collect Payment']]")));
        collectPaymentButton.click();
        logger.info("Payment collected");
    }

    /**
     * Downloads all available receipts
     */
    private void downloadReceipts(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Downloading receipts");
        
        List<WebElement> svgButtons = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div.primary-label-btn.d-grid")));
        
        for (WebElement buttonContainer : svgButtons) {
            WebElement svg = buttonContainer.findElement(By.tagName("svg"));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", svg);
            Thread.sleep(300);
            svg.click();
            logger.info("Downloaded: " + buttonContainer.getText().trim());
            Thread.sleep(1000);
        }
        logger.info("All receipts downloaded");
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
     * Utility method to click buttons with XPath
     */
    private void clickButton(WebDriverWait wait, JavascriptExecutor js, String xpath) throws InterruptedException {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
        js.executeScript("arguments[0].scrollIntoView(true);", button);
        Thread.sleep(300);
        button.click();
    }

    /**
     * Selects city dropdown during login
     */
    private void selectCityDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
        WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
        actions.moveToElement(cityDropdownArrow).click().perform();

        WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card")));
        WebElement firstCityOption = dropdownOptions.findElement(By.cssSelector(".profile-dropdown--item:first-child"));
        actions.moveToElement(firstCityOption).click().perform();
    }

    /**
     * Clicks the TAKE ACTION button
     */
    private void clickTakeActionButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='TAKE ACTION']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        logger.info("Clicked TAKE ACTION button");
    }

    /**
     * Handles the take action menu and selects appropriate action
     */
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
                } else if (text.equals("REJECT")) {
                    logger.info("Application Rejected");
                    break;
                }
            }
        } catch (Exception e) {
            logger.info("Take Action Menu not found or no valid option present: " + e.getMessage());
        }
    }

    /**
     * Handles popup submission with comment and document upload
     */
    private void handlePopupAndSubmit(WebDriver driver, WebDriverWait wait, String comment, String filePath) throws InterruptedException {
        // Enter comment
        WebElement commentField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("comments")));
        commentField.clear();
        commentField.sendKeys(comment);

        // Upload document
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-doc")));
        fileInput.sendKeys(filePath);
        logger.info("Document uploaded");

        // Click Verify or Approve button
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
}