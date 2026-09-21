package org.upyog.Automation.Utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.upyog.Automation.Reports.ReportManager;

import java.util.List;

public class CommonActions {

    public static void fillInput(WebDriverWait wait, String fieldName, String value) {
        By locator = By.xpath("//input[@name='" + fieldName + "' or @id='" + fieldName + "'] | //*[@name='" + fieldName + "']");
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(locator));
        input.clear();
        input.sendKeys(value);
    }

    public static void clickButton(WebDriverWait wait,
                                   JavascriptExecutor js,
                                   String xpath) {

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(xpath)
                )
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                button
        );

        wait.until(
                ExpectedConditions.visibilityOf(button)
        );

        js.executeScript(
                "arguments[0].click();",
                button
        );
    }

    public static void selectDropdown(WebDriver driver, WebDriverWait wait, String fieldName, String value) {
        WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        dropdown.click();

        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[contains(text(),'" + value + "')]")));
        option.click();
    }

    public static void selectRadioButtonByLabel(WebDriver driver,
                                                WebDriverWait wait,
                                                JavascriptExecutor js,
                                                String labelText) {

        WebElement radio = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//*[normalize-space()='" + labelText + "']" +
                                        "/ancestor::*[contains(@class,'radio-wrap')]" +
                                        "//input[@type='radio']"
                        )
                )
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                radio
        );

        js.executeScript(
                "arguments[0].click();",
                radio
        );
    }

    public static void clickButtonByText(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String text) {
        By locator = By.xpath("//button[.//header[normalize-space()='" + text + "'] or normalize-space()='" + text + "' or .//span[normalize-space()='" + text + "']]");
        WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", button);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", button);
        }
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CommonActions.class);

    public static void selectCity(WebDriver driver,
                                  WebDriverWait wait,
                                  JavascriptExecutor js,
                                  String cityName) {

        String selectedUrl = WorkflowDataStore.get(AutomationConstants.KEY_SELECTED_URL);
        if (selectedUrl != null && selectedUrl.toLowerCase().contains("sandbox")) {
            if (cityName == null || cityName.isBlank() || "City A".equalsIgnoreCase(cityName) || "Delhi".equalsIgnoreCase(cityName)) {
                cityName = "City A Muncipal Corporation";
            }
        } else if (selectedUrl != null && selectedUrl.toLowerCase().contains("niuatt")) {
            if (cityName == null || cityName.isBlank() || "Delhi".equalsIgnoreCase(cityName) || "City A Muncipal Corporation".equalsIgnoreCase(cityName)) {
                cityName = "City A";
            }
        } else if (selectedUrl != null && selectedUrl.toLowerCase().contains("upyog.niua.org")) {
            if (cityName == null || cityName.isBlank() || "City A".equalsIgnoreCase(cityName) || "City A Muncipal Corporation".equalsIgnoreCase(cityName)) {
                cityName = "Delhi";
            }
        }

        logger.info("Selecting city on location screen: '{}' (Target URL: {})", cityName, selectedUrl);

        // 1. Wait for location screen container or radio elements
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.radio-wrap")),
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='radio']")),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Choose your location') or contains(text(),'Select City') or contains(text(),'City')]"))
            ));
        } catch (Exception e) {
            logger.warn("Location container wait, checking options directly: {}", e.getMessage());
        }

        // 2. Direct XPath match for matching label
        List<WebElement> matchingLabels = driver.findElements(By.xpath(
                "//label[normalize-space()='" + cityName + "' or contains(normalize-space(),'" + cityName + "')]"
        ));

        if (!matchingLabels.isEmpty()) {
            WebElement label = matchingLabels.get(0);
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", label);
            try {
                WebElement radio = label.findElement(By.xpath("./preceding-sibling::span/input[@type='radio'] | ./following-sibling::span/input[@type='radio'] | .//input[@type='radio'] | ./ancestor::*[contains(@class,'radio-wrap')]//input[@type='radio']"));
                js.executeScript("arguments[0].click();", radio);
            } catch (Exception ignored) {
                js.executeScript("arguments[0].click();", label);
            }
            logger.info("Successfully selected city via matching label: {}", cityName);
            return;
        }

        // 3. Scan through all radio container elements
        List<WebElement> cityOptions = driver.findElements(
                By.cssSelector("div.radio-wrap div, div.radio-wrap, .reverse-radio-selection-wrapper div, label"));

        for (WebElement option : cityOptions) {
            try {
                String text = option.getText().trim();
                if (text.equalsIgnoreCase(cityName) ||
                    (cityName.contains("City A") && text.contains("City A")) ||
                    (cityName.equalsIgnoreCase("Delhi") && text.contains("Delhi"))) {

                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);

                    List<WebElement> radioInputs = option.findElements(By.cssSelector("input[type='radio']"));
                    if (!radioInputs.isEmpty()) {
                        WebElement radioInput = radioInputs.get(0);
                        js.executeScript("arguments[0].click();", radioInput);
                    } else {
                        js.executeScript("arguments[0].click();", option);
                    }
                    logger.info("Selected city option: '{}' for target: '{}'", text, cityName);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 4. Fallback to first available radio option
        List<WebElement> allRadios = driver.findElements(By.cssSelector("input[type='radio']"));
        if (!allRadios.isEmpty()) {
            WebElement firstRadio = allRadios.get(0);
            js.executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", firstRadio);
            logger.warn("Target city '{}' exact match not found, selected first radio option.", cityName);
            return;
        }

        throw new RuntimeException("Failed to select city: " + cityName);
    }

    public static void selectDropdownByIndex(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, int dropdownIndex, int optionIndex)
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
}