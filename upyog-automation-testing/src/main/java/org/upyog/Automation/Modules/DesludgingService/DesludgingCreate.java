package org.upyog.Automation.Modules.DesludgingService;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.stereotype.Component;

import org.upyog.Automation.engine.TestEngine;

@Component
public class DesludgingCreate {

    public void desludgingReg(
            WebDriver driver,
            WebDriverWait wait,
            JavascriptExecutor js) {

        try {

            TestEngine engine =
                    new TestEngine(
                            driver,
                            "src/main/resources/config/dev.properties"
                    );

            engine.executeModule(
                    "src/main/resources/test-config/desludging/desludging_citizen_module.json"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Desludging Citizen Flow Failed",
                    e
            );
        }
    }
}
