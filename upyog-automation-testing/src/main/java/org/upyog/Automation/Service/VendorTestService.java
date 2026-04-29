package org.upyog.Automation.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Common.CommonVendorTest;

@Service
public class VendorTestService {

    private static final Logger logger = LoggerFactory.getLogger(VendorTestService.class);

    @Autowired
    private CommonVendorTest commonVendorTest;

    public String runVendorSideTest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String applicationNumber) {
        logger.info("Starting {} vendor test", moduleName);

        new Thread(() -> {
            try {
                // CommonVendorTest test = new CommonVendorTest();
                commonVendorTest.runVendorTest(baseUrl, moduleName, mobileNumber, otp, cityName, applicationNumber);
            } catch (Exception e) {
                logger.error("Error in vendor test: {}", e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return moduleName + " test started successfully. Check browser for automation.";
    }
}
