package org.upyog.Automation.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.upyog.Automation.Modules.CnD.CndVendor;
import org.upyog.Automation.Modules.RequestService.MobileToiletVendor;
import org.upyog.Automation.Modules.RequestService.WaterTankerVendor;

@Component
public class CommonVendorTest {

    private static final Logger logger = LoggerFactory.getLogger(CommonVendorTest.class);

    @Autowired
    private WaterTankerVendor waterTankerVendor;

    @Autowired
    private MobileToiletVendor mobileToiletVendor;

    @Autowired
    private CndVendor cndVendor;

    public void runVendorTest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String applicationNumber) throws InterruptedException{
        logger.info("Starting {} vendor test", moduleName);

        try {
            switch (moduleName.toUpperCase()) {

                case "WATER_TANKER_VENDOR":
                    waterTankerVendor.waterTankerVCreate(baseUrl, moduleName, mobileNumber, otp, cityName, applicationNumber);
                    break;

                case "MOBILE_TOILET_VENDOR":
                    mobileToiletVendor.mobileToiletVCreate(baseUrl, moduleName, mobileNumber, otp, cityName, applicationNumber);
                    break;

                case "CONSTRUCTION_AND_DEMOLITION":
                    cndVendor.cndVReg(baseUrl, moduleName, mobileNumber, otp, cityName, applicationNumber);
                    break;

                default:
                    logger.error("Unknown vendor module: {}", moduleName);
                    throw new RuntimeException("Unknown vendor module: " + moduleName);
            }

            logger.info("{} vendor test completed", moduleName);

        } catch (Exception e) {
            logger.error("Error in {} vendor test: {}", moduleName, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
