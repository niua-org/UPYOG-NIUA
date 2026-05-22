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
                        : "SEWERAGE_EMP";

        WebDriver driver = null;

        try {

            driver = createWebDriver();

//            // Runtime values
//            WorkflowDataStore.put("selected.url",
//                    "https://upyog.niua.org/upyog-ui/citizen/login");
//
//            WorkflowDataStore.put("selected.cndCitizen.url",
//                    "https://upyog.niua.org/cnd-ui/citizen/login");
//
//            WorkflowDataStore.put("selected.svCitizen.url",
//                    "https://upyog.niua.org/sv-ui/citizen/login");
//
//            WorkflowDataStore.put("selected.cndEmployee.url",
//                    "https://upyog.niua.org/cnd-ui/employee/login");
//
//            WorkflowDataStore.put("selected.svEmployee.url",
//                    "https://upyog.niua.org/sv-ui/employee/login");
//
//            WorkflowDataStore.put("selected.mobile",
//                    "9999999999");
//
//            WorkflowDataStore.put("selected.otp",
//                    "123456");
//
//            WorkflowDataStore.put("selected.city",
//                    "Delhi");
//
//            WorkflowDataStore.put("selected.city.mohali",
//                    "Mohali");

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
                        "test-config/advertisement/adv_citizen_module.json"
                );

                break;

            case "ADVERTISEMENT_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/advertisement/adv_employee_module.json"
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
                        "test-config/cnd/cnd_citizen_module.json"
                );

                break;

            case "CND_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/cnd/cnd_employee_module.json"
                );

                break;

            case "CND_VENDOR":

                result = engine.executeModule(
                        "test-config/cnd/cnd_vendor_module.json"
                );

                break;

            // =====================================================
            // DESLUDGING SERVICE
            // =====================================================

            case "DESLUDGING":

                result = engine.executeModule(
                        "test-config/desludging/desludging_citizen_module.json"
                );

                break;


            // =====================================================
            // OBPAS
            // =====================================================

            case "OBPAS_CREATE":

                result = engine.executeModule(
                        "test-config/obpas/obpas_citizen_module.json"
                );

                break;

            case "OBPAS_EMPLOYEE":

                result = engine.executeModule(
                        "test-config/obpas/obpas_employee_module.json"
                );

                break;

            case "OBPAS_OC":

                result = engine.executeModule(
                        "test-config/obpas/obpas_oc_citizen_module.json"
                );

                break;

            case "OBPAS_OC_EMP":

                result = engine.executeModule(
                        "test-config/obpas/obpas_oc_employee_module.json"
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
                        "test-config/propertyTax/property_tax_citizen_module.json"
                );

                break;

            case "PROPERTY_TAX_EMP":

                result = engine.executeModule(
                        "test-config/propertyTax/property_tax_employee_module.json"
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
            // WATER + SEWERAGE
            // =====================================================

            case "MOBILE_TOILET_CITIZEN":

                result = engine.executeModule(
                        "test-config/RequestService/mobile_toilet_citizen_module.json"
                );

                break;

            case "MOBILE_TOILET_EMP":

                result = engine.executeModule(
                        "test-config/RequestService/mobile_toilet_employee_module.json"
                );

                break;
            case "MOBILE_TOILET_VENDOR":

                result = engine.executeModule(
                        "test-config/RequestService/mobile_toilet_vendor_module.json"
                );

                break;

            case "TREE_PRUNING_CITIZEN":

                result = engine.executeModule(
                        "test-config/RequestService/tree_pruning_citizen_module.json"
                );

                break;

            case "TREE_PRUNING_EMP":

                result = engine.executeModule(
                        "test-config/RequestService/tree_pruning_employee_module.json"
                );

                break;

            case "TREE_PRUNING_VENDOR":

                result = engine.executeModule(
                        "test-config/RequestService/tree_pruning_vendor_module.json"
                );

                break;

            case "WATER_TANKER_CITIZEN":

                result = engine.executeModule(
                        "test-config/RequestService/water_tanker_citizen_module.json"
                );

                break;

            case "WATER_TANKER_EMP":

                result = engine.executeModule(
                        "test-config/RequestService/water_tanker_employee_module.json"
                );

                break;

            case "WATER_TANKER_VENDOR":

                result = engine.executeModule(
                        "test-config/RequestService/water_tanker_vendor_module.json"
                );

                break;


            // =====================================================
            // STREET VENDING
            // =====================================================

            case "STREET_VENDING":

                result = engine.executeModule(
                        "test-config/streetVending/street_vending_citizen_module.json"
                );

                break;

            case "STREET_VENDING_EMP":

                result = engine.executeModule(
                        "test-config/streetVending/street_vending_employee_module.json"
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
                        "test-config/waterAndSewerage/water_and_sewerage_citizen_module.json"
                );

                break;

            case "WATER_EMP":

                result = engine.executeModule(
                        "test-config/waterAndSewerage/water_employee_module.json"
                );

                break;

            case "SEWERAGE_EMP":

                result = engine.executeModule(
                        "test-config/waterAndSewerage/sewerage_employee_module.json"
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