package org.upyog.Automation.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Common.CommonVendorTest;
import org.upyog.Automation.Utils.WorkflowDataStore;

@Service
public class VendorTestService {

    private static final Logger logger =
            LoggerFactory.getLogger(VendorTestService.class);

    @Autowired
    private CommonVendorTest commonVendorTest;

    public String runVendorTest(
            String baseUrl,
            String moduleName,
            String mobileNumber,
            String otp,
            String cityName,
            String applicationNumber) {

        WorkflowDataStore.put("selected.mobile", mobileNumber);
        WorkflowDataStore.put("selected.otp", otp);
        WorkflowDataStore.put("selected.city", cityName);
        WorkflowDataStore.put("selected.applicationNumber", applicationNumber);
        WorkflowDataStore.put("selected.url", baseUrl);

        String env = baseUrl.contains("niuatt")
                ? "NIUATT"
                : "UPYOG";

        WorkflowDataStore.put("selected.env", env);

        logger.info("Selected ENV: {}", env);

        new Thread(() -> {
            try {
                commonVendorTest.runVendorTest(
                        baseUrl,
                        moduleName,
                        mobileNumber,
                        otp,
                        cityName,
                        applicationNumber
                );
            } catch (Exception e) {
                logger.error("Error in vendor test", e);
            }
        }).start();

        return moduleName + " vendor test started successfully.";
    }
}