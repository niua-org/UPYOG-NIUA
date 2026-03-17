package org.upyog.Automation.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Modules.Adv.AdvBookingCreate;
import org.upyog.Automation.Modules.CHB.chbCreate;
import org.upyog.Automation.Modules.EWaste.EWasteCreate;
import org.upyog.Automation.Modules.OBPAS.OBPASCreate;
import org.upyog.Automation.Modules.Pet.PetCreateApplication;
import org.upyog.Automation.Modules.PublicGrievanceRedressal.PgrCreate;
import org.upyog.Automation.Modules.PropertyTax.PropertyTaxCreate;
import org.upyog.Automation.Modules.StreetVending.CreateApplication;
import org.upyog.Automation.Modules.TradeLicense.TradeLicenseCreate;
import org.upyog.Automation.Modules.RequestService.TreePruningCitizen;
import org.upyog.Automation.Modules.RequestService.WaterTankerCitizen;
import org.upyog.Automation.Modules.RequestService.MobileToiletCitizen;
/**
 * Common entry point for all citizen module tests
 * Routes to appropriate module based on moduleName
 */
public class CommonCitizenTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonCitizenTest.class);

    public void runCitizenTest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName) {
        logger.info("Starting {} citizen test", moduleName);

        try {
            if ("STREET_VENDING".equalsIgnoreCase(moduleName)) {
                CreateApplication svApp = new CreateApplication();
                svApp.svCreateApplication(baseUrl, moduleName, mobileNumber, otp, cityName);

            } else if ("TRADE_LICENSE".equalsIgnoreCase(moduleName)) {
                TradeLicenseCreate tlApp = new TradeLicenseCreate();
                tlApp.TradeLicenceCitizenReg(baseUrl, moduleName, mobileNumber, otp, cityName);

            } else if ("PET_REGISTRATION".equalsIgnoreCase(moduleName)) {
                PetCreateApplication petApp = new PetCreateApplication();
                petApp.PetApptest(baseUrl, moduleName, mobileNumber, otp, cityName);

            } else if ("ADVERTISEMENT".equalsIgnoreCase(moduleName)) {
                AdvBookingCreate advApp = new AdvBookingCreate();
                advApp.AdvBookingReg(baseUrl, moduleName, mobileNumber, otp, cityName);

            }else if ("TREE_PRUNING".equalsIgnoreCase(moduleName)) {

                TreePruningCitizen treePruningApp = new TreePruningCitizen();
                treePruningApp.TreePruningCreate(baseUrl, moduleName, mobileNumber, otp, cityName);

            } else if ("WATER_TANKER".equalsIgnoreCase(moduleName)) {
                WaterTankerCitizen waterTankerApp = new WaterTankerCitizen();
                waterTankerApp.WaterTankerCreate(baseUrl, moduleName, mobileNumber, otp, cityName);

            } else if ("MOBILE_TOILET".equalsIgnoreCase(moduleName)) {
                MobileToiletCitizen mobileToiletApp = new MobileToiletCitizen();
                mobileToiletApp.MobileToiletCreate(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else if ("PROPERTY_TAX".equalsIgnoreCase(moduleName)) {
                PropertyTaxCreate propertyTaxApp = new PropertyTaxCreate();
                propertyTaxApp.NewPropertyReg(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else if ("PUBLIC_GRIEVANCE_REDRESSAL".equalsIgnoreCase(moduleName)) {
                PgrCreate pgrApp = new PgrCreate();
                pgrApp.PgrReg(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else if ("ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM".equalsIgnoreCase(moduleName)) {
                OBPASCreate obpasApp = new OBPASCreate();
                obpasApp.OBPASReg(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else if ("EWASTE_MANAGEMENT_SYSTEM".equalsIgnoreCase(moduleName)) {
                EWasteCreate eWasteApp = new EWasteCreate();
                eWasteApp.EWasteReg(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else if ("COMMUNITY HALL BOOKING".equalsIgnoreCase(moduleName)) {
                chbCreate chbApp = new chbCreate();
                chbApp.chbReg(baseUrl, moduleName, mobileNumber, otp, cityName);
            }
            else {
                logger.error("Unknown module: {}", moduleName);
                throw new RuntimeException("Unknown module: " + moduleName);
            }

            logger.info("{} test completed", moduleName);

        } catch (Exception e) {
            logger.error("Error in {} test: {}", moduleName, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
