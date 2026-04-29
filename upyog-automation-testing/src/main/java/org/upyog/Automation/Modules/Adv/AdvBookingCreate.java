package org.upyog.Automation.Modules.Adv;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Utils.ConfigReader;
import org.upyog.Automation.config.WebDriverFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Automated test class for UPYOG Advertisement Booking (Citizen)
 * Workflow:
 *  - Citizen login with OTP
 *  - Navigate to Advertisement → Book Advertisement
 *  - Fill advertisement details (dropdowns, dates, radio)
 *  - Select sites (checkboxes) and add to cart
 *  - View cart and book now
 *  - Fill applicant details & address
 *  - Upload documents (3 document rows on one page)
 *  - Submit application
 */
@Component
public class AdvBookingCreate {

    private static final Logger logger = LoggerFactory.getLogger(AdvBookingCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    /**
     * Main test method for Advertisement booking workflow.
     * Uncomment @PostConstruct above to run automatically on context init.
     */
    //@PostConstruct
    public void advBookingReg() {
        advBookingReg(ConfigReader.get("citizen.base.url"),
                     "Advertisement",
                     ConfigReader.get("citizen.mobile.number"),
                     ConfigReader.get("test.otp"),
                     ConfigReader.get("test.cityA.name"));
    }

    public void advBookingReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Advertisement Booking by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Advertisement Booking
            navigateToAdvertisement(driver, wait, js);

            // STEP 3: Fill Advertisement Details and Search
            fillAdvertisementDetails(driver, wait, js);

            // STEP 4: Select checkboxes and add to cart
            selectCheckboxesAndAddToCart(driver, wait, js);

            // STEP 5: View cart and Book Now
            viewCartAndBookNow(driver, wait, js);

            // STEP 6: Address Details (always Fill New Details for automation)
            handleAddressDetails(driver, wait, js);

            // STEP 7: Applicant Details
            fillApplicantDetails(driver, wait, js);

            // STEP 8: Applicant Address Details
            fillApplicantAddressDetails(driver, wait, js);

            // STEP 9: Upload Documents (3 rows on one page)
            uploadDocuments(driver, wait, js);

            // STEP 10: Submit Application
            submitApplication(driver, wait, js);

            logger.info("Advertisement Booking completed successfully!");
            Thread.sleep(50000); // Keep browser open for observation

        } catch (Exception e) {
            logger.error("Exception in Advertisement Booking", e);
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
    // STEP 2: NAVIGATE TO ADVERTISEMENT MODULE
    // =====================================================================

    private void navigateToAdvertisement(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Navigating to Advertisement Booking");

        // Sidebar Advertisement link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/ads-home']"))));

        Thread.sleep(2000);
        logger.info("Reached Advertisement home page");

        // "Advertisement Book" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/ads/bookad']"))));

        logger.info("Clicked Advertisement Book link");
    }

    // =====================================================================
    // STEP 3: ADVERTISEMENT DETAILS (dropdowns + dates + radio + search)
    // =====================================================================

    private void fillAdvertisementDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Advertisement Details");

        // Example: 3 dropdowns – Category, Ad Type, Location (adjust as per UI)
        selectDropdownOption(driver, wait, js, 0);
        selectDropdownOption(driver, wait, js, 1);
        selectDropdownOption(driver, wait, js, 2);
        Thread.sleep(1000);

        // Date range (type="date" → yyyy-MM-dd) - Dynamic dates
        List<WebElement> dateInputs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("input[type='date']")));


            WebElement fromDate = dateInputs.get(0);
            WebElement toDate = dateInputs.get(1);

        //  STEP 1: read min date from UI
        String minDateStr = fromDate.getAttribute("min");   // e.g. 2026-04-07
        LocalDate minDate = LocalDate.parse(minDateStr);


        //  STEP 2: calculate dates based on min
        LocalDate from = minDate.plusDays(1);
        LocalDate to = minDate.plusDays(10);


        //  format (ONLY valid for type=date)
        // FORMAT
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        String fromStr = from.format(formatter);
        String toStr = to.format(formatter);

// =========================
// FROM DATE
// =========================
        js.executeScript(
                "var input = arguments[0];" +
                        "var lastValue = input.value;" +
                        "input.value = arguments[1];" +


                        "var event = new Event('input', { bubbles: true });" +
                        "event.simulated = true;" +
                        "var tracker = input._valueTracker;" +
                        "if (tracker) { tracker.setValue(lastValue); }" +


                        "input.dispatchEvent(event);" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('blur', { bubbles: true }));",
                fromDate,
                fromStr
        );
// =========================
// TO DATE
// =========================
        js.executeScript(
                "var input = arguments[0];" +
                        "var lastValue = input.value;" +
                        "input.value = arguments[1];" +


                        "var event = new Event('input', { bubbles: true });" +
                        "event.simulated = true;" +
                        "var tracker = input._valueTracker;" +
                        "if (tracker) { tracker.setValue(lastValue); }" +


                        "input.dispatchEvent(event);" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('blur', { bubbles: true }));",
                toDate,
                toStr
        );
        logger.info("From Date: {}", fromStr);
        logger.info("To Date: {}", toStr);
        Thread.sleep(2000);
        Thread.sleep(1000);

        // Radio: Advertisement With Night Light? -> No
        selectRadioButtonByLabel(driver, "No");

        // Search
        clickButtonByHeader(driver, wait, "Search");
    }

    // =====================================================================
    // STEP 4: SELECT CHECKBOXES AND ADD TO CART
    // =====================================================================

    private void selectCheckboxesAndAddToCart(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Selecting checkboxes and adding to cart");

        // Only checkboxes inside result table
        By resultCheckboxesLocator = By.cssSelector("table tbody input[type='checkbox']");
        List<WebElement> checkboxes = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(resultCheckboxesLocator));

        logger.info("Found {} result checkboxes", checkboxes.size());

        for (int i = 0; i < checkboxes.size(); i++) {
            try {
                WebElement checkbox = checkboxes.get(i);
                js.executeScript("arguments[0].scrollIntoView(true);", checkbox);
                Thread.sleep(200);
                if (!checkbox.isSelected()) {
                    js.executeScript("arguments[0].click();", checkbox);
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                logger.error("Error clicking checkbox {}: {}", i, e.getMessage());
            }
        }

        // Add to Cart button
        WebElement addToCartBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(., 'Add to cart') or contains(., 'Add to Cart')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", addToCartBtn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", addToCartBtn);
        logger.info("Clicked Add to Cart");
        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 5: VIEW CART AND BOOK NOW
    // =====================================================================

    private void viewCartAndBookNow(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Viewing cart and booking");

        WebElement viewCartBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(., 'View Cart')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", viewCartBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", viewCartBtn);
        logger.info("Clicked View Cart");

        Thread.sleep(2000);

        WebElement bookNowBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(., 'Book Now')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", bookNowBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", bookNowBtn);
        logger.info("Clicked Book Now");

        Thread.sleep(2000);
    }

    // =====================================================================
    // STEP 6: ADDRESS DETAILS – FILL NEW DETAILS
    // =====================================================================

    private void handleAddressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Handling address details – Fill New Details");

        // For automation stability we always choose "Fill New Details"
        WebElement fillNewBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(., 'Fill New Details')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", fillNewBtn);
        js.executeScript("arguments[0].click();", fillNewBtn);
        Thread.sleep(2000);

        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 7: APPLICANT DETAILS
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
    // STEP 8: APPLICANT ADDRESS DETAILS
    // =====================================================================

    private void fillApplicantAddressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Filling Applicant Address Details");

        fillInput(wait, "houseNo", "A-12");
        fillInput(wait, "houseName", "Jagbir Bhawan");
        fillInput(wait, "streetName", "qwerty");
        fillInput(wait, "addressline1", "qwerty123");
        fillInput(wait, "addressline2", "qwerty2");
        fillInput(wait, "landmark", "qwerty123");

        // City & Locality dropdowns on this page (index may differ – adjust if needed)
        selectDropdownOption(driver, wait,js, 0); // City
        Thread.sleep(2000);
        selectDropdownOption(driver, wait,js, 1); // Locality

        fillInput(wait, "pincode", "110011");

        Thread.sleep(500);
        clickNextButton(driver, wait, js);
    }

    // =====================================================================
    // STEP 9: UPLOAD DOCUMENTS (3 rows on a single page)
    // =====================================================================

    private void uploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Uploading Documents");

        // 1) Select all three dropdowns (any valid option – first one is fine for automation)
        // Advertisement Sample Document
        selectDropdownOption(driver, wait,js, 0);

        // Applicant Address Proof
        selectDropdownOption(driver, wait,js, 1);

        // Applicant Identity Proof
        selectDropdownOption(driver, wait,js, 2);

        // 2) Upload 3 files in order – using your existing config keys
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.sampleDocument.proof"));   // top row
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.address.proof"));      // middle row
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.Identity.proof")); // bottom row

        Thread.sleep(1000);

        // 3) Click Next
        clickNextButton(driver, wait, js);
        logger.info("Finished Upload Documents step");
    }


    // =====================================================================
    // STEP 10: SUBMIT APPLICATION
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        logger.info("Submitting Advertisement Application");

        Thread.sleep(5000);
        // Declaration checkbox – assume last checkbox on page
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

        // Click Submit
        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        logger.info("Advertisement application: Submit clicked");

        // Wait for success acknowledgement (green banner) and then handle payment
        try {
            // Wait for a success-like message on the page (contains 'successfully' or 'Booking No')
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(
                            "//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'successfully')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(
                            "//*[contains(., 'Booking No') or contains(.,'Booking No.')]"))
            ));

            logger.info("Application submitted successfully (acknowledgement detected).");

            // Call payment handler (STEP 11)
            handlePaymentFlow(driver, wait, js);

        } catch (Exception e) {
            logger.info("Post-submit: success acknowledgement NOT detected within timeout: " + e.getMessage());
            // still attempt payment step in case page navigated directly to bill
            try {
                handlePaymentFlow(driver, wait, js);
            } catch (Exception ex) {
                logger.info("handlePaymentFlow also failed/skipped: " + ex.getMessage());
            }
        }

        logger.info("Advertisement Booking completed successfully!");
        Thread.sleep(50000);
    }


    //======================================================================
    // STEP 11: MAKE PAYMENT
    // =====================================================================

    /**
     * Helper: wait for common overlays/loaders to disappear
     */
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

    /**
     * Helper: try clicking an element multiple times, with a JS fallback.
     * Returns true if clicked.
     */
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


    /**
     * Payment flow tailored to: Net Banking -> select ICICI -> Pay -> click Success on mock bank
     * Improved with retries and overlay handling to avoid stuck at Proceed/Pay.
     */
    /**
     * Payment flow: Card → Pay Now → Razorpay mock "Success"
     */
    private void handlePaymentFlow(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        logger.info("Starting payment flow (Card → Pay Now → Success)...");

        // remember UPYOG window
        String mainHandle = driver.getWindowHandle();

        // -----------------------------
        // STEP 1: "Make Payment" (ack page)
        // -----------------------------
        try {
            By makePaymentSel = By.xpath(
                    "//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'make payment')]");
            boolean clicked = tryClickWithRetries(driver, wait, js, makePaymentSel, 25, 4, 700);
            if (clicked) {
                logger.info("Clicked 'Make Payment'");
                Thread.sleep(1000);
            } else {
                logger.info("'Make Payment' button not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            logger.info("'Make Payment' error: " + e.getMessage());
        }

        // -----------------------------
        // STEP 2: "Proceed To Pay" (Tax Bill Details page)
        // -----------------------------
        try {
            By proceedSel = By.xpath(
                    "//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'proceed to pay') or " +
                            "(contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'proceed') and " +
                            " contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'pay'))]");
            boolean proceedClicked = tryClickWithRetries(driver, wait, js, proceedSel, 40, 5, 800);
            if (proceedClicked) {
                logger.info("Clicked 'Proceed To Pay'");
                Thread.sleep(2000);
            } else {
                logger.info("'Proceed To Pay' not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            logger.info("'Proceed To Pay' error: " + e.getMessage());
        }

        // -----------------------------
        // STEP 3: "Pay" on UPYOG payment-method page (PAYGOV)
        // -----------------------------
        try {
            logger.info("Clicking UPYOG Pay button (STRICT)...");

            WebElement payBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//button[contains(@class,'submit-bar') and .//header[normalize-space()='Pay']]")
            ));
            logger.info("==== DEBUG STEP 3 ====");
            logger.info("After Pay click URL: " + driver.getCurrentUrl());
            logger.info("Window handles count: " + driver.getWindowHandles().size());
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", payBtn);
            Thread.sleep(500);

            // USER CLICK (IMPORTANT)
            new Actions(driver)
                    .moveToElement(payBtn)
                    .pause(Duration.ofMillis(300))
                    .click()
                    .perform();

            logger.info("UPYOG Pay clicked properly");

            // WAIT for gateway to initialize
            Thread.sleep(4000);

            logger.info("After Pay click URL: " + driver.getCurrentUrl());

        } catch (Exception e) {
            logger.info("Error in UPYOG Pay click: " + e.getMessage());
        }

        try {
            Thread.sleep(3000);

            Set<String> handles = driver.getWindowHandles();

            for (String handle : handles) {
                driver.switchTo().window(handle);
            }

            logger.info("Switched to payment window");
            logger.info("Current URL after switch: " + driver.getCurrentUrl());

        } catch (Exception e) {
            logger.info("Window switch failed: " + e.getMessage());
        }
        /* ------------------------------------------------------
       STEP 4 → CLICK "NET BANKING" TAB
    ------------------------------------------------------ */
        try {
            Thread.sleep(1500);
            java.util.List<By> NETBANKING_LOCATORS = java.util.Arrays.asList(
                    By.xpath("//a[contains(.,'Net Banking')]"),
                    By.xpath("//div[contains(.,'Net Banking')]"),
                    By.xpath("//button[contains(.,'Net Banking')]"),
                    By.xpath("//*[contains(text(),'Net Banking')]")
            );

            WebElement netBankingTab = null;

            for (By sel : NETBANKING_LOCATORS) {
                try {
                    netBankingTab = wait.until(ExpectedConditions.elementToBeClickable(sel));
                    if (netBankingTab != null) break;
                } catch (Exception ignored) {}
            }
            Thread.sleep(1000);
            logger.info("==== DEBUG STEP 4 ====");
            logger.info("Current URL before NetBanking: " + driver.getCurrentUrl());
            logger.info("Iframe count: " + driver.findElements(By.tagName("iframe")).size());
            if (netBankingTab != null) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", netBankingTab);
                Thread.sleep(1000);
                js.executeScript("arguments[0].click();", netBankingTab);
                logger.info("Clicked NET BANKING tab");
            } else {
                logger.info(" Net Banking tab NOT FOUND — maybe gateway UI changed or hidden.");
            }

            Thread.sleep(1000);

        } catch (Exception e) {
            logger.info("Error clicking Net Banking tab: " + e.getMessage());
        }

        try {
            Thread.sleep(2000);

            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));

            if (!iframes.isEmpty()) {
                driver.switchTo().frame(iframes.get(0));
                logger.info("Switched to payment iframe");
            }

        } catch (Exception e) {
            logger.info("Iframe switch failed: " + e.getMessage());
        }

    /* ------------------------------------------------------
       STEP 5 → SELECT ICICI BANK
    ------------------------------------------------------ */
        try {
            Thread.sleep(1500);
            logger.info("==== DEBUG STEP 5 ====");
            logger.info("Trying to find ICICI...");
            logger.info("Page contains ICICI text: " + driver.getPageSource().toLowerCase().contains("icici"));
            java.util.List<WebElement> iciciOptions =
                    driver.findElements(By.xpath("//*[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'icici')]"));

            if (iciciOptions.isEmpty()) {
                // Try bank icon alt text
                iciciOptions = driver.findElements(By.xpath("//img[contains(translate(@alt,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'icici')]/parent::*"));
            }

            if (!iciciOptions.isEmpty()) {
                WebElement icici = iciciOptions.get(0);
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", icici);
                Thread.sleep(200);
                js.executeScript("arguments[0].click();", icici);
                logger.info("Selected ICICI Bank");
            } else {
                logger.info("⚠ ICICI not found — clicking first available bank option");

                java.util.List<WebElement> bankTiles =
                        driver.findElements(By.xpath("//div[contains(@class,'bank') or contains(@class,'tile')]"));

                if (!bankTiles.isEmpty()) {
                    WebElement first = bankTiles.get(0);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", first);
                    Thread.sleep(200);
                    js.executeScript("arguments[0].click();", first);
                    logger.info("Clicked fallback BANK tile.");
                }
            }

            Thread.sleep(1000);
        } catch (Exception e) {
            logger.info("Error selecting bank: " + e.getMessage());
        }

    /* ------------------------------------------------------
       STEP 6 → CLICK "PAY" BUTTON
    ------------------------------------------------------ */
        try {
            By payBtn = By.xpath(
                    "//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'pay') " +
                            "and not(contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cancel'))]"
            );

            boolean ok = tryClickWithRetries(driver, wait, js, payBtn, 30, 5, 600);

            if (ok) {
                logger.info("Clicked PAY button");
            } else {
                logger.info("⚠ Pay button NOT FOUND on gateway.");
            }

            Thread.sleep(1500);

        } catch (Exception e) {
            logger.info("Error clicking Pay button: " + e.getMessage());
        }

    /* ------------------------------------------------------
       STEP 7 → CLICK "SUCCESS" (MOCK BANK PAGE)
    ------------------------------------------------------ */
        try {
            Thread.sleep(1500);

            WebElement successBtn = null;

            successBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(.,'Success')]")
            ));

            if (successBtn != null) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", successBtn);
                Thread.sleep(2000);
                js.executeScript("arguments[0].click();", successBtn);
                logger.info("Clicked SUCCESS on mock bank");
            }

        } catch (Exception e) {
            logger.info("Success button not found: " + e.getMessage());
        }
        // -----------------------------
        // STEP 8: Switch back to UPYOG window
        // -----------------------------
        try {
            driver.switchTo().window(mainHandle);
        } catch (Exception e) {
            logger.info("Could not switch back to UPYOG handle directly: " + e.getMessage());
            // fallback: pick any window that has 'upyog' in URL
            try {
                java.util.Set<String> handles = driver.getWindowHandles();
                for (String h : handles) {
                    driver.switchTo().window(h);
                    try {
                        String url = driver.getCurrentUrl();
                        if (url != null && url.toLowerCase().contains("upyog")) {
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        logger.info("Payment flow finished (Card route).");
    }


    /** small helper to read current url safely */
    private String safeGetUrl(WebDriver driver) {
        try { return driver.getCurrentUrl(); } catch (Exception e) { return "unknown"; }
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
            logger.info(" No file input at index " + index + " for " + filePath);
            return;
        }

        java.io.File f = new java.io.File(filePath);
        logger.info("Attempting upload from: " + f.getAbsolutePath() + "  exists? " + f.exists());

        if (!f.exists()) {
            logger.info("File does NOT exist on disk. Skipping this input.");
            return;
        }

        WebElement input = fileInputs.get(index);

        // Make sure Selenium can interact (visibility & scroll)
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", input);
        Thread.sleep(300);

        input.sendKeys(f.getAbsolutePath());
        logger.info("Uploaded document into input index " + index);
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