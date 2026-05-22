package org.upyog.Automation.Modules.WaterAndSewerage;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.upyog.Automation.engine.TestEngine;

@Component
public class WAndSCreate {

    public void wandSReg(
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
                    "src/main/resources/test-config/waterandsewerage/water_and_sewerage_citizen_module.json"
            );

        }catch(Exception e){

            throw new RuntimeException(
                    "Water And Sewerage Citizen Failed",
                    e
            );
        }
    }
}

