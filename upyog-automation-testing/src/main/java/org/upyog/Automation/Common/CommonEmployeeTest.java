package org.upyog.Automation.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Base.BaseTest;
import org.upyog.Automation.Modules.Adv.AdvEmp;
import org.upyog.Automation.Modules.Asset.AssetEmp;
import org.upyog.Automation.Modules.CHB.chbEmp;
import org.upyog.Automation.Modules.CnD.CnDEmp;
import org.upyog.Automation.Modules.EWaste.EWasteEmp;
import org.upyog.Automation.Modules.OBPAS.OBPASEmp;
import org.upyog.Automation.Modules.OBPAS.OBPASOcEmp;
import org.upyog.Automation.Modules.Pet.PetApplicationEmp;
import org.upyog.Automation.Modules.PropertyTax.PropertyTaxEmp;
import org.upyog.Automation.Modules.PublicGrievanceRedressal.PgrEmp;
import org.upyog.Automation.Modules.RequestService.MobileToiletEmp;
import org.upyog.Automation.Modules.RequestService.TreePruningEmp;
import org.upyog.Automation.Modules.RequestService.TreePruningVerifier;
import org.upyog.Automation.Modules.StreetVending.SvEmp;
import org.upyog.Automation.Modules.TradeLicense.InboxEmpTl;
import org.upyog.Automation.Modules.TradeLicense.TradeLicenseEmp;
import org.upyog.Automation.Modules.WaterAndSewerage.SewerageEmp;
import org.upyog.Automation.Modules.WaterAndSewerage.WaterEmp;
import org.upyog.Automation.Modules.RequestService.WaterTankerEmployee;
import org.upyog.Automation.Utils.ModuleWrapper;

/**
 * Common entry point for all employee module tests
 * Routes to appropriate module based on moduleName
 */
@Component
public class CommonEmployeeTest extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonEmployeeTest.class);

    @Autowired
    private SvEmp svEmp;

    @Autowired
    private PetApplicationEmp petApplicationEmp;
    
    @Autowired
    private TradeLicenseEmp tradeLicenseEmp;

    @Autowired
    private InboxEmpTl inboxEmpTl;
    
    @Autowired
    private AssetEmp assetEmp;

    @Autowired
    private AdvEmp advEmp;

    @Autowired
    private PropertyTaxEmp propertyTaxEmp;

    @Autowired
    private EWasteEmp eWasteEmp;

    @Autowired
    private OBPASEmp obpasEmp;

    @Autowired
    private OBPASOcEmp obpasOcEmp;

    @Autowired
    private WaterTankerEmployee waterTankerEmployee;

    @Autowired
    private TreePruningEmp treePruningEmp;

    @Autowired
    private TreePruningVerifier treePruningVerifier;

    @Autowired
    private MobileToiletEmp mobileToiletEmp;

    @Autowired
    private chbEmp chbEmp;

    @Autowired
    private CnDEmp cndEmp;

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


//                case "STREET_VENDING":
//                    svEmp.inboxEmpSv(baseUrl, username, password, applicationNumber);
//                    break;

                case "STREET_VENDING":

                    ModuleWrapper.execute(
                            "STREET_VENDING",
                            () -> svEmp.inboxEmpSv(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "PET_REGISTRATION":
//                    petApplicationEmp.petInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "PET":

                    ModuleWrapper.execute(
                            "PET",
                            () -> petApplicationEmp.petInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "TRADE_LICENSE":
//                    tradeLicenseEmp.tlInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "TRADE_LICENSE":

                    ModuleWrapper.execute(
                            "TRADE_LICENSE",
                            () -> tradeLicenseEmp.tlInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

                case "TRADE_LICENSE1":
                    inboxEmpTl.inboxEmpTl(baseUrl, username, password, applicationNumber);
                    break;

//                case "ASSET_MANAGEMENT_SYSTEM":
//                    assetEmp.assetInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "ASSET_MANAGEMENT_SYSTEM":

                    ModuleWrapper.execute(
                            "ASSET_EMP",
                            () -> assetEmp.assetEmployeeFlow(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "ADVERTISEMENT":
//                    advEmp.advInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "ADVERTISEMENT":

                    ModuleWrapper.execute(
                            "ADV_EMP",
                            () -> advEmp.advApproval(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "PROPERTY_TAX":
//                    propertyTaxEmp.propertyInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "PROPERTY_TAX":

                    ModuleWrapper.execute(
                            "PROPERTY_TAX",
                            () -> propertyTaxEmp.propertyInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "EWASTE_MANAGEMENT_SYSTEM":
//                    eWasteEmp.eWasteInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "EWASTE_MANAGEMENT_SYSTEM":

                    ModuleWrapper.execute(
                            "EWASTE_EMP",
                            () -> eWasteEmp.eWasteApproval(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM":
//                    obpasEmp.OBPASInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "OBPAS_EMP":

                    ModuleWrapper.execute(
                            "OBPAS_EMP",
                            () -> obpasEmp.OBPASInbox(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC":
//                    obpasOcEmp.OBPASOcInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "OBPAS_OC":

                    ModuleWrapper.execute(
                            "OBPAS_OC",
                            () -> obpasOcEmp.OBPASOcInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "WATER_TANKER":
//                    waterTankerEmployee.waterTankerInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "WATER_TANKER":

                    ModuleWrapper.execute(
                            "WATER_TANKER",
                            () -> waterTankerEmployee.waterTankerInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "TREE_PRUNING":
//                    treePruningEmp.treePruningInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "TREE_PRUNING":

                    ModuleWrapper.execute(
                            "TREE_PRUNING",
                            () -> treePruningEmp.treePruningInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "TREE_PRUNING_VERIFIER":
//                    treePruningVerifier.treePruningInboxVerifier(baseUrl, username, password, applicationNumber);
//                    break;

                case "TREE_PRUNING_VENDOR":

                    ModuleWrapper.execute(
                            "TREE_PRUNING_VENDOR",
                            () -> treePruningVerifier.treePruningInboxVerifier(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "MOBILE_TOILET":
//                    mobileToiletEmp.mobileToiletInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "MOBILE_TOILET":

                    ModuleWrapper.execute(
                            "MOBILE_TOILET",
                            () -> mobileToiletEmp.mobileToiletInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "COMMUNITY_HALL_BOOKING":
//                    chbEmp.chbInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "COMMUNITY_HALL_BOOKING":

                    ModuleWrapper.execute(
                            "CHB",
                            () -> chbEmp.chbInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "CONSTRUCTION_AND_DEMOLITION":
//                    cndEmp.CnDInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;
                case "CONSTRUCTION_AND_DEMOLITION":

                    ModuleWrapper.execute(
                            "CND_EMP",
                            () -> cndEmp.cndApproval(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "PUBLIC_GRIEVANCE_REDRESSAL":
//                    pgrEmp.pgrInboxEmp(baseUrl, username, password, applicationNumber);
//                    break;

                case "PGR":

                    ModuleWrapper.execute(
                            "PGR",
                            () -> pgrEmp.pgrInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

//                case "WATER_AND_SEWERAGE":
//                    if (applicationNumber.startsWith("SW")) {
//                        sewerageEmp.sewerageInboxEmp(baseUrl, username, password, applicationNumber);
//                    } else {
//                        waterEmp.waterInboxEmp(baseUrl, username, password, applicationNumber);
//                    }
//                    break;

                case "SEWERAGE":

                    ModuleWrapper.execute(
                            "SEWERAGE",
                            () -> sewerageEmp.sewerageInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

                    break;

                case "WATER":

                    ModuleWrapper.execute(
                            "WATER",
                            () -> waterEmp.waterInboxEmp(
                                    driver,
                                    wait,
                                    js
                            )
                    );

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