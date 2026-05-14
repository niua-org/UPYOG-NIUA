package org.upyog.Automation.Modules.CHB;

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
import org.upyog.Automation.config.WebDriverFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
public class ChbCreate {

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct

    public void chbReg() {
        chbReg(ConfigReader.get("citizen.base.url"),
                "Community Hall Booking",
                ConfigReader.get("citizen.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city1.name"));
    }

    public void chbReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        System.out.println("Community Hall Booking by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to Community Hall Booking
            navigateToCommunityHallBooking(driver, wait, js);

            // STEP 3: Search Community Hall Booking
            searchCommunityHall(driver, wait, js);

            // STEP 4: Select Checkboxes
            selectCheckBox(driver, wait, js);

            // STEP 5: Select Fill New Details
            selectFillNewDetails(driver, wait, js);

            // STEP 6: Info Page
            infoPage(driver, wait, js);

            // STEP 7: Fill Applicant Details
            fillApplicantDetails(driver, wait, js);

            // STEP 8: Additional Details
            fillAdditionalDetails(driver, wait, js);

            // STEP 9: Address Details
            fillAddressDetails(driver, wait, js);

            // STEP 10: Fill Bank Details
            fillBankDetails(driver, wait, js);

            // STEP 11: Upload Documents
            uploadDocumentPage(driver, wait, js);

            // STEP 12: Summary Page
            submitApplication(driver, wait, js);

            // STEP 13: Payment Handle
            handlePaymentFlow(driver, wait, js);

        } catch (Exception e) {
            System.out.println("Exception in Community Hall Booking Registration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }}
    }

    //=====================================================================
    // STEP 1: CITIZEN LOGIN
    // =====================================================================


    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
            throws InterruptedException {

        driver.get(baseUrl);
        System.out.println("Open the Citizen Login Portal");

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
    // STEP 2: NAVIGATE TO COMMUNITY HALL BOOKING MODULE
    //=====================================================================


    private void navigateToCommunityHallBooking(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to Community Hall Booking");

        // Sidebar Community Hall Booking link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/chb-home']"))));

        Thread.sleep(2000);
        System.out.println("Reached Community Hall Booking home page");

        // "Community Hall Search" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/chb/bookHall']"))));

        System.out.println("Clicked Search Hall link");
    }


    // =====================================================================
    // STEP 3: SEARCH COMMUNITY HALL/PARKS
    //=====================================================================


    private void searchCommunityHall(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Searching Community Hall");

        selectDropdownByIndex(driver, wait, js, 0, 0);
        System.out.println("Selected Hall Name");
        Thread.sleep(1000);

        selectDateRange(driver, wait, js);
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 1, 0);
        System.out.println("Selected Hall Name");
        Thread.sleep(1000);

        clickButtonByHeader(driver, wait, "Search");
        Thread.sleep(2000);

    }

    // =====================================================================
    // STEP 4: SELECT CHECKBOXES
    //=====================================================================

    private void selectCheckBox(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Selecting checkboxes");

        // Only checkboxes inside result table
        By resultCheckboxesLocator = By.cssSelector("table tbody input[type='checkbox']");
        List<WebElement> checkboxes = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(resultCheckboxesLocator));

        System.out.println("Found " + checkboxes.size() + " result checkboxes");

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
                System.out.println("Error clicking checkbox " + i + ": " + e.getMessage());
            }
        }
        WebElement bookbtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(., 'Book') or contains(., 'Book')]")));
        js.executeScript("arguments[0].scrollIntoView(true);", bookbtn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", bookbtn);
        System.out.println("Clicked Book");
        Thread.sleep(2000);

    }

    // =====================================================================
    // STEP 5: SELECT FILL NEW DETAILS
    //=====================================================================

    private void selectFillNewDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling address details – Fill New Details");

        // Wait for popup (overlay/modal)
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'ReactModal__Content') or contains(@class,'popup')]")
        ));

        Thread.sleep(1000);

        // Correct button locator
        WebElement fillNewBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Fill New Details']")
        ));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fillNewBtn);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", fillNewBtn);

        System.out.println(" Clicked Fill New Details");

        Thread.sleep(2000);
    }

    //=====================================================================
    // STEP 6: INFO PAGE DETAILS
    // =====================================================================


    private void infoPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Construction and Demolition Info Page - Clicking Next");
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
                System.out.println("Clicked Next on info page");
                return;
            } catch (Exception e) {
                System.out.println("Next selector failed: " + selector);
            }
        }

        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 7: FILL APPLICANT DETAILS
    // =====================================================================

    private void fillApplicantDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Applicant Details");

        fillInput(wait, "applicantName", "Arpit Rao");
        Thread.sleep(1000);
        fillInput(wait, "emailId", "arpit@gmail.com");
        Thread.sleep(1000);

        // mobile optional
        try {
            fillInput(wait, "mobileNumber", "9999999999");
        } catch (Exception e) {
            System.out.println("Mobile skipped");
        }

        Thread.sleep(1500);

        // IMPORTANT: wait until button ENABLED
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(@class,'submit-bar')]")
        ));

        wait.until(driver1 -> btn.isEnabled());

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", btn);

        System.out.println("Clicked Next");
    }

    // =====================================================================
    // STEP 8: FILL ADDITIONAL DETAILS
    // =====================================================================

    private void fillAdditionalDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Additional Details");
        Thread.sleep(500);

        selectDropdownByIndex(driver, wait, js, 0, 0);
        Thread.sleep(1000);

        selectDropdownByIndex(driver, wait, js, 1, 1);
        Thread.sleep(1000);

        WebElement purposeDescriptionInput = null;

        // Correct selectors (textarea)
        By[] purposeDescriptionSelectors = {
                By.xpath("//textarea[contains(@name,'purposeDescription')]"),
                By.xpath("//textarea[contains(@placeholder,'Purpose')]"),
                By.cssSelector("textarea.card-textarea")
        };

        // Find textarea
        for (By selector : purposeDescriptionSelectors) {
            try {
                purposeDescriptionInput = wait.until(ExpectedConditions.elementToBeClickable(selector));
                break;
            } catch (Exception e) {
                System.out.println("Purpose Description selector failed: " + selector);
            }
        }

        // FILL TEXTAREA
        if (purposeDescriptionInput != null) {

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", purposeDescriptionInput);
            Thread.sleep(500);

            js.executeScript("arguments[0].click();", purposeDescriptionInput);

            purposeDescriptionInput.clear();
            purposeDescriptionInput.sendKeys("Community Meeting");

            // React validation trigger
            js.executeScript(
                    "arguments[0].dispatchEvent(new Event('input', {bubbles: true}))",
                    purposeDescriptionInput
            );

            System.out.println("Purpose Description filled");

        } else {
            System.out.println(" Purpose Description not found");
        }

        Thread.sleep(1000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);
    }

    // =====================================================================
    // STEP 9: FILL ADDRESS DETAILS
    // =====================================================================

    private void fillAddressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        fillInput(wait, "pincode", "143001");
        Thread.sleep(500);

        fillInput(wait, "streetName", "Test Street");
        Thread.sleep(500);

        fillInput(wait, "houseNo", "420");
        Thread.sleep(500);

        WebElement landmarkTextarea = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("textarea.card-textarea")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", landmarkTextarea);
        Thread.sleep(300);

        landmarkTextarea.click();
        landmarkTextarea.clear();
        landmarkTextarea.sendKeys("Test Land Registration");

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", landmarkTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur',{bubbles:true}));", landmarkTextarea);

        Thread.sleep(3000);

        clickNextButton(driver, wait, js);

        Thread.sleep(1000);
        System.out.println("Filled Address Details");

    }


    // =====================================================================
    // STEP 10: FILL BANK DETAILS
    // =====================================================================

    private void fillBankDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Filling Bank Account Details");
        Thread.sleep(500);

        fillInput(wait, "accountNumber", "1168977476837687");
        Thread.sleep(1000);

        fillInput(wait, "confirmAccountNumber", "1168977476837687");
        Thread.sleep(1000);

        fillInput(wait, "ifscCode", "ICIC0000001");
        Thread.sleep(1000);

        fillInput(wait, "accountHolderName", "Arpit Rao");
        Thread.sleep(1000);


        clickNextButton(driver, wait, js);

        Thread.sleep(2000);
        System.out.println("Filled Bank Account Details");

    }

    // =====================================================================
    // STEP 11: SUMMARY PAGE
    // =====================================================================

    private void uploadDocumentPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Uploading Documents");
        Thread.sleep(2000);

        // Document 0: Bank Account Proof
        selectDropdownByIndex(driver, wait, js, 0, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.bankAccount.proof"));
        Thread.sleep(2000);

        // Document 1: Address proof
        selectDropdownByIndex(driver, wait, js, 1, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.addressChb.proof"));
        Thread.sleep(2000);

        // Document 2: Applicant Identity Proof
        selectDropdownByIndex(driver, wait, js, 2, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.applicantIdentity.proof"));
        Thread.sleep(2000);

        clickNextButton(driver, wait, js);
        Thread.sleep(1000);

    }

    // =====================================================================
    // STEP 12: SUMMARY PAGE
    // =====================================================================

    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Submitting Property Tax Application - Summary Page");
        Thread.sleep(3000);

        List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
        if (!checkboxes.isEmpty()) {
            WebElement lastCheckbox = checkboxes.get(checkboxes.size() - 1);
            try {
                if (!lastCheckbox.isSelected()) {
                    js.executeScript("arguments[0].scrollIntoView(true);", lastCheckbox);
                    Thread.sleep(300);
                    js.executeScript("arguments[0].click();", lastCheckbox);
                    System.out.println("Checked declaration checkbox");
                }
            } catch (Exception ex) {
                System.out.println("Could not click declaration checkbox: " + ex.getMessage());
            }
        }

        WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@class='submit-bar ' and @type='button'][.//header[text()='Submit']]")));
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(300);
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(200);
        submitButton.click();
        System.out.println("Property tax application: Submit clicked");
    }

    // =====================================================================
    // STEP 13: MAKE PAYMENT
    // =====================================================================

    private void handlePaymentFlow(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Starting payment flow (Card → Pay Now → Success)...");

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
                System.out.println("Clicked 'Make Payment'");
                Thread.sleep(1000);
            } else {
                System.out.println("'Make Payment' button not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            System.out.println("'Make Payment' error: " + e.getMessage());
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
                System.out.println("Clicked 'Proceed To Pay'");
                Thread.sleep(2000);
            } else {
                System.out.println("'Proceed To Pay' not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            System.out.println("'Proceed To Pay' error: " + e.getMessage());
        }

        // -----------------------------
        // STEP 3: "Pay" on UPYOG payment-method page (PAYGOV)
        // -----------------------------
        try {
            System.out.println("Clicking UPYOG Pay button (STRICT)...");

            WebElement payBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//button[contains(@class,'submit-bar') and .//header[normalize-space()='Pay']]")
            ));
            System.out.println("==== DEBUG STEP 3 ====");
            System.out.println("After Pay click URL: " + driver.getCurrentUrl());
            System.out.println("Window handles count: " + driver.getWindowHandles().size());
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", payBtn);
            Thread.sleep(500);

            // REAL USER CLICK (IMPORTANT)
            new Actions(driver)
                    .moveToElement(payBtn)
                    .pause(Duration.ofMillis(300))
                    .click()
                    .perform();

            System.out.println("UPYOG Pay clicked properly");

            // WAIT for gateway to initialize
            Thread.sleep(4000);

            System.out.println("After Pay click URL: " + driver.getCurrentUrl());

        } catch (Exception e) {
            System.out.println("Error in UPYOG Pay click: " + e.getMessage());
        }

        try {
            Thread.sleep(3000);

            Set<String> handles = driver.getWindowHandles();

            for (String handle : handles) {
                driver.switchTo().window(handle);
            }

            System.out.println("Switched to payment window");
            System.out.println("Current URL after switch: " + driver.getCurrentUrl());

        } catch (Exception e) {
            System.out.println("Window switch failed: " + e.getMessage());
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
                } catch (Exception ignored) {
                }
            }
            Thread.sleep(1000);
            System.out.println("==== DEBUG STEP 4 ====");
            System.out.println("Current URL before NetBanking: " + driver.getCurrentUrl());
            System.out.println("Iframe count: " + driver.findElements(By.tagName("iframe")).size());
            if (netBankingTab != null) {
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", netBankingTab);
                Thread.sleep(1000);
                js.executeScript("arguments[0].click();", netBankingTab);
                System.out.println("Clicked NET BANKING tab");
            } else {
                System.out.println(" Net Banking tab NOT FOUND — maybe gateway UI changed or hidden.");
            }

            Thread.sleep(1000);

        } catch (Exception e) {
            System.out.println("Error clicking Net Banking tab: " + e.getMessage());
        }

        try {
            Thread.sleep(2000);

            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));

            if (!iframes.isEmpty()) {
                driver.switchTo().frame(iframes.get(0));
                System.out.println("Switched to payment iframe");
            }

        } catch (Exception e) {
            System.out.println("Iframe switch failed: " + e.getMessage());
        }

    /* ------------------------------------------------------
       STEP 5 → SELECT ICICI BANK
    ------------------------------------------------------ */
        try {
            Thread.sleep(1500);
            System.out.println("==== DEBUG STEP 5 ====");
            System.out.println("Trying to find ICICI...");
            System.out.println("Page contains ICICI text: " + driver.getPageSource().toLowerCase().contains("icici"));
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
                System.out.println("Selected ICICI Bank");
            } else {
                System.out.println("⚠ ICICI not found — clicking first available bank option");

                java.util.List<WebElement> bankTiles =
                        driver.findElements(By.xpath("//div[contains(@class,'bank') or contains(@class,'tile')]"));

                if (!bankTiles.isEmpty()) {
                    WebElement first = bankTiles.get(0);
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", first);
                    Thread.sleep(200);
                    js.executeScript("arguments[0].click();", first);
                    System.out.println("Clicked fallback BANK tile.");
                }
            }

            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Error selecting bank: " + e.getMessage());
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
                System.out.println("Clicked PAY button");
            } else {
                System.out.println("Pay button NOT FOUND on gateway.");
            }

            Thread.sleep(1500);

        } catch (Exception e) {
            System.out.println("Error clicking Pay button: " + e.getMessage());
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
                System.out.println("Clicked SUCCESS on mock bank");
            }

        } catch (Exception e) {
            System.out.println("Success button not found: " + e.getMessage());
        }
        // -----------------------------
        // STEP 8: Switch back to UPYOG window
        // -----------------------------
        try {
            driver.switchTo().window(mainHandle);
        } catch (Exception e) {
            System.out.println("Could not switch back to UPYOG handle directly: " + e.getMessage());
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
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        System.out.println("Payment flow finished (Card route).");
    }


    /**
     * small helper to read current url safely
     */
    private String safeGetUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            return "unknown";
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
                System.out.println("Filled optional field: " + fieldName);
            } else {
                System.out.println("Optional field " + fieldName + " not interactable, skipping");
            }
        } catch (Exception e) {
            System.out.println("Optional field " + fieldName + " not found, skipping");
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
        System.out.println("Clicked Next");
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
                    System.out.println("Clicked element " + locator + " (attempt " + attempt + ")");
                    return true;
                } catch (Exception clickEx) {
                    // fallback to JS click
                    try {
                        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                        Thread.sleep(150);
                        js.executeScript("arguments[0].click();", el);
                        System.out.println("JS-clicked element " + locator + " (attempt " + attempt + ")");
                        return true;
                    } catch (Exception jsEx) {
                        System.out.println("Click failed attempt " + attempt + " for " + locator + " : " + jsEx.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("Element not clickable yet (" + locator + ") attempt " + attempt + " : " + e.getMessage());
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
                System.out.println("Selected radio button: " + labelText);
            }
        } catch (Exception e) {
            System.out.println("Error selecting radio button '" + labelText + "': " + e.getMessage());
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
            System.out.println("Dropdown index " + dropdownIndex + " not found. Total: " + dropdownSvgs.size());
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
            System.out.println("File input index " + index + " not found for path: " + filePath);
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        // Make sure Selenium can interact with the hidden input
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fileInput);
        js.executeScript("arguments[0].style.opacity='1'; arguments[0].style.display='block';", fileInput);
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        System.out.println("Uploaded file at index " + index + ": " + filePath);
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
        System.out.println("Selected radio: " + labelText);
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

        System.out.println("Selected radio index: " + index);
        Thread.sleep(500);
    }

    private void selectDateRange(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Selecting Date Range via Calendar Icon");

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

        System.out.println("Calendar icon clicked");

        Thread.sleep(2000); // wait for calendar UI

        WebElement threeDays = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[text()='Three Days']")
        ));
        js.executeScript("arguments[0].click();", threeDays);

        System.out.println("Three Days selected");
        }
}