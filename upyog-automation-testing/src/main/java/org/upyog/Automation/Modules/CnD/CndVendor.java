package org.upyog.Automation.Modules.CnD;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.stereotype.Component;

import org.upyog.Automation.engine.TestEngine;

@Component
public class CndVendor {

    public void cndVendorFlow(
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
                    "src/main/resources/test-config/cnd/cnd_vendor_module.json"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "CnD Vendor Flow Failed",
                    e
            );
        }
    }
}
