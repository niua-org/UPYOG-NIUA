package org.upyog.Automation.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Reports.ExtentManager;
import org.upyog.Automation.Reports.ReportManager;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.upyog.Automation.config.EmployeeData;
import org.upyog.Automation.config.ModuleData;
import org.upyog.Automation.config.StakeholderConfigLoader;
import org.upyog.Automation.model.WorkflowData;
import org.upyog.Automation.model.WorkflowStep;
import org.upyog.Automation.config.WorkflowConfigLoader;

@Service
public class WorkflowExecutor {

    @Autowired
    private CitizenTestService citizenTestService;

    @Autowired
    private EmployeeTestService employeeTestService;

    public void executeWorkflow(
            String workflowPath,
            String stakeholderPath,
            String citizenUrl
    )

    {
        String employeeUrl =
                citizenUrl.replace(
                        "/citizen/login",
                        "/employee/login"
                );

        WorkflowData workflow =
                WorkflowConfigLoader.load(workflowPath);
        WorkflowDataStore.remove("APPLICATION_NO");

        System.out.println(
                "APPLICATION_NO RESET"
        );

        ModuleData stakeholder =
                StakeholderConfigLoader.load(
                        stakeholderPath
                );
        System.out.println(
                "NEW REPORT CREATED FOR = "
                        + workflow.getModuleName()
        );
        ExtentManager.reset();
        ReportManager.clearTest();

        System.out.println(
                "AFTER RESET TEST = "
                        + ReportManager.getTest()
        );

        ReportManager.startTest(
                workflow.getModuleName(),
                workflow.getModuleName()
        );

        for (WorkflowStep step : workflow.getSteps()) {
            try {
                System.out.println(
                        "STEP TYPE = " + step.getType()
                );

                System.out.println(
                        "STEP MODULE = " + step.getModule()
                );

                System.out.println(
                        "STEP ROLE = " + step.getRole()
                );

            ReportManager.logFlow(
                    step.getName()
            );

            ReportManager.logStep(
                    "Executing : " + step.getModule()
            );

            System.out.println(
                    "Executing : "
                            + step.getModule()
            );

            if ("CITIZEN".equalsIgnoreCase(step.getType())) {
                System.out.println(
                        "CALLING CITIZEN SERVICE"
                );

                citizenTestService.runCitizenSideTest(
                        citizenUrl,
                        step.getModule(),
                        stakeholder.getCitizen().getMobile(),
                        stakeholder.getCitizen().getOtp(),
                        stakeholder.getCitizen().getCity(),
                        null
                );
            }

            else if ("EMPLOYEE".equalsIgnoreCase(step.getType())) {

                EmployeeData employee =
                        stakeholder.getEmployeeByRole(
                                step.getRole()
                        );

                if (employee == null) {

                    throw new RuntimeException(
                            "Role not found in stakeholder file : "
                                    + step.getRole()
                    );
                }
                System.out.println(
                        "APPLICATION_NO = "
                                + WorkflowDataStore.get("APPLICATION_NO")
                );

                String applicationNo =
                        WorkflowDataStore.get("APPLICATION_NO");

                if (applicationNo == null ||
                        applicationNo.isBlank()) {

                    throw new RuntimeException(
                            "APPLICATION_NO not found in WorkflowDataStore"
                    );

                }
                System.out.println(
                        "CALLING EMPLOYEE SERVICE"
                );

                employeeTestService.runEmployeeTest(
                        employeeUrl,
                        step.getModule(),
                        employee.getUsername(),
                        employee.getPassword(),
                        applicationNo
                );
            }
                ReportManager.logStep(
                        "PASSED : "
                                + step.getModule()
                );

            } catch (Exception e) {

                e.printStackTrace();

                ReportManager.logStep(
                        "FAILED : "
                                + step.getModule()
                                + " | "
                                + e.getMessage()
                );
            }
        }

        ReportManager.flush();

        System.out.println(
                "REPORT SAVED = "
                        + workflow.getModuleName()
        );

        ReportManager.clearTest();

        ExtentManager.reset();

    }
}