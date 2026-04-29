package org.upyog.Automation.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Modules.Adv.AdvBookingCreate;
import org.upyog.Automation.Modules.CHB.ChbCreate;
import org.upyog.Automation.Modules.CnD.CnDRequest;
import org.upyog.Automation.Modules.DesludgingService.DesludgingCreate;
import org.upyog.Automation.Modules.EWaste.EWasteCreate;
import org.upyog.Automation.Modules.OBPAS.OBPASCreate;
import org.upyog.Automation.Modules.OBPAS.OBPASOcCreate;
import org.upyog.Automation.Modules.Pet.PetCreateApplication;
import org.upyog.Automation.Modules.PublicGrievanceRedressal.PgrCreate;
import org.upyog.Automation.Modules.PropertyTax.PropertyTaxCreate;
import org.upyog.Automation.Modules.StreetVending.SvCreateApplication;
import org.upyog.Automation.Modules.TradeLicense.TradeLicenseCreate;
import org.upyog.Automation.Modules.RequestService.TreePruningCitizen;
import org.upyog.Automation.Modules.RequestService.WaterTankerCitizen;
import org.upyog.Automation.Modules.RequestService.MobileToiletCitizen;
import org.upyog.Automation.Modules.WaterAndSewerage.WAndSCreate;


/**
 * Common entry point for all citizen module tests
 * Routes to appropriate module based on moduleName
 */

@Component
public class CommonCitizenTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonCitizenTest.class);

    @Autowired
    private SvCreateApplication svCreateApplication;
    
    @Autowired
    private TradeLicenseCreate tradeLicenseCreate;
    
    @Autowired
    private PetCreateApplication petCreateApplication;
    
    @Autowired
    private AdvBookingCreate advBookingCreate;
    
    @Autowired
    private TreePruningCitizen treePruningCitizen;
    
    @Autowired
    private WaterTankerCitizen waterTankerCitizen;
    
    @Autowired
    private MobileToiletCitizen mobileToiletCitizen;
    
    @Autowired
    private PropertyTaxCreate propertyTaxCreate;
    
    @Autowired
    private PgrCreate pgrCreate;
    
    @Autowired
    private OBPASCreate obpasCreate;
    
    @Autowired
    private EWasteCreate eWasteCreate;
    
    @Autowired
    private ChbCreate chbCreate;

    @Autowired
    private CnDRequest cndRequest;

    @Autowired
    private OBPASOcCreate obpasOcCreate;

    @Autowired
    private DesludgingCreate desludgingCreate;

    @Autowired
    private WAndSCreate wAndSCreate;

    public void runCitizenTest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String permitNumber) throws InterruptedException {
        logger.info("Starting {} citizen test", moduleName);

        try {
            switch (moduleName.toUpperCase()) {

                case "STREET_VENDING":
                    svCreateApplication.svCreateReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "TRADE_LICENSE":
                    tradeLicenseCreate.tradeLicenceCitizenReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "PET_REGISTRATION":
                    petCreateApplication.petApptest(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "ADVERTISEMENT":
                    advBookingCreate.advBookingReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "TREE_PRUNING":
                    treePruningCitizen.treePruningCreate(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "WATER_TANKER":
                    waterTankerCitizen.waterTankerCreate(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "MOBILE_TOILET":
                    mobileToiletCitizen.mobileToiletCreate(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "PROPERTY_TAX":
                    propertyTaxCreate.newPropertyReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "PUBLIC_GRIEVANCE_REDRESSAL":
                    pgrCreate.pgrReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM":
                    obpasCreate.obpasReg(baseUrl, moduleName, mobileNumber, otp, cityName, permitNumber);
                    break;

                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC":
                    obpasOcCreate.obpasOCReg(baseUrl, moduleName, mobileNumber, otp, cityName, permitNumber);
                    break;

                case "EWASTE_MANAGEMENT_SYSTEM":
                    eWasteCreate.eWasteReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "COMMUNITY_HALL_BOOKING":
                    chbCreate.chbReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "CONSTRUCTION_AND_DEMOLITION":
                    cndRequest.cndReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "DESLUDGING_SERVICE":
                    desludgingCreate.desludgingReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                case "WATER_AND_SEWERAGE":
                    wAndSCreate.wandSReg(baseUrl, moduleName, mobileNumber, otp, cityName);
                    break;

                default:
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