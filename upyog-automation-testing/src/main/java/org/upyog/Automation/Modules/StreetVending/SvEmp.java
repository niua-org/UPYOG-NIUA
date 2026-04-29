package org.upyog.Automation.Modules.StreetVending;

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
public class SvEmp {

    private static final Logger logger = LoggerFactory.getLogger(SvEmp.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct
    public void inboxEmpSv() {
        inboxEmpSv(ConfigReader.get("sv.employee.base.url"),
                  ConfigReader.get("sv.login.username"),
                  ConfigReader.get("SV.login.password"),
                  ConfigReader.get("sv.application.number"));
    }

    public void inboxEmpSv(String baseUrl, String username, String password, String applicationNumber) {

        logger.info("SV Employee Inbox Workflow");

        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions, baseUrl, username, password);

            // STEP 2: Navigate to SV Inbox
            navigateToInbox(driver, wait, js);

            // STEP 3: Search and Select Application
            searchAndSelectApplication(driver, wait, applicationNumber);

            // STEP 4: Execute Complete Workflow
            runWorkflow(driver, wait);

            logger.info("SV Employee Workflow completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.error("Exception in SV Employee Workflow: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String username, String password) throws InterruptedException {
        driver.get(baseUrl);
        driver.manage().window().maximize();
        logger.info("Open the Employee Login Portal");

        // Enter credentials from parameters
        fillInput(wait, "username", username);
        fillInput(wait, "password", password);
        logger.info("Filled username and password");

        // Select city dropdown
        selectCityDropdown(driver, wait, actions);

        // Click Continue button
        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]")));
        js.executeScript("arguments[0].scrollIntoView(true);", continueButton);
        continueButton.click();
    }

    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    private void selectCityDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        WebElement cityDropdownContainer = driver.findElement(By.cssSelector("div.select"));
        WebElement cityDropdownArrow = cityDropdownContainer.findElement(By.tagName("svg"));
        actions.moveToElement(cityDropdownArrow).click().perform();

        WebElement dropdownOptions = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        WebElement firstCityOption = dropdownOptions.findElement(By.cssSelector(".profile-dropdown--item:first-child"));
        actions.moveToElement(firstCityOption).click().perform();
    }

    private void navigateToInbox(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Navigating to SV Inbox");

        Thread.sleep(300);

        WebElement svInbox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//a[contains(@href, '/sv-ui/employee/sv/inbox') and contains(normalize-space(.), 'Inbox')]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", svInbox);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", svInbox);
        logger.info("Clicked SV Inbox");
    }

    private void searchAndSelectApplication(WebDriver driver, WebDriverWait wait, String applicationNumber) throws InterruptedException {
        logger.info("Searching and selecting application");

        Thread.sleep(1000);

        // Enter application number from parameter
        WebElement appNumberInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("employee-card-input")));
        appNumberInput.clear();
        appNumberInput.sendKeys(applicationNumber);

        // Click search button
        WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("submit-bar-search")));
        searchButton.click();

        // Click on application link
        WebElement appLink = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.linkText(applicationNumber)));
        appLink.click();
        logger.info("Selected application: {}", applicationNumber);
    }

    private void runWorkflow(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        logger.info("========== Executing complete workflow ==========");

        logger.info("\n========== STARTING 1ST FORWARD ==========");
        FirstForwardStep(driver, wait);
        logger.info("========== COMPLETED 1ST FORWARD ==========\n");

        Thread.sleep(2000);
        logger.info("\n========== STARTING 2ND FORWARD ==========");
        SecondForwardStep(driver, wait);
        logger.info("========== COMPLETED 2ND FORWARD ==========\n");

        logger.info("\n========== STARTING APPROVE ==========");
        approveStep(driver, wait);
        logger.info("========== COMPLETED APPROVE ==========\n");

        logger.info("\n========== STARTING COLLECT FEES ==========");
        collectFeesStep(driver, wait);
        logger.info("========== COMPLETED COLLECT FEES ==========\n");

        logger.info("========== Full workflow completed successfully! ==========");
    }

    private void FirstForwardStep(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        logger.info("[FORWARD] Step 1: Clicking Take Action button...");
        clickTakeActionButton(driver, wait);
        
        logger.info("[FORWARD] Step 2: Clicking Forward option...");
        clickForward(driver, wait);
        
        logger.info("[FORWARD] Step 3: Selecting dropdown option...");
        selectDropdownFirstOption(driver, wait);
        
        logger.info("[FORWARD] Step 4: Filling comments...");
        fillComments(driver, wait, "Forward");
        
        logger.info("[FORWARD] Step 5: Clicking Forward button...");
        clickForwardButton(driver, wait);
        
        logger.info("[FORWARD] First forward steps completed successfully!");
    }


    private void SecondForwardStep(WebDriver driver, WebDriverWait wait) throws InterruptedException {

        Thread.sleep(2000);
        logger.info("[FORWARD] Step 1: Clicking Take Action button...");
        clickTakeActionButton2(driver, wait);

        logger.info("[FORWARD] Step 2: Clicking Forward option...");
        clickForward(driver, wait);

        logger.info("[FORWARD] Step 3: Selecting dropdown option...");
        selectDropdownFirstOption(driver, wait);

        logger.info("[FORWARD] Step 4: Filling comments...");
        fillComments(driver, wait, "Forward");

        logger.info("[FORWARD] Step 5: Clicking Forward button...");
        clickForwardButton(driver, wait);

        logger.info("[FORWARD] All steps completed successfully!");
    }

    private void clickTakeActionButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
       // Thread.sleep(5000);
        WebElement takeActionButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and .//header[normalize-space()='TAKE ACTION']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(300);
        takeActionButton.click();
        logger.info("Clicked TAKE ACTION button");
    }

    private void clickTakeActionButton2(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        Thread.sleep(1000);

        // Wait for page to be stable
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));

        // Get fresh element reference
        WebElement takeActionButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@type='button' and contains(@class, 'submit-bar')]//header[text()='TAKE ACTION']/..")));

        // Wait for it to be clickable
        wait.until(ExpectedConditions.elementToBeClickable(takeActionButton));

        // Scroll into view
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", takeActionButton);
        Thread.sleep(500);

        // Try multiple click methods
        try {
            takeActionButton.click();
            logger.info("Clicked TAKE ACTION button (regular click)");
        } catch (Exception e) {
            try {
                Actions actions = new Actions(driver);
                actions.moveToElement(takeActionButton).click().perform();
                logger.info("Clicked TAKE ACTION button (Actions click)");
            } catch (Exception e2) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", takeActionButton);
                logger.info("Clicked TAKE ACTION button (JS click)");
            }
        }
    }




    private void clickForward(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement forwardOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div/p[text()='Forward']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", forwardOption);
        Thread.sleep(2000);
        logger.info("Clicked Forward option");
    }

    private void selectDropdownFirstOption(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        // Click dropdown to open
        WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("div.employee-select-wrap svg.cp")));
        dropdown.click();
        Thread.sleep(1000);
        
        // Select first option from dropdown
        WebElement optionsContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.options-card")));
        WebElement firstOption = optionsContainer.findElement(
                By.cssSelector("div.profile-dropdown--item:first-child"));
        firstOption.click();
        logger.info("Selected first option from dropdown");
        Thread.sleep(500);
    }

    private void fillComments(WebDriver driver, WebDriverWait wait, String comment) throws InterruptedException {
        WebElement commentsField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.name("comments")));
        commentsField.clear();
        commentsField.sendKeys(comment);
        Thread.sleep(300);
        logger.info("Filled comments: {}", comment);
    }

    private void clickForwardButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement forwardButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit'][@form='modal-action']//h2[text()='Forward']/..")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", forwardButton);
        Thread.sleep(1000);
        logger.info("Clicked Forward button");
    }

    private void approveStep(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        clickTakeActionButton(driver, wait);
        clickApprove(driver, wait);
        fillComments(driver, wait, "Approved");
        clickApproveButton(driver, wait);
    }

    private void clickApprove(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        Thread.sleep(1000);
        WebElement approveOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div/p[text()='Approve']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveOption);
        logger.info("Clicked Approve option");
        Thread.sleep(500);
    }

    private void clickApproveButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement approveButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit'][@form='modal-action']//h2[text()='Approve']/..")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveButton);
        Thread.sleep(1000);
        logger.info("Clicked Approve button");
    }

    private void collectFeesStep(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        clickTakeActionButton(driver, wait);
        clickCollectFees(driver, wait);
        fillPayerMobile(driver, wait);
    }

    private void clickCollectFees(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        Thread.sleep(1000);
        WebElement collectFeesOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div/p[text()='Collect Fees']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", collectFeesOption);
        logger.info("Clicked Collect Fees option");
        Thread.sleep(500);
    }

    private void fillPayerMobile(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        Thread.sleep(1000);
        WebElement payerMobileInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.name("payerMobile")));
        payerMobileInput.clear();
        payerMobileInput.sendKeys("9999999999");
        logger.info("Filled Payer Mobile: 9999999999");
        Thread.sleep(500);
        
        // Click Collect Payment button
        clickCollectPaymentButton(driver, wait);
    }

    private void clickCollectPaymentButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement collectPaymentButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit']//header[text()='Collect Payment']/..")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", collectPaymentButton);
        Thread.sleep(300);
        collectPaymentButton.click();
        logger.info("Clicked Collect Payment button");
        Thread.sleep(1000);
    }
}
