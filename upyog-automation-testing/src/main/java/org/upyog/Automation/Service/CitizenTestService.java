package org.upyog.Automation.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Common.CommonCitizenTest;

@Service
public class CitizenTestService {

    private static final Logger logger = LoggerFactory.getLogger(CitizenTestService.class);

    @Autowired
    private CommonCitizenTest commonCitizenTest;

    public String runCitizenSideTest(String baseUrl, String moduleName, String mobileNumber, String otp, String cityName, String permitNumber) {
        logger.info("Starting {} citizen test", moduleName);

        try {
            commonCitizenTest.runCitizenTest(baseUrl, moduleName, mobileNumber, otp, cityName, permitNumber);
        } catch (Exception e) {
            logger.error("Error in citizen test: {}", e.getMessage());
            e.printStackTrace();
        }

        return moduleName + " test started successfully. Check browser for automation.";
    }
}
