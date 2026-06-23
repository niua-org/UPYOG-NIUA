package org.upyog.Automation.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Controller.ModuleTestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class ModuleTestService {

    private static final Logger logger =
            LoggerFactory.getLogger(ModuleTestService.class);

    @Autowired
    private WorkflowExecutor workflowExecutor;

    public String runModule(ModuleTestController.ModuleRequest request) {

        String moduleName = request.getModuleName();

        String citizenUrl = request.getBaseUrl();

        System.out.println(
                "Module Received: " + moduleName
        );

        if (moduleName.contains(",")) {

            String[] modules = moduleName.split(",");

            for (String module : modules) {

                System.out.println(
                        "RUNNING MODULE = "
                                + module
                );

                executeSingleModule(
                        module.trim(),
                        citizenUrl
                );
                System.out.println(
                        "COMPLETED MODULE = "
                                + module
                );
            }

            return "Multiple Workflows Executed";
        }

        return executeSingleModule(
                moduleName,
                citizenUrl
        );
    }

    private String executeSingleModule(
            String moduleName,
            String citizenUrl
    ) {

        switch (moduleName.toUpperCase()) {

            case "DESLUDGING_SERVICE":

                workflowExecutor.executeWorkflow(
                        "test-config/desludging/desludging_workflow.json",
                        "test-config/desludging/desludging_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "PET_REGISTRATION":

                workflowExecutor.executeWorkflow(
                        "test-config/pet/pet_workflow.json",
                        "test-config/pet/pet_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "EWASTE_MANAGEMENT_SYSTEM":

                workflowExecutor.executeWorkflow(
                        "test-config/ewaste/ewaste_workflow.json",
                        "test-config/ewaste/ewaste_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "WATER_TANKER":

                workflowExecutor.executeWorkflow(
                        "test-config/requestService/water_tanker_workflow.json",
                        "test-config/requestService/water_tanker_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            default:

                System.out.println(
                        "DEFAULT CASE HIT : " + moduleName
                );

                return "Unsupported Module : " + moduleName;
        }
    }
}