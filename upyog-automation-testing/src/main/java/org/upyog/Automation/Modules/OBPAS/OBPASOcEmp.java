package org.upyog.Automation.Modules.OBPAS;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.upyog.Automation.engine.TestEngine;

@Component
public class OBPASOcEmp {

    public void OBPASOcInboxEmp(
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
                    "src/main/resources/test-config/obpas/obpas_oc_employee.json"
            );

        }catch(Exception e){

            throw new RuntimeException(
                    "OBPAS OC Employee Flow Failed",
                    e
            );
        }
    }
}


