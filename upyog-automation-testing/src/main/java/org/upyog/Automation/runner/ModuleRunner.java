package org.upyog.Automation.runner;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.upyog.Automation.engine.TestEngine;
import org.upyog.Automation.engine.TestEngine.ExecutionResult;

public class ModuleRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(ModuleRunner.class);

    private static final String PROPERTIES_PATH =
            "config/dev.properties";

    public static void main(String[] args) {

        String moduleToRun =
                args.length > 0
                        ? args[0].toUpperCase()
                        : "EWASTE_CITIZEN";

        WebDriver driver = null;

        try {

            driver = createWebDriver();

            // Runtime values
            WorkflowDataStore.put("selected.url",
                    "https://upyog.niua.org/upyog-ui/citizen/login");

            WorkflowDataStore.put("selected.mobile",
                    "9999999999");

            WorkflowDataStore.put("selected.otp",
                    "123456");

            WorkflowDataStore.put("selected.city",
                    "Delhi");

            TestEngine engine =
                    new TestEngine(
                            driver,
                            PROPERTIES_PATH
                    );

            runModule(engine, moduleToRun);

        } catch (Exception e) {

            logger.error(
                    "Execution Failed",
                    e
            );
        }
    }

    private static WebDriver createWebDriver() {

        // Download matching ChromeDriver and clear old cached drivers
        WebDriverManager.chromedriver()
                .clearDriverCache()
                .setup();

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");

        WebDriver driver = new ChromeDriver(options);

        return driver;
    }

    private static void runModule(
            TestEngine engine,
            String moduleName) {

        ExecutionResult result;

        switch (moduleName) {

            // =====================================================
            // EWASTE
            // =====================================================

            case "EWASTE_CITIZEN":

                result = engine.executeModule(
                        "test-config/ewaste/ewaste_citizen_module.json"
                );

                break;

            case "EWASTE_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/ewaste/ewaste_employee_module.json"
                );

                break;


            // =====================================================
            // ADVERTISEMENT
            // =====================================================

            case "ADVERTISEMENT_CITIZEN":

                result = engine.executeModule(
                        "test-config/advertisement/advertisement_citizen_module.json"
                );

                break;

            case "ADVERTISEMENT_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/advertisement/advertisement_employee_module.json"
                );

                break;


            // =====================================================
            // ASSET
            // =====================================================

            case "ASSET_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/asset/asset_employee_module.json"
                );

                break;


            // =====================================================
            // CHB
            // =====================================================

            case "CHB_CITIZEN":

                result = engine.executeModule(
                        "test-config/chb/chb_citizen_module.json"
                );

                break;

            case "CHB_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/chb/chb_employee_module.json"
                );

                break;


            // =====================================================
            // CND
            // =====================================================

            case "CND_REQUEST":

                result = engine.executeModule(
                        "test-config/cnd/cnd_request_module.json"
                );

                break;

            case "CND_VENDOR":

                result = engine.executeModule(
                        "test-config/cnd/cnd_vendor_module.json"
                );

                break;


            // =====================================================
            // OBPAS
            // =====================================================

            case "OBPAS_CREATE":

                result = engine.executeModule(
                        "test-config/obpas/obpas_create_module.json"
                );

                break;

            case "OBPAS_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/obpas/obpas_employee_module.json"
                );

                break;

            case "OBPAS_OC":

                result = engine.executeModule(
                        "test-config/obpas/obpas_oc_module.json"
                );

                break;

            case "OBPAS_OC_EMP":

                result = engine.executeModule(
                        "test-config/obpas/obpas_oc_emp_module.json"
                );

                break;


            // =====================================================
            // PET
            // =====================================================

            case "PET":

                result = engine.executeModule(
                        "test-config/pet/pet_citizen_module.json"
                );

                break;

            case "PET_EMP":

                result = engine.executeModule(
                        "test-config/pet/pet_employee_module.json"
                );

                break;


            // =====================================================
            // PROPERTY TAX
            // =====================================================

            case "PROPERTY_TAX":

                result = engine.executeModule(
                        "test-config/property/property_tax_citizen_module.json"
                );

                break;

            case "PROPERTY_TAX_EMP":

                result = engine.executeModule(
                        "test-config/property/property_tax_employee_module.json"
                );

                break;


            // =====================================================
            // PGR
            // =====================================================

            case "PGR":

                result = engine.executeModule(
                        "test-config/pgr/pgr_citizen_module.json"
                );

                break;

            case "PGR_EMP":

                result = engine.executeModule(
                        "test-config/pgr/pgr_employee_module.json"
                );

                break;


            // =====================================================
            // STREET VENDING
            // =====================================================

            case "STREET_VENDING":

                result = engine.executeModule(
                        "test-config/streetvending/street_vending_citizen_module.json"
                );

                break;

            case "STREET_VENDING_EMP":

                result = engine.executeModule(
                        "test-config/streetvending/street_vending_employee_module.json"
                );

                break;


            // =====================================================
            // TRADE LICENSE
            // =====================================================

            case "TRADE_LICENSE":

                result = engine.executeModule(
                        "test-config/tradelicense/trade_license_citizen_module.json"
                );

                break;

            case "TRADE_LICENSE_EMP":

                result = engine.executeModule(
                        "test-config/tradelicense/trade_license_employee_module.json"
                );

                break;


            // =====================================================
            // WATER + SEWERAGE
            // =====================================================

            case "WATER_AND_SEWERAGE":

                result = engine.executeModule(
                        "test-config/waterandsewerage/water_and_sewerage_citizen_module.json"
                );

                break;

            case "WATER":

                result = engine.executeModule(
                        "test-config/waterandsewerage/water_employee_module.json"
                );

                break;

            case "SEWERAGE":

                result = engine.executeModule(
                        "test-config/waterandsewerage/sewerage_employee_module.json"
                );

                break;


            default:

                throw new RuntimeException(
                        "Unknown module : " + moduleName
                );
        }

        reportResult(result);
    }

    private static void reportResult(
            ExecutionResult result) {

        if (result.isSuccess()) {

            logger.info(
                    "PASSED : {}",
                    result
            );

        } else {

            logger.error(
                    "FAILED : {}",
                    result
            );
        }
    }
}