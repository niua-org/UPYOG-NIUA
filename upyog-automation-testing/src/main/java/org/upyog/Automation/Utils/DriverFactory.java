package org.upyog.Automation.Utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.github.bonigarcia.wdm.WebDriverManager;

public class DriverFactory {

    public static WebDriver createChromeDriver() {

        WebDriverManager.chromedriver().setup();

        // Grid config
        boolean isGrid = Boolean.parseBoolean(
                System.getProperty("selenium.grid.enabled",
                        ConfigReader.get("selenium.grid.enabled"))
        );

        String gridUrl = System.getProperty("selenium.grid.url",
                ConfigReader.get("selenium.grid.url"));

        // Execution mode (local / headless / vnc)
        String executionMode = System.getProperty("executionMode", "local");

        ChromeOptions options = new ChromeOptions();

        // Load options from config
        for (int i = 1; ; i++) {
            String opt = ConfigReader.get("chrome.option." + i);
            if (opt == null) break;
            options.addArguments(opt);
        }

        // Mode-based options
        switch (executionMode.toLowerCase()) {

            case "headless":
                options.addArguments("--headless=new");
                options.addArguments("--window-size=1920,1080");
                break;

            case "vnc":
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--no-sandbox");
                break;

            case "local":
            default:
                options.addArguments("--start-maximized");
                break;
        }

        // Chrome prefs
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("autofill.address_enabled",
                Boolean.parseBoolean(ConfigReader.get("chrome.prefs.autofill.address_enabled")));
        prefs.put("autofill.profile_enabled",
                Boolean.parseBoolean(ConfigReader.get("chrome.prefs.autofill.profile_enabled")));
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver;

        try {
            if (isGrid) {

                System.out.println("Running on Selenium Grid: " + gridUrl);

                driver = new RemoteWebDriver(
                        new java.net.URL(gridUrl),
                        options
                );

                // Important for file upload
                ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());

            } else {

                System.out.println("Running on Local Chrome");

                driver = new ChromeDriver(options);
            }

        } catch (Exception e) {
            throw new RuntimeException("Driver initialization failed", e);
        }

        // Maximize for local
        if (!isGrid && executionMode.equalsIgnoreCase("local")) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    public static WebDriverWait createWebDriverWait(WebDriver driver) {
        int timeout = Integer.parseInt(ConfigReader.get("webdriver.wait.timeout"));
        return new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }
}