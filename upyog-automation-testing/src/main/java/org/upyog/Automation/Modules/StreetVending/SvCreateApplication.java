package org.upyog.Automation.Modules.StreetVending;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.upyog.Automation.engine.TestEngine;

@Component
public class SvCreateApplication {

    public void svCreateReg(
            WebDriver driver,
            WebDriverWait wait,
            JavascriptExecutor js){

        try{

            TestEngine engine =
                    new TestEngine(
                            driver,
                            "src/main/resources/config/dev.properties"
                    );

            engine.executeModule(
                    "src/main/resources/test-config/streetvending/street_vending_citizen_module.json"
            );

        }catch(Exception e){

            throw new RuntimeException(
                    "Street Vending Citizen Failed",
                    e
            );
        }
    }
}

