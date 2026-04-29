package org.upyog.Automation.Modules.Pet;

import java.io.File;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
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


@Component
public class PetCreateApplication {

    private static final Logger logger = LoggerFactory.getLogger(PetCreateApplication.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    /**
     * Main test method that executes the complete pet registration workflow
     * Runs automatically when Spring context is initialized
     */
    //@PostConstruct
    public void testingPetApp() {
        petApptest(ConfigReader.get("citizen.base.url"),
                   "Pet Registration",
                   ConfigReader.get("citizen.mobile.number"),
                   ConfigReader.get("test.otp"),
                   ConfigReader.get("test.city.name"));
    }

    public void petApptest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Pet Registration by Citizen");

        WebDriver driver = null;
        try {
            // Initialize WebDriver using WebDriverFactory
            driver = webDriverFactory.createDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Actions actions = new Actions(driver);
            // STEP 1: User Login Process
            driver.get(baseUrl);
            
            // Enter mobile number
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("mobileNumber")))
                .sendKeys(mobileNumber);

            // Accept terms and conditions checkbox
            WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[type='checkbox'].form-field")));
            if (!checkbox.isSelected()) {
                js.executeScript("arguments[0].click();", checkbox);
                wait.until(ExpectedConditions.elementToBeSelected(checkbox));
            }

            // Click Next to proceed to OTP screen
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='submit']//header[text()='Next']/.."))).click();

            // STEP 2: OTP Verification
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.input-otp-wrap")));
            List<WebElement> otpInputs = driver.findElements(By.cssSelector("input.input-otp"));
            // Fill each OTP digit in separate input fields
            for (int i = 0; i < otp.length() && i < otpInputs.size(); i++) {
                otpInputs.get(i).sendKeys(String.valueOf(otp.charAt(i)));
            }

            // Submit OTP and proceed
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='submit']//header[text()='Next']/.."))).click();

            // STEP 3: City Selection
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper")));
            
            List<WebElement> cityOptions = driver.findElements(
                    By.cssSelector("div.radio-wrap.reverse-radio-selection-wrapper div"));
            
            // Find and select the specified city
            for (WebElement option : cityOptions) {
                WebElement label = option.findElement(By.tagName("label"));
                if (label.getText().trim().equals(cityName)) {
                    WebElement radioInput = option.findElement(By.cssSelector("input[type='radio']"));
                    if (!radioInput.isSelected()) {
                        js.executeScript("arguments[0].click();", radioInput);
                        wait.until(ExpectedConditions.elementToBeSelected(radioInput));
                    }
                    break;
                }
            }

            // Continue to main dashboard
            WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'submit-bar') and contains(., 'Continue')]")));
            js.executeScript("arguments[0].scrollIntoView(true);", continueBtn);
            actions.moveToElement(continueBtn).click().perform();

            // STEP 4: Navigate to Pet Registration Module
            js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@href='/digit-ui/citizen/ptr-home']"))));

            // Click on Register Pet option
            js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'CitizenHomeCard')]//a[text()='Register Pet']"))));

            // Skip document requirements page
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span/button[contains(@class,'submit-bar')]//header[normalize-space()='Next']/..")));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextBtn);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", nextBtn);

            // STEP 5: Fill Owner Details
            fillInput(wait, "fatherName", "Ramesh Singh");
            fillInput(wait, "emailId", "ramesh@gmail.com");
            Thread.sleep(700);

            // Proceed to pet details page
            WebElement nextBtnOwner = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class,'submit-bar')]//header[normalize-space()='Next']/..")));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", nextBtnOwner);
            Thread.sleep(300);
            nextBtnOwner.click();

            // STEP 6: Fill Pet Details
            selectDropdownOption(driver, wait, actions, 0); // Pet Type (Dog/Cat)
            selectDropdownOption(driver, wait, actions, 1); // Breed Type
            selectDropdownOption(driver, wait, actions, 2); // Gender
            selectDropdownOption(driver, wait, actions, 3); // Color

            // Select birth/adoption option (first radio button)
            List<WebElement> birthAdoptionRadios = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("input[type='radio'][name='selectBirthAdoption']")));
            if (!birthAdoptionRadios.isEmpty() && !birthAdoptionRadios.get(0).isSelected()) {
                js.executeScript("arguments[0].click();", birthAdoptionRadios.get(0));
                wait.until(ExpectedConditions.elementToBeSelected(birthAdoptionRadios.get(0)));
            }

            // Fill pet information fields
            fillInput(wait, "birthDate", "20-05-2025");
            fillInput(wait, "petName", "cherry");
            fillInput(wait, "identificationMark", "black color");
            fillInput(wait, "petAge", "5");
            fillInput(wait, "doctorName", "kdjfksdfsf");
            fillInput(wait, "clinicName", "sdjhfsjfsfsg");
            
            // Fill vaccination date with special handling
            WebElement vaccinationDate = wait.until(ExpectedConditions.elementToBeClickable(By.name("lastVaccineDate")));
            vaccinationDate.clear();
            vaccinationDate.sendKeys("20-05-2025");
            vaccinationDate.sendKeys(Keys.TAB);
            Thread.sleep(1000);

            fillInput(wait, "vaccinationNumber", "34545645");

            // Proceed to property details
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[type='submit']:not([disabled])"))).click();

            // STEP 7: Fill Property Details
            fillInput(wait, "propertyId", "435345");
            fillInput(wait, "buildingName", "gdfgdgdgdfg");
            fillInput(wait, "doorNo", "4534");

            // Select property location dropdowns
            selectDropdownOption(driver, wait, actions, 0); // City
            fillInput(wait, "street", "lajpat");
            selectDropdownOption(driver, wait, actions, 1); // Mohalla/Area
            fillInput(wait, "pincode", "110011");
            selectDropdownOption(driver, wait, actions, 2); // Fire Station
            fillInput(wait, "gisCode", "2343453");

            // Proceed to address details
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[type='submit']:not([disabled])"))).click();

            // STEP 8: Fill Address Details
            fillInput(wait, "houseNo", "2342");
            fillInput(wait, "houseName", "My House");
            fillInput(wait, "streetName", "Main Street");
            fillInput(wait, "addressline1", "123 Main Street, Apt 4B");
            fillInput(wait, "addressline2", "Near Central Park");
            fillInput(wait, "landmark", "Opposite City Mall");

            // Use independent method for city/locality selection (handles timing issues)
            selectAddressCityAndLocality(driver, wait, actions, js);
            fillInput(wait, "pincode", "112233");

            // Proceed to document upload
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[type='submit']:not([disabled])"))).click();

            // STEP 9: Select Identity Proof Type
            selectDropdownOption(driver, wait, actions, 0);

            // STEP 10: Upload Required Documents
            uploadDocuments(driver, wait, js);

            // Proceed to final submission
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[type='submit']:not([disabled])"))).click();

            // STEP 11: Declaration and Final Submission
            WebElement declarationCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[type='checkbox'][value^='I declare']")));
            js.executeScript("arguments[0].scrollIntoView(true);", declarationCheckbox);
            Thread.sleep(300);
            if (!declarationCheckbox.isSelected()) {
                declarationCheckbox.click();
            }

            // Submit the application
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@type='button' and contains(@class,'submit-bar')][header='Submit' or .//header[text()='Submit']]")));
            js.executeScript("arguments[0].scrollIntoView(true);", submitButton);
            Thread.sleep(300);
            submitButton.click();

            // STEP 12: Navigate to Application Tracking
            // Return to home page
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[@href='/digit-ui/citizen']//span[contains(text(),'Go back to home page')]"))).click();

            // Go back to Pet Registration module
            js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@href='/digit-ui/citizen/ptr-home']"))));

            // Access My Applications section
            js.executeScript("arguments[0].click();", wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'CitizenHomeCard')]//a[text()='My Applications']"))));

            // Track the submitted application
            WebElement trackButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'submit-bar') and @type='button' and .//header[normalize-space()='Track']]")));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", trackButton);
            Thread.sleep(300);
            trackButton.click();

            logger.info("Test completed successfully!");
            Thread.sleep(5000); // Brief pause to observe results
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                logger.info("WebDriver closed successfully");
            }
        }
    }



    /**
     * Utility method to fill input fields
     * @param wait WebDriverWait instance
     * @param fieldName Name attribute of the input field
     * @param value Value to enter in the field
     */
    private void fillInput(WebDriverWait wait, String fieldName, String value) {
        try {
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
            ((JavascriptExecutor) wait.until(d -> d)).executeScript("arguments[0].scrollIntoView(true);", input);
            input.clear();
            input.sendKeys(value);
            logger.info("Filled field '" + fieldName + "' with value: " + value);
        } catch (Exception e) {
            System.err.println("Error filling field '" + fieldName + "': " + e.getMessage());
            throw new RuntimeException("Failed to fill input field: " + fieldName, e);
        }
    }

    /**
     * Generic method to select first option from dropdown menus
     * @param driver WebDriver instance
     * @param wait WebDriverWait instance
     * @param actions Actions instance for mouse operations
     * @param dropdownIndex Index of dropdown to select (0-based)
     */
    private void selectDropdownOption(WebDriver driver, WebDriverWait wait, Actions actions, int dropdownIndex) {
        try {
            List<WebElement> dropdowns = driver.findElements(By.cssSelector("div.select"));
            if (dropdowns.size() > dropdownIndex) {
                WebElement dropdown = dropdowns.get(dropdownIndex);
                WebElement button = dropdown.findElement(By.tagName("svg"));
                
                // Scroll to element and click
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                wait.until(ExpectedConditions.elementToBeClickable(button));
                actions.moveToElement(button).click().perform();
                
                // Wait for dropdown to open
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));
                
                WebElement firstOption = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#jk-dropdown-unique .profile-dropdown--item:first-child")));
                actions.moveToElement(firstOption).click().perform();
                
                // Wait for dropdown to close
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));
            }
        } catch (Exception e) {
            System.err.println("Error selecting dropdown option at index " + dropdownIndex + ": " + e.getMessage());
            throw new RuntimeException("Failed to select dropdown option", e);
        }
    }

    /**
     * Independent method for city and locality selection in address details
     * Handles timing issues that occur after landmark field input
     * Uses robust element detection and proper wait strategies
     * @param driver WebDriver instance
     * @param wait WebDriverWait instance
     * @param actions Actions instance for mouse operations
     * @param js JavascriptExecutor for scrolling operations
     */
    private void selectAddressCityAndLocality(WebDriver driver, WebDriverWait wait, Actions actions, JavascriptExecutor js) {
        try {
            // Wait for page stabilization after landmark input
            Thread.sleep(1000);
            
            // CITY SELECTION - Robust approach with multiple selectors
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.select input.employee-select-wrap--elipses")));
            
            WebElement cityContainer = driver.findElement(By.cssSelector("div.select"));
            js.executeScript("arguments[0].scrollIntoView(true);", cityContainer);
            Thread.sleep(500);
            
            // Click city dropdown
            WebElement cityDropdownToggle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.select svg.cp")));
            actions.moveToElement(cityDropdownToggle).click().perform();
            
            // Wait for options and select first city
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));
            WebElement cityOption = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@id='jk-dropdown-unique']//div[contains(@class,'profile-dropdown--item')][1]")));
            cityOption.click();
            logger.info("Selected first available city option");
            
            // Wait for city dropdown to close
            Thread.sleep(500);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));
            
            // LOCALITY SELECTION - Handle second dropdown
            List<WebElement> allDropdowns = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.select")));
            
            if (allDropdowns.size() > 1) {
                WebElement localityDropdownContainer = allDropdowns.get(1);
                js.executeScript("arguments[0].scrollIntoView(true);", localityDropdownContainer);
                Thread.sleep(500);
                
                // Click locality dropdown
                WebElement localityArrow = localityDropdownContainer.findElement(By.cssSelector("svg.cp"));
                actions.moveToElement(localityArrow).click().perform();
                
                // Wait for options and select first locality
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));
                WebElement localityOption = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[@id='jk-dropdown-unique']//div[contains(@class,'profile-dropdown--item')][1]")));
                localityOption.click();
                logger.info("Selected first available locality option");
                
                // Wait for locality dropdown to close
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));
            }
            
            logger.info("City and locality selections completed!");
            
        } catch (Exception e) {
            logger.info("Error in address city/locality selection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles document upload for all required files
     * Uploads identity proof, ownership proof, vaccination certificate, fitness certificate, and pet photo
     * @param driver WebDriver instance
     * @param wait WebDriverWait instance
     * @param js JavascriptExecutor for scrolling operations
     */
    private void uploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        // File paths for required documents from configuration
        String[] filePaths = {
            ConfigReader.get("document.identity.proof"),    // Identity proof
            ConfigReader.get("document.owner.proof"),      // Ownership proof
            ConfigReader.get("document.vaccination.cert"), // Vaccination certificate
            ConfigReader.get("document.fitness.cert")      // Fitness certificate
        };

        // Upload documents using file input elements
        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input.input-mirror-selector-button[type='file']"));
        
        for (int i = 0; i < Math.min(filePaths.length, fileInputs.size()); i++) {
            try {
                WebElement fileInput = fileInputs.get(i);
                js.executeScript("arguments[0].scrollIntoView(true);", fileInput);
                Thread.sleep(100);
                fileInput.sendKeys(getAbsolutePath(filePaths[i]));
                Thread.sleep(100);
            } catch (Exception e) {
                logger.info("Error uploading file " + i + ": " + e.getMessage());
            }
        }

        // Upload pet photo separately (different input element)
        try {
            WebElement petPhotoInput = driver.findElement(By.cssSelector("input[type='file']#upload"));
            js.executeScript("arguments[0].scrollIntoView(true);", petPhotoInput);
            Thread.sleep(100);
            petPhotoInput.sendKeys(getAbsolutePath(ConfigReader.get("document.pet.photo")));
            Thread.sleep(100);
        } catch (Exception e) {
            logger.info("Error uploading pet photo: " + e.getMessage());
        }
    }

    /**
     * Converts relative path to absolute path for file uploads
     * Selenium requires absolute paths for sendKeys() on file inputs
     */
    private String getAbsolutePath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        File file = new File(relativePath);
        if (!file.exists()) {
            // Try to find file in resources directory
            try {
                String resourcePath = relativePath.replace("src/main/resources/", "");
                java.net.URL resource = getClass().getClassLoader().getResource(resourcePath);
                if (resource != null) {
                    file = new File(resource.getPath());
                }
            } catch (Exception e) {
                System.err.println("Error resolving resource path: " + e.getMessage());
            }
            
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + relativePath);
            }
        }
        
        logger.info("Using file: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }
}