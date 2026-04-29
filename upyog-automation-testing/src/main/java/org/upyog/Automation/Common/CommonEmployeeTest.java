package org.upyog.Automation.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Modules.Adv.AdvEmp;
import org.upyog.Automation.Modules.Asset.AssetEmp;
import org.upyog.Automation.Modules.CHB.ChbEmp;
import org.upyog.Automation.Modules.CnD.CndEmp;
import org.upyog.Automation.Modules.EWaste.EWasteEmp;
import org.upyog.Automation.Modules.OBPAS.ObpasEmp;
import org.upyog.Automation.Modules.OBPAS.ObpasOcEmp;
import org.upyog.Automation.Modules.Pet.PetApplicationEmp;
import org.upyog.Automation.Modules.PropertyTax.PropertyTaxEmp;
import org.upyog.Automation.Modules.PublicGrievanceRedressal.PgrEmp;
import org.upyog.Automation.Modules.RequestService.MobileToiletEmp;
import org.upyog.Automation.Modules.RequestService.TreePruningEmp;
import org.upyog.Automation.Modules.RequestService.TreePruningVerifier;
import org.upyog.Automation.Modules.StreetVending.SvEmp;
import org.upyog.Automation.Modules.TradeLicense.TradeLicenseEmp;
import org.upyog.Automation.Modules.WaterAndSewerage.SewerageEmp;
import org.upyog.Automation.Modules.WaterAndSewerage.WaterEmp;
import org.upyog.Automation.Modules.RequestService.WaterTankerEmployee;

/**
 * Common entry point for all employee module tests
 * Routes to appropriate module based on moduleName
 */
@Component
public class CommonEmployeeTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonEmployeeTest.class);

    @Autowired
    private SvEmp svEmp;
    
    @Autowired
    private PetApplicationEmp petApplicationEmp;
    
    @Autowired
    private TradeLicenseEmp tradeLicenseEmp;
    
    @Autowired
    private AssetEmp assetEmp;

    @Autowired
    private AdvEmp advEmp;

    @Autowired
    private PropertyTaxEmp propertyTaxEmp;

    @Autowired
    private EWasteEmp eWasteEmp;

    @Autowired
    private ObpasEmp obpasEmp;

    @Autowired
    private ObpasOcEmp obpasOcEmp;

    @Autowired
    private WaterTankerEmployee waterTankerEmployee;

    @Autowired
    private TreePruningEmp treePruningEmp;

    @Autowired
    private TreePruningVerifier treePruningVerifier;

    @Autowired
    private MobileToiletEmp mobileToiletEmp;

    @Autowired
    private ChbEmp chbEmp;

    @Autowired
    private CndEmp cndEmp;

    @Autowired
    private PgrEmp pgrEmp;

    @Autowired
    private SewerageEmp sewerageEmp;

    @Autowired
    private WaterEmp waterEmp;



    public void runEmployeeTest(String baseUrl, String moduleName, String username, String password, String applicationNumber) {
        logger.info("Starting {} employee test", moduleName);

        try {
            switch (moduleName.toUpperCase()) {


                case "STREET_VENDING":
                    svEmp.inboxEmpSv(baseUrl, username, password, applicationNumber);
                    break;

                case "PET_REGISTRATION":
                    petApplicationEmp.petInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "TRADE_LICENSE":
                    tradeLicenseEmp.tlInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "ASSET_MANAGEMENT_SYSTEM":
                    assetEmp.assetInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "ADVERTISEMENT":
                    advEmp.advInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "PROPERTY_TAX":
                    propertyTaxEmp.propertyInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "EWASTE_MANAGEMENT_SYSTEM":
                    eWasteEmp.eWasteInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM":
                    obpasEmp.obpasInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC":
                    obpasOcEmp.obpasOcInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "WATER_TANKER":
                    waterTankerEmployee.waterTankerInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "TREE_PRUNING":
                    treePruningEmp.treePruningInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "TREE_PRUNING_VERIFIER":
                    treePruningVerifier.treePruningInboxVerifier(baseUrl, username, password, applicationNumber);
                    break;

                case "MOBILE_TOILET":
                    mobileToiletEmp.mobileToiletInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "COMMUNITY_HALL_BOOKING":
                    chbEmp.chbInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "CONSTRUCTION_AND_DEMOLITION":
                    cndEmp.cndInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "PUBLIC_GRIEVANCE_REDRESSAL":
                    pgrEmp.pgrInboxEmp(baseUrl, username, password, applicationNumber);
                    break;

                case "WATER_AND_SEWERAGE":
                    if (applicationNumber.startsWith("SW")) {
                        sewerageEmp.sewerageInboxEmp(baseUrl, username, password, applicationNumber);
                    } else {
                        waterEmp.waterInboxEmp(baseUrl, username, password, applicationNumber);
                    }
                    break;

                default:
                    logger.error("Unknown module: {}", moduleName);
                    throw new RuntimeException("Unknown module: " + moduleName);
            }

            logger.info("{} employee test completed", moduleName);

        } catch (Exception e) {
            logger.error("Error in {} employee test: {}", moduleName, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}