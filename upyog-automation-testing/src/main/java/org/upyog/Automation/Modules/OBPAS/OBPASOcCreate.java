package org.upyog.Automation.Modules.OBPAS;

import java.time.Duration;
import java.util.List;
import java.util.Set;

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
import org.upyog.Automation.Utils.DriverFactory;
import org.upyog.Automation.Utils.TestDataStore;
import org.upyog.Automation.config.WebDriverFactory;

@Component
public class OBPASOcCreate {

    @Autowired
    private WebDriverFactory webDriverFactory;

    //@PostConstruct

    public void obpasOCReg() {
        obpasOCReg(ConfigReader.get("citizen.base.url"),
                "OBPAS",
                ConfigReader.get("architect.mobile.number"),
                ConfigReader.get("test.otp"),
                ConfigReader.get("test.city.name"),
                ConfigReader.get("permit.number"));
    }

    public void obpasOCReg(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String permitNumber) {
        System.out.println("OBPAS Registration by Citizen");

        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = DriverFactory.createWebDriverWait(driver);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Architect Login
            performArchitectLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 2: Navigate to OBPAS Module
            navigateToOBPAS(driver, wait, js);

            // STEP 3: Info page
            InfoPage(driver, wait, js);

            // STEP 4: Permit Number
            permitNumber(driver);

            // STEP 5: Upload DXF
            uploadDxf(driver, wait, js);

            // STEP 6: OC Building Permit
            clickApplyForOcNewBuildingPermit(driver, wait, js);

            // STEP 7: Info Page Details
            infoPageDetails(driver,wait, js);

            // STEP 8: Basic Details Page
            basicDetailPage(driver, wait, js);

            // STEP 9: Plot Details
            plotDetailPage(driver, wait, js);

            // STEP 10: Scrutiny Details Page
            scrutinyDetailPage(driver, wait, js);

            // STEP 11: Documents Details Page
            uploadDocumentPage(driver, wait, js);

            // STEP 12: Noc Upload Page
            nocUploadPage(driver, wait, js);

            // STEP 13: Summary Page
            summaryPage(driver, wait, js);

            // STEP 14: Logout
            logout(driver, wait, js);

            // STEP 15: Citizen Login
            performCitizenLogin(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 16: Navigate to OBPAS Module
            navigateToOBPAS1(driver, wait, js);

            // STEP 17: My Application
            myApplications(driver, wait, js);

            // STEP 18: Application Approval
            citizenApproveFlow(driver, wait, js);

            // STEP 19: Forward Popup
            applicationPopup(driver, wait, js);

            // STEP 20: Logout Again
            logoutAgain(driver, wait, js);

            // STEP 21: Citizen Login Again
            performArchitectLoginAgain(driver, wait, js, actions, baseUrl, mobileNumber, otp, cityName);

            // STEP 22: Architect Login Again
            navigateToOBPAS2(driver, wait, js);

            // STEP 23: Open Application
            openFirstApplication(driver, wait, js);

            // STEP 24: Submit Application
            submitApplication(driver, wait, js);

            // STEP 25: Payment
            handlePaymentFlow(driver, wait, js);



        } catch (Exception e) {
            System.out.println("Exception in OBPAS Registration: " + e.getMessage());
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
             STEP 1: ARCHITECT LOGIN
             =====================================================================
             */

    private void performArchitectLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
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

             /*
             =====================================================================
             STEP 2: NAVIGATE TO OBPAS MODULE
             =====================================================================
             */

    private void navigateToOBPAS(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to OBPAS");

        // Sidebar Property Tax link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/obps-home']"))));

        Thread.sleep(2000);
        System.out.println("Reached OBPAS home page");

        // "Registered Architect Login" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/obps/home']"))));

        System.out.println("Clicked Registered Architect Login link");

        // "Plan scrutiny for new construction" link
        WebElement newConstruction = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(),'OC Plan Scrutiny for new Construction')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", newConstruction);
        Thread.sleep(500);

        js.executeScript("arguments[0].click();", newConstruction);

        System.out.println("Clicked Plan scrutiny for new construction");
        Thread.sleep(2000);

    }

            /*
             =====================================================================
             STEP 3: INFO PAGE DETAILS
             =====================================================================
            */

    private void InfoPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("OBPAS Info Page - Clicking Next");
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

             /*
             =====================================================================
             STEP 4: PERMIT NUMBER AND PERMIT DATE
             =====================================================================
             */

    private void permitNumber(WebDriver driver) throws InterruptedException {


        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;


        String permitNumber = TestDataStore.PERMIT_NUMBER;
        String permitDate   = TestDataStore.PERMIT_DATE;


        System.out.println("Using Permit: " + permitNumber);
        System.out.println("Using Date: " + permitDate);




        // =============================
        // STEP 1: PERMIT NUMBER
        // =============================
        WebElement permitInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@name='permitNumber']")
                )
        );


        js.executeScript(
                "const el = arguments[0];" +
                        "const val = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,val);" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));",
                permitInput,
                permitNumber
        );




        // =============================
        // STEP 2: DATE (CRITICAL FIX)
        // =============================
        WebElement dateInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@type='date']")
                )
        );


        js.executeScript(
                "const el = arguments[0];" +
                        "const val = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
                        "setter.call(el,val);" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.blur();",
                dateInput,
                permitDate
        );


        System.out.println("Set Date: " + dateInput.getAttribute("value"));




        // WAIT FOR REACT VALIDATION
        Thread.sleep(1000);






        // STEP 3: SEARCH


        WebElement searchBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Search']")
                )
        );


        js.executeScript("arguments[0].click();", searchBtn);


        System.out.println("Search clicked");
        Thread.sleep(3000);






        // STEP 4: NEXT BUTTON


        WebElement proceedBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Proceed for OC Scrutiny']")
                )
        );
        js.executeScript("arguments[0].click();", proceedBtn);


        System.out.println("Proceed for OC Scrutiny clicked");
    }


            /*
             =====================================================================
             STEP 5: UPLOAD DXF
             =====================================================================
            */

    private void uploadDxf(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Uploading DXF");
        Thread.sleep(2000);

        System.out.println("Uploading the DXF file");
        uploadDxf(driver, wait, js, 0, ConfigReader.get("document.drawing.dxf"));
        Thread.sleep(3000);
        System.out.println("Finished Upload DXF Documents step");

        WebElement submitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(@class,'submit-bar') and @type='submit']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(300);

        submitBtn.click();
        Thread.sleep(10000);

        System.out.println("Clicked SUBMIT button");
    }

    /*
             =====================================================================
             STEP 6: APPLY FOR OC NEW BUILDING PLAN PERMIT
             =====================================================================
            */

    private void clickApplyForOcNewBuildingPermit(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        WebElement applyBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[.//header[contains(text(),'Apply for OC for New Construction')]]")
                )
        );

        Thread.sleep(3000);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applyBtn);
        Thread.sleep(300);

        applyBtn.click();

        System.out.println("Clicked Apply for Building Plan Permit");
    }

    /*
             =====================================================================
             STEP 7: INFO PAGE DETAILS
             =====================================================================
            */

    private void infoPageDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("OBPAS Info Page - Clicking Next");
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

    /*
             =====================================================================
             STEP 8: BASIC DETAILS PAGE
             =====================================================================
            */

    private void basicDetailPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Basic Details Page - Clicking Next");
        Thread.sleep(2000);

        // Try multiple Next button selectors for Basic Detail page
        By[] nextSelectors = {
                By.xpath("//button[contains(.,'Next')]")

        };

        for (By selector : nextSelectors) {
            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(selector));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", nextBtn);
                System.out.println("Clicked Next on Basic Details Page");
                return;
            } catch (Exception e) {
                System.out.println("Next selector failed: " + selector);

            }
        }

        Thread.sleep(2000);

    }

    /*
             =====================================================================
             STEP 9: PLOT DETAILS PAGE
             =====================================================================
            */

    private void plotDetailPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{
        System.out.println("Fill Plot Details Page");

        fillInput(wait, "holdingNumber", "Test1234");
        Thread.sleep(500);

        WebElement landRegistrationTextarea = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("textarea.card-textarea")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", landRegistrationTextarea);
        Thread.sleep(300);

        landRegistrationTextarea.click();
        landRegistrationTextarea.clear();
        landRegistrationTextarea.sendKeys("Test Land Registration");

        js.executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", landRegistrationTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", landRegistrationTextarea);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur',{bubbles:true}));", landRegistrationTextarea);

        Thread.sleep(3000);

        clickNextButton(driver, wait, js);

        System.out.println("Landmark submitted successfully");

    }

    /*
             =====================================================================
             STEP 10: SCRUTINY DETAILS
             =====================================================================
            */

    private void scrutinyDetailPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{
        System.out.println("Fill Scrutiny Details Page- Click Next Button");

        By[] nextSelectors = {
                By.xpath("//button[contains(.,'Next')]"),

        };

        for (By selector : nextSelectors) {
            try {
                WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(selector));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", nextBtn);
                System.out.println("Clicked Next on Scrutiny Detail Page");
                return;
            } catch (Exception e) {
                System.out.println("Next selector failed: " + selector);
            }
        }

        Thread.sleep(3000);

    }

    /*
             =====================================================================
             STEP 11: DOCUMENT DETAILS
           =====================================================================
            */

    private void uploadDocumentPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        System.out.println("Uploading Documents");
        Thread.sleep(2000);

        // Document 0: Identity proof
        selectDropdownByIndex(driver, wait, js, 0, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);

        // Document 1: Address proof
        selectDropdownByIndex(driver, wait, js, 1, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 1, ConfigReader.get("document.aDdress.proof"));
        Thread.sleep(2000);

        // Document 2: Land Tax Receipt
        selectDropdownByIndex(driver, wait, js, 2, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.landTax.proof"));
        Thread.sleep(2000);

        // Document 3: Title deed
        selectDropdownByIndex(driver, wait, js, 3, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 3, ConfigReader.get("document.titleDeedOfTheProperty.proof"));
        Thread.sleep(2000);

        // Document 4: Layout Approval
        selectDropdownByIndex(driver, wait, js, 4, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 4, ConfigReader.get("document.layoutApprovalCopy.proof"));
        Thread.sleep(2000);

        // Document 5: Structural Stability
        selectDropdownByIndex(driver, wait, js, 5, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 5, ConfigReader.get("document.structuralStabilityCertificate.proof"));
        Thread.sleep(2000);

        // Document 6: Location Sketch
        selectDropdownByIndex(driver, wait, js, 6, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 6, ConfigReader.get("document.locationSketchAndVillageSketch.proof"));
        Thread.sleep(2000);

        // Document 7: Site Plan
        selectDropdownByIndex(driver, wait, js, 7, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 7, ConfigReader.get("document.sitePlan.proof"));
        Thread.sleep(2000);

        // Document 8: Floor Plan
        selectDropdownByIndex(driver, wait, js, 8, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 8, ConfigReader.get("document.floorPlan.proof"));
        Thread.sleep(2000);

        // Document 9: Service Plan
        selectDropdownByIndex(driver, wait, js, 9, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 9, ConfigReader.get("document.servicePlan.proof"));
        Thread.sleep(2000);

        // Document 10: Elevation Plan
        selectDropdownByIndex(driver, wait, js, 10, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 10, ConfigReader.get("document.elevationPlan.proof"));
        Thread.sleep(2000);

        // Document 11: Section Plan
        selectDropdownByIndex(driver, wait, js, 11, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 11, ConfigReader.get("document.sectionPlan.proof"));
        Thread.sleep(2000);

        // Document 12: Parking Plan
        selectDropdownByIndex(driver, wait, js, 12, 0);
        Thread.sleep(1000);
        uploadFile(driver, wait, js, 12, ConfigReader.get("document.parkingPlan.proof"));
        Thread.sleep(3000);

        System.out.println("All documents uploaded, waiting for Next button...");
        Thread.sleep(5000);

        // Wait for Next button to be enabled
        wait.until(driver1 -> {
            WebElement btn = driver.findElement(By.xpath("//button[contains(.,'Next')]"));
            return btn.isEnabled() && !btn.getAttribute("class").contains("disabled");
        });

        WebElement nextBtn = driver.findElement(By.xpath("//button[contains(., 'Next')]"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", nextBtn);
        System.out.println("Clicked Next button");
        Thread.sleep(2000);
    }

    /*
             =====================================================================
             STEP 12: NOC DETAILS
             =====================================================================
            */

    private void nocUploadPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        System.out.println("NOC Upload Page - Clicking Next");
        Thread.sleep(2000);

        //Airport NOC
        uploadFile(driver, wait, js, 0, ConfigReader.get("document.iDentity.proof"));
        Thread.sleep(2000);


        //Fire NOC
        uploadFile(driver, wait, js, 2, ConfigReader.get("document.landTax.proof"));
        Thread.sleep(2000);

        // Try multiple Next button selectors for NOC Upload page
        By[] nextSelectors = {By.xpath("//button[contains(.,'Next')]"),

        };

        for (By selector : nextSelectors) {
            try {               WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(selector));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", nextBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", nextBtn);
                System.out.println("Clicked Next on NOC Upload Page");
                return;
            } catch (Exception e) {
                System.out.println("Next selector failed: " + selector);
            }
        }

        Thread.sleep(2000);

    }

             /*
             =====================================================================
             STEP 13: SUMMARY PAGE
             =====================================================================
             */

    private void summaryPage(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException{

        System.out.println("Summary Page - Looking for Submit button");
        Thread.sleep(3000);

        // Try multiple button selectors for summary page
        String[] buttonTexts = {"Submit", "Send to Citizen", "Send Application", "Apply", "Confirm"};

        for (String buttonText : buttonTexts) {
            try {
                List<WebElement> buttons = driver.findElements(
                        By.xpath("//button[contains(., '" + buttonText + "')]")
                );

                if (!buttons.isEmpty()) {
                    System.out.println("Found " + buttons.size() + " buttons with text: " + buttonText);

                    for (WebElement btn : buttons) {
                        if (btn.isDisplayed() && btn.isEnabled()) {
                            js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
                            Thread.sleep(500);
                            js.executeScript("arguments[0].click();", btn);
                            System.out.println("Clicked " + buttonText + " button on Summary page");
                            Thread.sleep(2000);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Button '" + buttonText + "' not found");
            }
        }

        // If no specific button found, try generic submit button
        try {
            WebElement submitBtn = driver.findElement(By.xpath("//button[@type='submit' or @type='button']"));
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", submitBtn);
            System.out.println("Clicked generic submit button");
        } catch (Exception e) {
            System.out.println("No submit button found on summary page");
        }
    }

    /*
             =====================================================================
             STEP 16: LOGOUT
             =====================================================================
             */

    private void logout(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Logout Flow");


        // Go back to Home

        try {
            WebElement homeBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(.,'Go back to home')]")
                    )
            );

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", homeBtn);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", homeBtn);

            System.out.println("Clicked Go back to Home");
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Go back to Home button not found (maybe already on home)");
        }

        // sidebar logout

        WebElement logoutLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[contains(@href,'logout')] | //*[contains(text(),'Logout')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", logoutLink);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", logoutLink);

        System.out.println("Clicked Logout in sidebar");
        Thread.sleep(1500);

        // Confirm popup
        WebElement confirmLogout = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(.,'Yes')]")
                )
        );

        js.executeScript("arguments[0].click();", confirmLogout);

        System.out.println("Confirmed Logout");
        Thread.sleep(2000);
    }


             /*
             =====================================================================
             STEP 17: CITIZEN LOGIN
             =====================================================================
             */

    private void performCitizenLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
            throws InterruptedException {

        driver.get(baseUrl);
        System.out.println("Open the Citizen Login Portal");

        // Mobile number
        fillInput(wait, "mobileNumber", "9999999999");

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
             STEP 18: NAVIGATE TO OBPAS MODULE
             =====================================================================
             */

    private void navigateToOBPAS1(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to OBPAS");

        // Sidebar Property Tax link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@href='/upyog-ui/citizen/obps-home']"))));

        Thread.sleep(2000);
        System.out.println("Reached OBPAS home page");

        // "View Application By Citizen" link
        js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[@href='/upyog-ui/citizen/obps/my-applications']"))));

        System.out.println("Clicked View Application By Citizen link");
        Thread.sleep(2000);

    }

            /*
             =====================================================================
             STEP 19: MY APPLICATIONS
             =====================================================================
             */

    private void myApplications(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("On My Applications Page");

        wait.until(ExpectedConditions.urlContains("my-applications"));

        WebElement viewDetailsBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(.,'VIEW DETAILS')]")
                )
        );

        Thread.sleep(3000);

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", viewDetailsBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", viewDetailsBtn);

        System.out.println("Clicked View Details");
    }

             /*
             =====================================================================
             STEP 20: MY APPLICATIONS
             =====================================================================
             */


    private void citizenApproveFlow(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Citizen Approval Flow");

        // Scroll to bottom
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(1500);

        // Locate checkbox near Terms text
        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//*[contains(text(),'Terms and Conditions')]/preceding::input[@type='checkbox'][1]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", checkbox);
        Thread.sleep(500);

        // Click using JS (React safe)
        js.executeScript("arguments[0].click();", checkbox);
        System.out.println("Checkbox selected");

        Thread.sleep(1000);

        // Click Take Action button
        WebElement takeAction = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(.,'Take action')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", takeAction);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", takeAction);
        System.out.println("Clicked Take Action");

        Thread.sleep(1500);

// Wait for menu-wrap container first (NOT approve directly)
        WebElement menuWrap = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'menu-wrap')]")
                )
        );

        System.out.println("Dropdown menu visible");

// Now find APPROVE inside that container
        WebElement approveOption = menuWrap.findElement(
                By.xpath(".//div[contains(normalize-space(),'APPROVE')]")
        );

// Scroll & click
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", approveOption);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", approveOption);

        System.out.println("Clicked APPROVE successfully");

    }

             /*
             =====================================================================
             STEP 21: APPROVE POPUP
             =====================================================================
             */

    private void applicationPopup(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Handling Forward Application Popup");

        //  Wait for popup textarea
        WebElement commentBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//textarea[@name='comments']")
                )
        );

        // Scroll into view
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        //  Clear & enter comment
        commentBox.clear();
        commentBox.sendKeys("Approve");
        System.out.println("Entered comment: Approve");

        Thread.sleep(500);

        //  Click APPROVE button inside popup
        WebElement approveBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'popup-module')]//button[contains(.,'APPROVE')]")
                )
        );

        js.executeScript("arguments[0].click();", approveBtn);

        System.out.println("Clicked APPROVE button in popup");

        Thread.sleep(2000);
    }

     /*
             =====================================================================
             STEP 22: LOGOUT AGAIN
             =====================================================================
             */

    private void logoutAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Starting Logout Flow");


        // Go back to Home

        try {
            WebElement homeBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(.,'Go back to home')]")
                    )
            );

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", homeBtn);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", homeBtn);

            System.out.println("Clicked Go back to Home");
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Go back to Home button not found (maybe already on home)");
        }

        // sidebar logout

        WebElement logoutLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[contains(@href,'logout')] | //*[contains(text(),'Logout')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", logoutLink);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", logoutLink);

        System.out.println("Clicked Logout in sidebar");
        Thread.sleep(1500);

        // Confirm popup
        WebElement confirmLogout = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(.,'Yes')]")
                )
        );

        js.executeScript("arguments[0].click();", confirmLogout);

        System.out.println("Confirmed Logout");
        Thread.sleep(2000);
    }

    /*
    =====================================================================
    STEP 23: ARCHITECT LOGIN
    =====================================================================
    */
    private void performArchitectLoginAgain(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions, String baseUrl, String mobileNumber, String otp, String cityName)
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


             /*
             =====================================================================
             STEP 24: NAVIGATE TO OBPAS MODULE AGAIN
             =====================================================================
             */

    private void navigateToOBPAS2(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Navigating to OBPAS");

        // Step 1: Click OBPAS Home from sidebar
        WebElement obpasHome = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[@href='/upyog-ui/citizen/obps-home']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", obpasHome);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", obpasHome);

        System.out.println("Reached OBPAS home page");
        wait.until(ExpectedConditions.urlContains("obps-home"));
        Thread.sleep(1500);

        WebElement architectLogin = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[contains(text(),'Architect Login')]")
                )
        );

        js.executeScript("arguments[0].click();", architectLogin);

        // Step 2: Click OBPAS Inbox (View Application By Citizen)
        WebElement inboxLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[@href='/upyog-ui/citizen/obps/bpa/inbox']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", inboxLink);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", inboxLink);

        System.out.println("Clicked View Application By Citizen link");

        wait.until(ExpectedConditions.urlContains("inbox"));
        Thread.sleep(2000);
    }


             /*
             =====================================================================
             STEP 25: OBPAS INBOX
             =====================================================================
             */

    private void openFirstApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("Opening first application from Inbox");

        WebElement applicationLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(@href,'/obps/bpa/PG-BP')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", applicationLink);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", applicationLink);

        System.out.println("Clicked Application Number");
    }

             /*
             =====================================================================
             STEP 26: SUBMIT APPLICATION
             =====================================================================
             */


    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {

        System.out.println("On Application Details Page");

        // Wait for page load
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(),'Application Timeline')]")
        ));

        // Scroll to bottom
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(1500);

        // Checkbox select
        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//input[@type='checkbox']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", checkbox);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", checkbox);

        System.out.println("Checkbox selected");

        Thread.sleep(800);

        // Click bottom Submit
        WebElement submitBtn = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//button[contains(.,'Submit')]")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(500);
        js.executeScript("arguments[0].click();", submitBtn);

        System.out.println("Clicked bottom Submit button");

        Thread.sleep(1500);


        // HANDLE FORWARD APPLICATION POPUP

        System.out.println("Handling Forward Application popup");

        // Wait for popup textarea
        WebElement commentBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//textarea[@name='comments']")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", commentBox);
        Thread.sleep(500);

        // Enter comment
        js.executeScript(
                "arguments[0].value='Approved by Citizen'; arguments[0].dispatchEvent(new Event('input',{bubbles:true}));",
                commentBox
        );

        System.out.println("Entered popup comment");

        Thread.sleep(800);

        // Click popup Submit button
        WebElement popupSubmit = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'popup-module')]//button[contains(.,'Submit')]")
                )
        );

        js.executeScript("arguments[0].click();", popupSubmit);

        System.out.println("Clicked popup Submit button");

        Thread.sleep(2000);
    }


             /*
             =====================================================================
             STEP 27: PAYMENT
             =====================================================================
             */

    private void handlePaymentFlow(WebDriver driver, WebDriverWait wait, JavascriptExecutor js)
            throws InterruptedException {
        System.out.println("Starting payment flow (Card → Pay Now → Success)...");

        // remember UPYOG window
        String mainHandle = driver.getWindowHandle();

        // -----------------------------
        // STEP 1: "Make Payment" (ack page)
        // -----------------------------
        try {
            By paySel = By.xpath(
                    "//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'pay')]");
            boolean clicked = tryClickWithRetries(driver, wait, js, paySel, 25, 4, 700);
            if (clicked) {
                System.out.println("Clicked 'Pay'");
                Thread.sleep(1000);
            } else {
                System.out.println("'Pay' button not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            System.out.println("'Pay' error: " + e.getMessage());
        }

        // -----------------------------
        // STEP 2: "Proceed To Pay" (Tax Bill Details page)
        // -----------------------------
        try {
            By pay1Sel = By.xpath(
                    "//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'pay')]");
            boolean proceedClicked = tryClickWithRetries(driver, wait, js, pay1Sel, 40, 5, 800);
            if (proceedClicked) {
                System.out.println("Clicked 'Pay'");
                Thread.sleep(2000);
            } else {
                System.out.println("'Pay' not found or not clickable, continuing...");
            }
        } catch (Exception e) {
            System.out.println("'Pay' error: " + e.getMessage());
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
                } catch (Exception ignored) {}
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
                System.out.println("⚠ Pay button NOT FOUND on gateway.");
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
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        System.out.println("Payment flow finished (Card route).");
        Thread.sleep(5000);
        // ===============================
        // WAIT FOR ACKNOWLEDGEMENT PAGE
        // ===============================
        System.out.println("Waiting for acknowledgement page...");

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("acknowledgement"),
                ExpectedConditions.urlContains("success")
        ));

// Extra stability
        Thread.sleep(3000);

// Debug
        System.out.println("Final URL: " + driver.getCurrentUrl());
        System.out.println("Final Title: " + driver.getTitle());
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

        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));

        if (index >= fileInputs.size()) {
            System.out.println("ERROR: File input index " + index + " not found");
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        js.executeScript(
                "arguments[0].style.cssText = 'display:block !important; visibility:visible !important; opacity:1 !important;';",
                fileInput
        );
        Thread.sleep(300);

        fileInput.sendKeys(filePath);
        System.out.println("✓ Uploaded file at index " + index);

        js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fileInput);
        Thread.sleep(500);
    }



    private void uploadDocumentByLabel(WebDriver driver,
                                       JavascriptExecutor js,
                                       String labelText,
                                       String filePath) throws InterruptedException {

        System.out.println("Uploading for: " + labelText);

        // Find document card by label
        WebElement docSection = driver.findElement(By.xpath(
                "//div[contains(@class,'upload-file')]//preceding::h2[contains(.,'"
                        + labelText + "')][1]/ancestor::div[contains(@class,'card')]"
        ));

        WebElement fileInput = docSection.findElement(By.xpath(".//input[@type='file']"));

        js.executeScript(
                "arguments[0].style.cssText = 'display:block !important; visibility:visible !important; opacity:1 !important; position:relative !important;';",
                fileInput
        );

        Thread.sleep(500);

        fileInput.sendKeys(filePath);

        js.executeScript(
                "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                fileInput
        );

        Thread.sleep(1500);

        System.out.println("Uploaded file for " + labelText);
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

        // 🔥 CLICK PARENT, NOT INPUT
        WebElement clickable = radio.findElement(By.xpath(".."));
        js.executeScript("arguments[0].click();", clickable);

        System.out.println("Selected radio index: " + index);
        Thread.sleep(500);
    }

    private void uploadDxf(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                           int index, String filePath) throws InterruptedException {

        System.out.println("File path to upload: " + filePath);

        Thread.sleep(1000);

        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input[type='file']"));
        System.out.println("Total file inputs found: " + fileInputs.size());

        if (fileInputs.isEmpty() || index >= fileInputs.size()) {
            System.out.println("ERROR: File input not found at index " + index);
            return;
        }

        WebElement fileInput = fileInputs.get(index);

        js.executeScript(
                "arguments[0].style.cssText = 'display:block !important; visibility:visible !important; opacity:1 !important; position:relative !important; width:100px !important; height:30px !important;';",
                fileInput
        );

        Thread.sleep(500);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", fileInput);
        Thread.sleep(300);

        try {
            fileInput.sendKeys(filePath);
            System.out.println("File uploaded successfully");
        } catch (Exception e) {
            System.out.println("ERROR uploading file: " + e.getMessage());
        }

        Thread.sleep(1000);
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
}
