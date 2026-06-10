package org.upyog.Automation.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Controller.ModuleTestController;
import org.upyog.Automation.Utils.JsonConfigLoader;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Reports.ReportManager;
import org.upyog.Automation.config.ModuleData;
import org.upyog.Automation.config.StakeholderConfigLoader;

@Service
public class ModuleTestService {

    private static final Logger logger =
            LoggerFactory.getLogger(ModuleTestService.class);

    @Autowired
    private CitizenTestService citizenTestService;

    @Autowired
    private EmployeeTestService employeeTestService;

    public String runModule(ModuleTestController.ModuleRequest request) {

        String moduleName = request.getModuleName();

        String citizenUrl = request.getBaseUrl();

        String employeeUrl =
                citizenUrl.replace(
                        "/citizen/login",
                        "/employee/login"
                );

        System.out.println(
                "Module Received: " + moduleName
        );

        switch (moduleName.toUpperCase()) {

            case "PET_REGISTRATION":
                ModuleData moduleData =
                        StakeholderConfigLoader.load(
                                "test-config/pet/pet_stakeholder_module.json"
                        );

                logger.info(
                        "Citizen Mobile : {}",
                        moduleData.getCitizen().getMobile()
                );

                logger.info(
                        "Employee Username : {}",
                        moduleData.getEmployee().getUsername()
                );

                if (moduleData == null) {
                    return "No configuration found for PET_REGISTRATION";
                }

                // Step 1 Citizen
                ReportManager.startTest("Pet Registration - Full Flow");

                ReportManager.logStep("CITIZEN FLOW STARTED");

                logger.info("CITIZEN FLOW STARTED");
                citizenTestService.runCitizenSideTest(
                        citizenUrl,
                        "PET_REGISTRATION",
                        moduleData.getCitizen().getMobile(),
                        moduleData.getCitizen().getOtp(),
                        moduleData.getCitizen().getCity(),
                        null
                );
                ReportManager.logStep("CITIZEN FLOW COMPLETED");

                logger.info("CITIZEN FLOW COMPLETED");

                // Step 2 Get Application Number
                String applicationNo =
                        WorkflowDataStore.get("APPLICATION_NO");

                logger.info(
                        "APPLICATION_NO = {}",
                        applicationNo
                );

                ReportManager.logStep(
                        "Application Number : " + applicationNo
                );

                // Safety Check
                if (applicationNo == null || applicationNo.isEmpty()) {
                    return "Application Number not captured";
                }

                // Step 3 Employee
                ReportManager.logStep("EMPLOYEE FLOW STARTED");

                logger.info("EMPLOYEE FLOW STARTED");
                employeeTestService.runEmployeeTest(
                        employeeUrl,
                        "PET_REGISTRATION",
                        moduleData.getEmployee().getUsername(),
                        moduleData.getEmployee().getPassword(),
                        applicationNo
                );
                ReportManager.logStep("EMPLOYEE FLOW COMPLETED");

                logger.info("EMPLOYEE FLOW COMPLETED");

                ReportManager.flush();

                return "PET Module Citizen + Employee Flow Started";
            default:

                System.out.println("DEFAULT CASE HIT");
        }
        return "Unsupported Module : " + moduleName;
    }
}