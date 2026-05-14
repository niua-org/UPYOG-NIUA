package org.upyog.Automation.Base;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Utils.*;

import java.time.Duration;

public class BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;

    public void setUp() throws InterruptedException {

        driver = DriverFactory.createChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;

        String baseUrl =
                ConfigReader.get("citizen.base.url");

        logger.info(
                "LOGIN URL = {}",
                baseUrl
        );

        // AUTO LOGIN
        LoginHelper.citizenLogin(
                driver,
                wait,
                js,
                baseUrl,
                ConfigReader.get("user.mobile"),
                ConfigReader.get("user.otp"),
                ConfigReader.get("city.name")
        );
    }

    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}