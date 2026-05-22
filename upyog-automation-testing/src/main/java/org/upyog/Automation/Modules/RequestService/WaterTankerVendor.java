package org.upyog.Automation.Modules.RequestService;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.upyog.Automation.engine.TestEngine;

@Component
public class WaterTankerVendor {

    public void waterTankerVCreate(
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
                    "src/main/resources/test-config/requestservice/water_tanker_vendor_module.json"
            );

        }catch(Exception e){

            throw new RuntimeException(
                    "Water Tanker Vendor Failed",
                    e
            );
        }
    }
}

