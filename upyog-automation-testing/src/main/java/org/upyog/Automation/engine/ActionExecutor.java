package org.upyog.Automation.engine;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.upyog.Automation.model.TestInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Action Execution Engine
 *
 * Executes actions defined in JSON configuration against web elements.
 * Uses Strategy Pattern via switch expression to handle different action types.
 *
 * Each action type maps to a specific Selenium operation:
 * - TYPE: Clear field and enter text
 * - CLICK: Standard element click
 * - CLICK_JS: JavaScript click (bypasses overlays)
 * - HOVER: Mouse hover action
 * - SELECT_DROPDOWN_BY_INDEX: Custom dropdown selection
 * - UPLOAD_FILE: File input handling
 * - WAIT_FOR_VISIBLE: Explicit wait for visibility
 * - CHECK_LAST_CHECKBOX: Select last checkbox in a group
 * - SET_DATE_TODAY: Set date input to current date
 *
 * The executor handles exceptions gracefully and provides detailed logging
 * for debugging failed steps.
 */
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;
    private final Actions actions;
    private final org.upyog.Automation.engine.LocatorResolver locatorResolver;

    public ActionExecutor(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.js = (JavascriptExecutor) driver;
        this.actions = new Actions(driver);
        this.locatorResolver = new org.upyog.Automation.engine.LocatorResolver();
    }

    /**
     * Executes a single test instruction.
     * <p>
     * Flow:
     * 1. Resolve the locator from config
     * 2. Execute the action based on action type
     * 3. Apply dynamic sleep if specified
     *
     * @param instruction The instruction to execute
     * @throws Exception if action execution fails
     */
    public void execute(TestInstruction instruction) throws Exception {
        String action = instruction.getAction();
        String stepName = instruction.getStepName();

        logger.info(
                "Executing Step: {} | Action: {} | Locator: {}",
                instruction.getStepName(),
                instruction.getAction(),
                instruction.getLocatorValue()
        );

        try {
            // Dispatch to appropriate action handler based on action type
            // Using switch expression for clean, exhaustive handling
            switch (action.toUpperCase()) {

                case "TYPE":
                    executeType(instruction);
                    break;

                case "CLICK":
                    executeClick(instruction);
                    break;

                case "CLICK_JS":
                    executeJsClick(instruction);
                    break;

                case "HOVER":
                    executeHover(instruction);
                    break;

                case "UPLOAD_FILE":
                    executeFileUpload(instruction);
                    break;

                case "TYPE_OTP":
                    executeOtpType(instruction);
                    break;

                case "SELECT_RADIO_BY_TEXT":
                    executeRadioSelectionByText(instruction);
                    break;

                case "SELECT_DROPDOWN_BY_INDEX":
                    executeDropdownSelectionByIndex(instruction);
                    break;

                case "CHECK_LAST_CHECKBOX":
                    checkLastCheckbox(instruction);
                    break;

                case "CAPTURE_TEXT":
                    captureText(instruction);
                    break;

                case "TYPE_FROM_STORE":
                    typeFromStore(instruction);
                    break;

                case "SET_DATE_TODAY":
                    executeSetDateToday(instruction);
                    break;

                case "SWITCH_WINDOW":
                    switchWindow();
                    break;

                case "WAIT_FOR_TEXT":
                    waitForText(instruction);
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown action: " + action
                    );
            }

            // Apply dynamic sleep after action (configured per-step in JSON)
            applyDynamicSleep(instruction);

            logger.info("✓ Completed step: {}", stepName);

        } catch (NoSuchElementException e) {
            logger.error("✗ Element not found for step '{}': {}", stepName, e.getMessage());
            throw new RuntimeException("Step failed - element not found: " + stepName, e);

        } catch (TimeoutException e) {
            logger.error("✗ Timeout waiting for element in step '{}': {}", stepName, e.getMessage());
            throw new RuntimeException("Step failed - timeout: " + stepName, e);

        } catch (ElementClickInterceptedException e) {
            logger.warn("Click intercepted for step '{}', attempting JS click", stepName);
            // Fallback to JS click when standard click is intercepted
            executeJsClick(instruction);
            applyDynamicSleep(instruction);
        }
    }

    /**
     * TYPE action: Clears the field and types the input value.
     */
    private void executeType(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));

        // Scroll element into view to ensure it's interactable
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);

        element.clear();
        element.sendKeys(instruction.getInputValue());

        logger.debug("Typed '{}' into element", instruction.getInputValue());
    }

    /**
     * CLICK action: Standard Selenium click.
     */
    private void executeClick(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        element.click();

        logger.debug("Clicked element");
    }

    /**
     * CLICK_JS action: JavaScript click that bypasses overlay elements.
     * Useful when standard click is intercepted by modals, loading spinners, etc.
     */
    private void executeJsClick(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        js.executeScript("arguments[0].click();", element);

        logger.debug("JS-clicked element");
    }

    /**
     * HOVER action: Mouse hover using Actions API.
     */
    private void executeHover(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

        actions.moveToElement(element).perform();

        logger.debug("Hovered over element");
    }

    /**
     * SELECT_DROPDOWN_BY_INDEX action: Handles custom dropdown components.
     * <p>
     * Input format: "dropdownIndex:optionIndex" (e.g., "0:0" for first dropdown, first option)
     * <p>
     * This handles non-standard dropdowns that use div/span elements
     * instead of native HTML select elements.
     */
    private void executeDropdownSelect(TestInstruction instruction) throws InterruptedException {
        By locator = locatorResolver.resolveLocator(instruction);
        String[] indices = instruction.getInputValue().split(":");
        int dropdownIndex = Integer.parseInt(indices[0]);
        int optionIndex = Integer.parseInt(indices[1]);

        // Find all dropdown triggers on the page
        List<WebElement> dropdowns = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(locator)
        );

        if (dropdownIndex >= dropdowns.size()) {
            throw new IndexOutOfBoundsException(
                    "Dropdown index " + dropdownIndex + " out of range (found " + dropdowns.size() + ")"
            );
        }

        WebElement dropdown = dropdowns.get(dropdownIndex);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", dropdown);
        Thread.sleep(200);

        // Click to open dropdown
        try {
            dropdown.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", dropdown);
        }

        // Wait for options to appear and select by index
        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card"))
        );

        List<WebElement> options = optionsContainer.findElements(
                By.cssSelector("div.profile-dropdown--item")
        );

        if (optionIndex < options.size()) {
            js.executeScript("arguments[0].click();", options.get(optionIndex));
            logger.debug("Selected dropdown option at index {}", optionIndex);
        }
    }

    /**
     * SELECT_FIRST_DROPDOWN_OPTION: Opens dropdown and selects first available option.
     */
    private void executeSelectFirstDropdownOption(TestInstruction instruction) throws InterruptedException {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement dropdownTrigger = wait.until(ExpectedConditions.elementToBeClickable(locator));

        actions.moveToElement(dropdownTrigger).click().perform();

        WebElement optionsContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.options-card"))
        );

        WebElement firstOption = optionsContainer.findElement(
                By.cssSelector(".profile-dropdown--item:first-child")
        );

        actions.moveToElement(firstOption).click().perform();
        logger.debug("Selected first dropdown option");
    }

    /**
     * UPLOAD_FILE action: Handles file input elements.
     * Makes hidden file inputs visible before sending file path.
     */
    private void executeFileUpload(TestInstruction instruction) throws InterruptedException {
        By locator = locatorResolver.resolveLocator(instruction);
        String filePath = instruction.getInputValue();

        // Validate file exists
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Upload file not found: " + filePath);
        }

        List<WebElement> fileInputs = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(locator)
        );

        if (fileInputs.isEmpty()) {
            throw new NoSuchElementException("No file input found with locator: " + locator);
        }

        // Use first file input by default
        WebElement fileInput = fileInputs.get(0);

        // Make hidden file input visible for Selenium interaction
        js.executeScript(
                "arguments[0].style.opacity='1'; arguments[0].style.display='block';",
                fileInput
        );
        Thread.sleep(300);

        fileInput.sendKeys(file.getAbsolutePath());
        logger.debug("Uploaded file: {}", filePath);
    }

    /**
     * WAIT_FOR_VISIBLE action: Explicit wait for element visibility.
     */
    private void executeWaitForVisible(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        logger.debug("Element is now visible");
    }

    /**
     * WAIT_FOR_URL_CONTAINS action: Waits until URL contains specified string.
     */
    private void executeWaitForUrlContains(TestInstruction instruction) {
        String urlPart = instruction.getLocatorValue();
        wait.until(ExpectedConditions.urlContains(urlPart));
        logger.debug("URL now contains: {}", urlPart);
    }

    /**
     * CHECK_LAST_CHECKBOX action: Finds all checkboxes and checks the last one.
     * Useful for declaration/terms checkboxes at end of forms.
     */
    private void executeCheckLastCheckbox(TestInstruction instruction) throws InterruptedException {
        By locator = locatorResolver.resolveLocator(instruction);
        List<WebElement> checkboxes = driver.findElements(locator);

        if (checkboxes.isEmpty()) {
            logger.warn("No checkboxes found");
            return;
        }

        WebElement lastCheckbox = checkboxes.get(checkboxes.size() - 1);

        if (!lastCheckbox.isSelected()) {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", lastCheckbox);
            Thread.sleep(300);
            js.executeScript("arguments[0].click();", lastCheckbox);
            logger.debug("Checked last checkbox");
        }
    }

    /**
     * SET_DATE_TODAY action: Sets a date input to today's date.
     * Uses JavaScript to set value and dispatch change event.
     */
    private void executeSetDateToday(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement dateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

        String today = LocalDate.now().toString(); // Format: yyyy-MM-dd

        js.executeScript(
                "arguments[0].value='" + today + "';" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateInput
        );

        logger.debug("Set date to: {}", today);
    }

    /**
     * CLEAR_AND_TYPE action: Clears existing content using keyboard shortcuts
     * before typing. Useful when element.clear() doesn't work properly.
     */
    private void executeClearAndType(TestInstruction instruction) {
        By locator = locatorResolver.resolveLocator(instruction);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));

        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);
        element.sendKeys(instruction.getInputValue());

        logger.debug("Cleared and typed: {}", instruction.getInputValue());
    }

    /**
     * Applies the dynamic sleep specified in the instruction.
     * This allows per-step sleep configuration in JSON without hardcoding.
     */
    private void applyDynamicSleep(TestInstruction instruction) throws InterruptedException {
        long sleepMs = instruction.getDynamicSleep();
        if (sleepMs > 0) {
            Thread.sleep(sleepMs);
            logger.debug("Applied dynamic sleep: {}ms", sleepMs);
        }
    }

    /**
     * TYPE_OTP- for OTP submission
     */
    private void executeOtpType(TestInstruction instruction) {

        List<WebElement> otpInputs =
                driver.findElements(By.cssSelector(instruction.getLocatorValue()));

        String otp = instruction.getInputValue();

        for (int i = 0; i < otp.length() && i < otpInputs.size(); i++) {
            otpInputs.get(i).sendKeys(String.valueOf(otp.charAt(i)));
        }
    }
    /**
     * SELECT_RADIO_BY_TEXT action: Selects a radio button by its visible text label.
     */

    private void executeRadioSelectionByText(
            TestInstruction instruction)
            throws InterruptedException {

        List<WebElement> options = driver.findElements(
                        By.cssSelector(instruction.getLocatorValue()));

        String expectedText = instruction.getInputValue();

        for (WebElement option : options) {

            WebElement label = option.findElement(
                            By.tagName("label"));

            if (label.getText().trim()
                    .equals(expectedText)) {

                WebElement radio =
                        option.findElement(
                                By.cssSelector(
                                        "input[type='radio']"
                                )
                        );

                if (!radio.isSelected()) {

                    js.executeScript(
                            "arguments[0].click();",
                            radio
                    );

                    Thread.sleep(
                            instruction.getDynamicSleep()
                    );
                }

                return;
            }
        }

        throw new RuntimeException(
                "City not found: "
                        + expectedText
        );
    }

    /**
     * SELECT_DROPDOWN_BY_INDEX action: Selects a dropdown by its visible text label.
     */

    private void executeDropdownSelectionByIndex(
            TestInstruction instruction)
            throws InterruptedException {

        String[] indexes =
                instruction.getInputValue()
                        .split(":");

        int dropdownIndex =
                Integer.parseInt(indexes[0]);

        int optionIndex =
                Integer.parseInt(indexes[1]);

        // Wait for dropdown inputs to appear
        wait.until(
                ExpectedConditions
                        .visibilityOfElementLocated(
                                By.cssSelector(
                                        instruction.getLocatorValue()
                                )
                        )
        );

        List<WebElement> dropdownInputs =
                driver.findElements(
                        By.cssSelector(
                                instruction.getLocatorValue()
                        )
                );

        logger.info(
                "Found {} dropdown(s)",
                dropdownInputs.size()
        );

        if (dropdownInputs.isEmpty()) {

            throw new RuntimeException(
                    "No dropdowns found with locator: "
                            + instruction.getLocatorValue()
            );
        }

        if (dropdownIndex >= dropdownInputs.size()) {

            throw new RuntimeException(
                    "Dropdown index out of range: "
                            + dropdownIndex
            );
        }

        WebElement dropdown =
                dropdownInputs.get(
                        dropdownIndex
                );

        // Scroll into view
        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                dropdown
        );

        Thread.sleep(300);

        // React-safe open dropdown
        js.executeScript(
                "arguments[0].focus();" +
                        "arguments[0].dispatchEvent(" +
                        "new MouseEvent('mousedown', { bubbles:true })" +
                        ");" +
                        "arguments[0].dispatchEvent(" +
                        "new MouseEvent('click', { bubbles:true })" +
                        ");",
                dropdown
        );

        // Wait for options
        WebElement optionsContainer =
                wait.until(
                        ExpectedConditions
                                .visibilityOfElementLocated(
                                        By.cssSelector(
                                                "div.options-card"
                                        )
                                )
                );

        List<WebElement> options =
                optionsContainer.findElements(
                        By.cssSelector(
                                "div.profile-dropdown--item"
                        )
                );

        logger.info(
                "Found {} option(s)",
                options.size()
        );

        if (optionIndex >= options.size()) {

            throw new RuntimeException(
                    "Option index out of range: "
                            + optionIndex
            );
        }

        WebElement option =
                options.get(
                        optionIndex
                );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                option
        );

        Thread.sleep(200);

        js.executeScript(
                "arguments[0].click();",
                option
        );

        Thread.sleep(
                instruction.getDynamicSleep()
        );

        logger.info(
                "Selected dropdown {} option {}",
                dropdownIndex,
                optionIndex
        );
    }

    private void checkLastCheckbox(
            TestInstruction instruction)
            throws InterruptedException {

        List<WebElement> checkboxes =
                driver.findElements(
                        By.cssSelector(
                                instruction.getLocatorValue()
                        )
                );

        if (checkboxes.isEmpty()) {
            throw new RuntimeException(
                    "No checkboxes found with locator: "
                            + instruction.getLocatorValue()
            );
        }

        WebElement lastCheckbox =
                checkboxes.get(
                        checkboxes.size() - 1
                );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                lastCheckbox
        );

        Thread.sleep(300);

        if (!lastCheckbox.isSelected()) {

            js.executeScript(
                    "arguments[0].click();",
                    lastCheckbox
            );

            logger.info(
                    "Last checkbox selected"
            );
        } else {

            logger.info(
                    "Last checkbox already selected"
            );
        }

        Thread.sleep(
                instruction.getDynamicSleep()
        );
    }

    private void captureText(
            TestInstruction instruction)
            throws InterruptedException {

        WebElement element =
                wait.until(
                        ExpectedConditions
                                .visibilityOfElementLocated(
                                        locatorResolver.resolveLocator(instruction)
                                )
                );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                element
        );

        Thread.sleep(300);

        String capturedValue =
                element.getText()
                        .trim();

        if (capturedValue.isEmpty()) {

            throw new RuntimeException(
                    "Captured text is empty"
            );
        }

        WorkflowDataStore.put(
                instruction.getInputValue(),
                capturedValue
        );

        logger.info(
                "Captured [{}] = {}",
                instruction.getInputValue(),
                capturedValue
        );

        Thread.sleep(
                instruction.getDynamicSleep()
        );
    }

    private void typeFromStore(
            TestInstruction instruction)
            throws InterruptedException {

        WebElement element =
                wait.until(
                        ExpectedConditions
                                .visibilityOfElementLocated(
                        locatorResolver.resolveLocator(instruction)

                                )
                );

        String storedValue =
                org.upyog.Automation.Utils.WorkflowDataStore.get(
                        instruction.getInputValue()
                );

        if (storedValue == null) {

            throw new RuntimeException(
                    "No value found in workflow store for key: "
                            + instruction.getInputValue()
            );
        }

        element.clear();

        element.sendKeys(
                storedValue
        );

        logger.info(
                "Typed stored value [{}] = {}",
                instruction.getInputValue(),
                storedValue
        );

        Thread.sleep(
                instruction.getDynamicSleep()
        );
    }

    private void switchWindow() {

        String currentWindow =
                driver.getWindowHandle();

        for (String windowHandle : driver.getWindowHandles()) {

            if (!windowHandle.equals(currentWindow)) {

                driver.switchTo()
                        .window(windowHandle);

                logger.info(
                        "Switched to new window"
                );

                return;
            }
        }

        throw new RuntimeException(
                "No new window found"
        );
    }

    private void waitForText(
            TestInstruction instruction) {

        By locator =
                locatorResolver.resolveLocator(
                        instruction
                );

        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        locator,
                        instruction.getInputValue()
                )
        );

        logger.info(
                "Text found: {}",
                instruction.getInputValue()
        );
    }
}