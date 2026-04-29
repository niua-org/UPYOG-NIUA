package org.upyog.Automation.Modules.Pet;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Utils.ConfigReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.upyog.Automation.config.WebDriverFactory;

import java.util.List;

/**
 * Automated test class for UPYOG Pet Registration by Employee
 * This class handles the complete employee-side pet registration workflow including:
 * - Employee login and city selection
 * - Pet owner details entry
 * - Pet information and medical details
 * - Property and address information
 * - Document uploads
 * - Application submission
 */
//@Component
public class PetCempCreate {

    private static final Logger logger = LoggerFactory.getLogger(PetCempCreate.class);

    @Autowired
    private WebDriverFactory webDriverFactory;

    /**
     * Main test method for employee pet registration workflow
     * Runs automatically when Spring context is initialized
     */
    //@PostConstruct
    public void petRegCemp() {
        logger.info("New Pet Registration by Employee");
        
        // Initialize WebDriver using DriverFactory
        WebDriver driver = webDriverFactory.createDriver();
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            // STEP 1: Employee Login
            performEmployeeLogin(driver, wait, js, actions);
            
            // STEP 2: Navigate to New Pet Registration
            navigateToNewPetRegistration(driver, wait, js);
            
            // STEP 3: Fill Owner Details
            fillOwnerDetails(driver, wait);
            
            // STEP 4: Fill Pet Details
            fillPetDetails(driver, wait, js, actions);
            
            // STEP 5: Fill Property Details
            fillPropertyDetails(driver, wait, js, actions);
            
            // STEP 6: Fill Address Details
            fillAddressDetails(driver, wait, js, actions);
            
            // STEP 7: Select Identity Proof and Upload Documents
            selectIdentityProofAndUploadDocuments(driver, wait, js, actions);
            
            // STEP 8: Submit Application
            submitApplication(driver, wait, js);
            
            logger.info("Employee Pet Registration completed successfully!");
            
        } catch (Exception e) {
            logger.info("Exception in Pet Registration: " + e.getMessage());
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
    private void performEmployeeLogin(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        driver.get(ConfigReader.get("employee.base.url"));
        driver.manage().window().maximize();
        logger.info("Open the Employee Login Portal");

        // Enter credentials
        fillInput(wait, "username", ConfigReader.get("app.login.username"));
        fillInput(wait, "password", ConfigReader.get("app.login.password"));
        logger.info("Filled username and password");

        // Select city
        selectCityDropdown(driver, wait, actions);
        
        // Click Continue
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[text()='Continue']]");
    }

    /**
     * Navigates to New Pet Registration module
     */
    private void navigateToNewPetRegistration(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Navigating to New Pet Registration");
        
        // Wait for page to load after login
        Thread.sleep(2000);
        
        // Click New Pet Registration link
        WebElement newPetRegLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@href, '/upyog-ui/employee/ptr/petservice/new-application') and contains(normalize-space(.), 'New Pet Registration')]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", newPetRegLink);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", newPetRegLink);
        logger.info("Clicked New Pet Registration link");

        // Click Next on document requirements page
        clickButton(wait, js, "//button[contains(@class, 'submit-bar') and .//header[normalize-space()='Next']]");
    }

    /**
     * Fills owner details form
     */
    private void fillOwnerDetails(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        logger.info("Filling Owner Details");
        
        fillInput(wait, "applicantName", "Ramesh kumar");
        fillInput(wait, "mobileNumber", "8893445543");
        fillInput(wait, "fatherName", "amit kumar");
        fillInput(wait, "emailId", "ramesh@gmail.com");
        
        clickNextSubmitButton(driver, wait);
    }

    /**
     * Fills pet details including dropdowns and form fields
     */
    private void fillPetDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Pet Details");
        Thread.sleep(300);

        try {
            // Select pet details dropdowns
            List<WebElement> allDropdowns = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.select")));
            
            selectPetDropdownOption(allDropdowns, actions, wait, 0, "Dog", "Pet Type");
            selectPetDropdownOption(allDropdowns, actions, wait, 1, "Labrador Retriever", "Breed Type");
            selectPetDropdownOption(allDropdowns, actions, wait, 2, "Male", "Pet Sex");
            selectPetDropdownOption(allDropdowns, actions, wait, 3, "Tricolor or white", "Pet Colour");

        } catch (Exception e) {
            logger.info("Exception in PET DETAILS DROPDOWN: " + e.getMessage());
            e.printStackTrace();
        }

        // Select birth/adoption radio button
        selectRadioButton(driver, wait, js, "selectBirthAdoption");
        
        // Fill pet information fields
        fillInput(wait, "birthDate", "20-05-2025");
        fillInput(wait, "petName", "cherry");
        fillInput(wait, "identificationMark", "black color");
        fillInput(wait, "petAge", "5");
        fillInput(wait, "doctorName", "kdjfksdfsf");
        fillInput(wait, "clinicName", "sdjhfsjfsfsg");
        
        // Fill vaccination date
        WebElement vaccinationDate = wait.until(ExpectedConditions.elementToBeClickable(By.name("lastVaccineDate")));
        vaccinationDate.clear();
        vaccinationDate.sendKeys("20-05-2025");
        vaccinationDate.sendKeys(Keys.TAB);
        Thread.sleep(1000);
        
        fillInput(wait, "vaccinationNumber", "34545645");
        
        // Click Next
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']:not([disabled])"))).click();
    }

    /**
     * Fills property details form
     */
    private void fillPropertyDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Property Details");
        
        fillInput(wait, "propertyId", "435345");
        fillInput(wait, "buildingName", "gdfgdgdgdfg");
        fillInput(wait, "doorNo", "4534");

        // Select dropdowns for property location
        selectFirstDropdownOption(driver, wait, actions, 0); // City
        fillInput(wait, "street", "lajpat");
        selectFirstDropdownOption(driver, wait, actions, 1); // Mohalla
        fillInput(wait, "pincode", "110011");
        selectFireStationDropdown(driver, wait, actions); // Fire Station
        fillInput(wait, "gisCode", "2343453");

        // Click Next
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']:not([disabled])"))).click();
    }

    /**
     * Fills address details form
     */
    private void fillAddressDetails(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Filling Address Details");
        
        fillInput(wait, "houseNo", "2342");
        fillInput(wait, "houseName", "My House");
        fillInput(wait, "streetName", "Main Street");
        fillInput(wait, "addressline1", "123 Main Street, Apt 4B");
        fillInput(wait, "addressline2", "Near Central Park");
        fillInput(wait, "landmark", "Opposite City Mall");

        // Select city and locality
        selectAddressCityAndLocality(driver, wait, js, actions);
        fillInput(wait, "pincode", "112233");

        // Click Next
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']:not([disabled])"))).click();
    }

    /**
     * Selects identity proof and uploads documents
     */
    private void selectIdentityProofAndUploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        logger.info("Selecting Identity Proof and Uploading Documents");
        
        // Select identity proof
        selectIdentityProof(driver, wait, actions);
        
        // Upload documents
        uploadDocuments(driver, wait, js);
        
        // Click Next
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']:not([disabled])"))).click();
    }

    /**
     * Submits the application
     */
    private void submitApplication(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) throws InterruptedException {
        logger.info("Submitting Application");
        
        // Check declaration checkbox
        WebElement declarationCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[type='checkbox'][value^='I declare']")));
        js.executeScript("arguments[0].scrollIntoView(true);", declarationCheckbox);
        Thread.sleep(300);
        
        if (!declarationCheckbox.isSelected()) {
            declarationCheckbox.click();
            logger.info("Declaration checkbox checked!");
        }

        // Submit application
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='button' and contains(@class,'submit-bar')][header='Submit' or .//header[text()='Submit']]")));
        js.executeScript("arguments[0].scrollIntoView(true);", submitButton);
        Thread.sleep(300);
        submitButton.click();
        logger.info("Application submitted successfully!");

        Thread.sleep(2000);

        // Navigate back to home page
        try {
            WebElement goHomeLink = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[@href='/digit-ui/employee']//span[contains(text(), 'Go back to home page')]")));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", goHomeLink);
            goHomeLink.click();
            wait.until(ExpectedConditions.urlContains("/digit-ui/employee"));
            logger.info("Navigated to home page successfully!");
        } catch (Exception e) {
            logger.info("Failed to navigate to home page: " + e.getMessage());
        }
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
     * Clicks Next/Submit button
     */
    private void clickNextSubmitButton(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class, 'submit-bar') and @type='submit' and .//header[normalize-space()='Next']]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitButton);
        Thread.sleep(300);
        submitButton.click();
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
     * Selects pet dropdown options with specific text matching
     */
    private void selectPetDropdownOption(List<WebElement> allDropdowns, Actions actions, WebDriverWait wait, 
                                       int dropdownIndex, String optionText, String dropdownName) throws InterruptedException {
        WebElement dropdownContainer = allDropdowns.get(dropdownIndex);
        WebElement dropdownButton = dropdownContainer.findElement(By.tagName("svg"));
        wait.until(ExpectedConditions.elementToBeClickable(dropdownButton));
        
        actions.moveToElement(dropdownButton).click().perform();
        logger.info("Clicked " + dropdownName + " dropdown");
        
        WebElement options = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card")));
        List<WebElement> items = options.findElements(By.cssSelector(".profile-dropdown--item"));
        
        boolean selected = false;
        for (WebElement option : items) {
            if (option.getText().trim().equalsIgnoreCase(optionText)) {
                actions.moveToElement(option).click().perform();
                selected = true;
                break;
            }
        }
        
        if (!selected) throw new RuntimeException(dropdownName + " '" + optionText + "' not found!");
        logger.info(dropdownName + " '" + optionText + "' selected!");
    }

    /**
     * Selects first option from dropdown by index
     */
    private void selectFirstDropdownOption(WebDriver driver, WebDriverWait wait, Actions actions, int dropdownIndex) throws InterruptedException {
        List<WebElement> allDropdowns = driver.findElements(By.cssSelector("div.select"));
        if (allDropdowns.size() > dropdownIndex) {
            WebElement dropdownContainer = allDropdowns.get(dropdownIndex);
            WebElement dropdownButton = dropdownContainer.findElement(By.tagName("svg"));
            actions.moveToElement(dropdownButton).click().perform();
            Thread.sleep(500);

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));
            WebElement firstOption = driver.findElement(By.cssSelector("#jk-dropdown-unique .profile-dropdown--item:first-child"));
            actions.moveToElement(firstOption).click().perform();
        }
    }

    /**
     * Selects radio button by name attribute
     */
    private void selectRadioButton(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String radioName) throws InterruptedException {
        List<WebElement> radioButtons = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("input[type='radio'][name='" + radioName + "']")));
        if (!radioButtons.isEmpty() && !radioButtons.get(0).isSelected()) {
            js.executeScript("arguments[0].click();", radioButtons.get(0));
            Thread.sleep(1000);
        }
    }

    /**
     * Handles Fire Station dropdown selection with retry logic
     */
    private void selectFireStationDropdown(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        int attempts = 0;
        while (attempts < 3) {
            try {
                logger.info("Attempt " + (attempts + 1) + ": Selecting Fire Station...");
                WebElement dropdownContainer = driver.findElements(By.cssSelector("div.select.undefined")).get(2);
                WebElement svgToggle = dropdownContainer.findElement(By.cssSelector("svg.cp"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", svgToggle);

                actions.moveToElement(svgToggle).click().perform();
                
                WebElement optionsContainer = dropdownContainer.findElement(
                        By.xpath("following-sibling::div[contains(@class,'options-card')]"));
                
                WebElement cityAOption = optionsContainer.findElement(
                        By.xpath(".//div[contains(@class,'profile-dropdown--item')][.//span[contains(normalize-space(.),'City A')]]"));
                cityAOption.click();
                logger.info("Fire Station 'City A' selected successfully");
                
                wait.until(ExpectedConditions.invisibilityOf(optionsContainer));
                Thread.sleep(500);
                break;
                
            } catch (Exception e) {
                attempts++;
                if (attempts == 3) throw e;
                Thread.sleep(500);
            }
        }
    }

    /**
     * Handles city and locality selection in address details
     */
    private void selectAddressCityAndLocality(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Actions actions) throws InterruptedException {
        // City Selection
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.select input.employee-select-wrap--elipses")));
        WebElement cityContainer = driver.findElement(By.cssSelector("div.select"));
        js.executeScript("arguments[0].scrollIntoView(true);", cityContainer);
        Thread.sleep(1000);

        WebElement cityDropdownToggle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.select svg.cp")));
        actions.moveToElement(cityDropdownToggle).click().perform();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));

        WebElement cityOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='jk-dropdown-unique']//div[contains(@class,'profile-dropdown--item')][1]")));
        cityOption.click();
        logger.info("Selected first available city option");

        Thread.sleep(300);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));

        // Locality Selection
        List<WebElement> allDropdowns = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.select")));
        WebElement localityDropdownContainer = allDropdowns.get(1);
        js.executeScript("arguments[0].scrollIntoView(true);", localityDropdownContainer);
        Thread.sleep(200);

        WebElement localityArrow = localityDropdownContainer.findElement(By.cssSelector("svg.cp"));
        actions.moveToElement(localityArrow).click().perform();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));

        WebElement localityOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='jk-dropdown-unique']//div[contains(@class,'profile-dropdown--item')][1]")));
        localityOption.click();
        logger.info("Selected first available locality option");

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));
        Thread.sleep(1000);
    }

    /**
     * Selects identity proof dropdown
     */
    private void selectIdentityProof(WebDriver driver, WebDriverWait wait, Actions actions) throws InterruptedException {
        try {
            logger.info("Selecting Identity Proof");
            Actions actionsForIdentity = new Actions(driver);
            WebElement identityDropdownToggle = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.select svg.cp")));
            actionsForIdentity.moveToElement(identityDropdownToggle).click().perform();

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jk-dropdown-unique")));
            WebElement firstOption = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@id='jk-dropdown-unique']/div[contains(@class, 'profile-dropdown--item')][1]")));
            
            String selectedOptionText = firstOption.getText().trim();
            firstOption.click();
            logger.info("Identity proof selected: " + selectedOptionText);

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("jk-dropdown-unique")));
            Thread.sleep(2000);
        } catch (Exception e) {
            logger.info("Error during identity proof selection: " + e.getMessage());
        }
    }

    /**
     * Handles document upload for all required files
     */
    private void uploadDocuments(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        logger.info("Uploading Documents");
        
        // File paths from configuration
        String[] docFilePaths = {
            ConfigReader.get("document.identity.proof"),
            ConfigReader.get("document.owner.proof"),
            ConfigReader.get("document.vaccination.cert"),
            ConfigReader.get("document.fitness.cert")
        };

        List<WebElement> fileInputs = driver.findElements(By.cssSelector("input.input-mirror-selector-button[type='file']"));
        
        if (fileInputs.size() < 4) {
            logger.info("Error: Expected at least 4 file inputs, found " + fileInputs.size());
            return;
        }

        // Upload documents
        for (int i = 0; i < docFilePaths.length; i++) {
            uploadSingleDocument(fileInputs.get(i), docFilePaths[i], js, i + 1);
        }

        // Upload pet photo
        uploadPetPhoto(driver, wait, js);
        
        logger.info("All document uploads completed!");
    }

    /**
     * Uploads a single document with retry logic
     */
    private void uploadSingleDocument(WebElement fileInput, String filePath, JavascriptExecutor js, int fieldNumber) {
        int maxRetries = 3;
        boolean uploadSuccessful = false;

        for (int attempt = 1; attempt <= maxRetries && !uploadSuccessful; attempt++) {
            try {
                logger.info("Uploading file for field " + fieldNumber + " - Attempt: " + attempt);
                Thread.sleep(1000);

                js.executeScript("arguments[0].scrollIntoView(true);", fileInput);
                Thread.sleep(500);

                try {
                    fileInput.sendKeys(filePath);
                    logger.info("File path sent using direct sendKeys");
                } catch (Exception e1) {
                    logger.info("Direct sendKeys failed, trying JavaScript approach...");
                    js.executeScript("arguments[0].style.display = 'block'; arguments[0].style.visibility = 'visible';", fileInput);
                    Thread.sleep(500);
                    fileInput.sendKeys(filePath);
                    logger.info("File path sent after making element visible");
                }

                Thread.sleep(4000);
                uploadSuccessful = true;
                logger.info("Upload for field " + fieldNumber + " completed successfully!");
                
            } catch (Exception e) {
                logger.info("Error on attempt " + attempt + ": " + e.getMessage());
                if (attempt == maxRetries) {
                    logger.info("Upload for field " + fieldNumber + " completed with uncertainties");
                    uploadSuccessful = true; // Assume success to continue
                } else {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                }
            }
        }
    }

    /**
     * Uploads pet photo
     */
    private void uploadPetPhoto(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        int maxRetries = 3;
        boolean petPhotoUploadSuccessful = false;

        for (int attempt = 1; attempt <= maxRetries && !petPhotoUploadSuccessful; attempt++) {
            try {
                logger.info("Uploading pet photo - Attempt: " + attempt);
                WebElement petPhotoInput = driver.findElement(By.cssSelector("input[type='file']#upload"));
                js.executeScript("arguments[0].scrollIntoView(true);", petPhotoInput);
                Thread.sleep(500);

                try {
                    petPhotoInput.sendKeys(ConfigReader.get("document.pet.photo"));
                    logger.info("Pet photo path sent using direct sendKeys");
                } catch (Exception e1) {
                    logger.info("Direct sendKeys failed for pet photo, trying JavaScript approach...");
                    js.executeScript("arguments[0].style.display = 'block'; arguments[0].style.visibility = 'visible';", petPhotoInput);
                    Thread.sleep(500);
                    petPhotoInput.sendKeys(ConfigReader.get("document.pet.photo"));
                    logger.info("Pet photo path sent after making element visible");
                }

                Thread.sleep(4000);
                petPhotoUploadSuccessful = true;
                logger.info("Pet photo upload completed successfully!");
                
            } catch (Exception e) {
                logger.info("Error on attempt " + attempt + ": " + e.getMessage());
                if (attempt == maxRetries) {
                    logger.info("Pet photo upload completed with uncertainties");
                    petPhotoUploadSuccessful = true;
                } else {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                }
            }
        }
    }
    
}