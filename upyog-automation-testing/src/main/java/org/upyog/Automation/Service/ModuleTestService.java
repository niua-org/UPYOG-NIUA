package org.upyog.Automation.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.upyog.Automation.Controller.ModuleTestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.upyog.Automation.model.ModuleExecutionResult;
import org.upyog.Automation.Utils.ExcelDataReader;

import java.util.Map;

import java.util.ArrayList;
import java.util.List;


@Service
public class ModuleTestService {

    private static final Logger logger =
            LoggerFactory.getLogger(ModuleTestService.class);
    // =========================
// EXECUTION PROGRESS
// =========================

    private volatile int totalTestCases = 0;
    private volatile int completedTestCases = 0;
    private volatile String currentTestCase = "";
    private volatile String currentModule = "";
    private volatile boolean executionRunning = false;

    @Autowired
    private WorkflowExecutor workflowExecutor;

    public List<ModuleExecutionResult> runModule(ModuleTestController.ModuleRequest request) {

        String moduleName = request.getModuleName();

        String citizenUrl = request.getBaseUrl();

        logger.info(
                "Module Received: " + moduleName
        );

        if (moduleName.contains(",")) {

            String[] modules = moduleName.split(",");

            List<ModuleExecutionResult> results =
                    new ArrayList<>();

            for (String module : modules) {

                module = module.trim();

                logger.info(
                        "RUNNING MODULE = {}",
                        module
                );

                try {

                    List<ModuleExecutionResult> excelResults =
                            executeFromExcelIfAvailable(
                                    module,
                                    citizenUrl
                            );

                    if (!excelResults.isEmpty()) {

                        results.addAll(excelResults);

                    } else {

                        // No Excel sheet / no Execute = YES
                        // existing dev.properties workflow
                        executeSingleModule(
                                module,
                                citizenUrl
                        );

                        results.add(
                                new ModuleExecutionResult(
                                        module,
                                        "PASS",
                                        "Executed Successfully"
                                )
                        );
                    }

                } catch (Exception e) {

                    logger.error(
                            "FAILED TO EXECUTE MODULE = {}",
                            module,
                            e
                    );

                    results.add(
                            new ModuleExecutionResult(
                                    module,
                                    "FAIL",
                                    e.getMessage()
                            )
                    );
                }
            }

            return results;
        }

        try {

            List<ModuleExecutionResult> excelResults =
                    executeFromExcelIfAvailable(
                            moduleName,
                            citizenUrl
                    );

            if (!excelResults.isEmpty()) {
                return excelResults;
            }

            // No Excel sheet / no Execute = YES
            // Run normal workflow using dev.properties
            executeSingleModule(
                    moduleName,
                    citizenUrl
            );

            return List.of(
                    new ModuleExecutionResult(
                            moduleName,
                            "PASS",
                            "Executed Successfully"
                    )
            );

        } catch (Exception e) {

            return List.of(
                    new ModuleExecutionResult(
                            moduleName,
                            "FAIL",
                            e.getMessage()
                    )
            );
        }
    }

    private List<ModuleExecutionResult> executeFromExcelIfAvailable(
            String moduleName,
            String citizenUrl
    ) {

        List<ModuleExecutionResult> results =
                new ArrayList<>();

        String sheetName = switch (moduleName.toUpperCase()) {

            case "PET_REGISTRATION" ->
                    "PET_Test_Data";

            case "PUBLIC_GRIEVANCE_REDRESSAL" ->
                    "PGR_Test_Data";

            case "NO_DUE_CERTIFICATE" ->
                    "NDC_Test_Data";

            case "PROPERTY_TAX" ->
                    "PT_Test_Data";

            case "ADVERTISEMENT" ->
                    "Advertisement_Test_Data";

            case "STREET_VENDING" ->
                    "StreetVending_Test_Data";

            case "TRADE_LICENSE" ->
                    "TradeLicense_Test_Data";

            case "TREE_PRUNING" ->
                    "TreePruning_Test_Data";

            case "WATER_TANKER" ->
                    "WaterTanker_Test_Data";

            case "MOBILE_TOILET" ->
                    "MobileToilet_Test_Data";

            case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM" ->
                    "OBPAS_Test_Data";

            case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC" ->
                    "OBPAS_OC_Create_Test_Data";

            case "EWASTE_MANAGEMENT_SYSTEM" ->
                    "EWaste_Test_Data";

            case "COMMUNITY_HALL_BOOKING" ->
                    "CHB_Test_Data";

            case "CONSTRUCTION_AND_DEMOLITION" ->
                    "CND_Test_Data";

            case "DESLUDGING_SERVICE" ->
                    "Desludging_Test_Data";

            case "DESLUDGING_SERVICE_PAYMENT" ->
                    "Desludging_Payment_Test_Data";

            case "DESLUDGING_SERVICE_PAYMENT2" ->
                    "Desludging_Payment2_Test_Data";

            case "WATER_AND_SEWERAGE" ->
                    "WaterAndSewerage_Test_Data";

            case "GARBAGE_COLLECTION" ->
                    "GC_Test_Data";

            case "GARBAGE_COLLECTION_PAYMENT" ->
                    "GC_Payment_Test_Data";

            case "ESTATE_MANAGEMENT" ->
                    "EstateManagement_Test_Data";

            case "ASSET_MANAGEMENT_SYSTEM" ->
                    "Asset_Test_Data";

            case "CHALLAN_GENERATION" ->
                    "Challan_Test_Data";

            case "DESLUDGING_EMPLOYEE_UPDATE" ->
                    "Desludging_Employee_Update_Test_Data";

            case "DESLUDGING_EMPLOYEE_COMPLETE" ->
                    "Desludging_Employee_Complete_Test_Data";

            case "DESLUDGING_EMPLOYEE_PSSO" ->
                    "Desludging_Employee_PSSO_Test_Data";

            case "DESLUDGING_EMPLOYEE_FSTPO" ->
                    "Desludging_Employee_FSTPO_Test_Data";

            case "ASSET_MANAGEMENT_SYSTEM_VERIFIER" ->
                    "Asset_Verifier_Test_Data";

            case "ASSET_MANAGEMENT_SYSTEM_APPROVER" ->
                    "Asset_Approver_Test_Data";

            case "TRADE_LICENSE1" ->
                    "TradeLicense1_Test_Data";

            case "SEWERAGE_EMP" ->
                    "Sewerage_Test_Data";

            case "WATER_EMP" ->
                    "Water_Test_Data";

            default ->
                    moduleName + "_Test_Data";
        };

        logger.info(
                "Checking Excel sheet: {}",
                sheetName
        );

        // Excel sheet does not exist
        if (!ExcelDataReader.hasSheet(sheetName)) {

            logger.info(
                    "Excel sheet not found: {}. Using normal workflow.",
                    sheetName
            );

            return results;
        }

        logger.info(
                "Excel sheet found: {}",
                sheetName
        );

        List<Map<String, String>> testCases =
                ExcelDataReader.readSheet(sheetName);

        // Sheet exists but no Execute = YES rows
        if (testCases.isEmpty()) {

            logger.info(
                    "No executable Excel test cases found in {}. Using normal workflow.",
                    sheetName
            );

            return results;
        }

        logger.info(
                "Excel test cases found: {}",
                testCases.size()
        );

        // Initialize execution progress
        totalTestCases = testCases.size();
        completedTestCases = 0;
        currentModule = moduleName;
        currentTestCase = "";
        executionRunning = true;

        for (Map<String, String> testData : testCases) {

            String testCase =
                    testData.getOrDefault(
                            "TestCase",
                            "UNKNOWN"
                    );

            currentTestCase = testCase;

            logger.info(
                    "EXECUTION PROGRESS: {} / {} | {}",
                    completedTestCases + 1,
                    totalTestCases,
                    testCase
            );

            logger.info(
                    "========== STARTING EXCEL TEST CASE: {} ==========",
                    testCase
            );

            try {

                // Clear previous Excel data
                WorkflowDataStore.clear();

                // Load current Excel row
                for (Map.Entry<String, String> entry :
                        testData.entrySet()) {

                    String key =
                            entry.getKey();

                    String value =
                            entry.getValue();

                    if (value != null
                            && !value.trim().isEmpty()) {

                        WorkflowDataStore.put(
                                key,
                                value.trim()
                        );

                        logger.info(
                                "Excel Data → {} = {}",
                                key,
                                value
                        );
                    }
                }

                // Execute the EXISTING workflow
                executeSingleModule(
                        moduleName,
                        citizenUrl
                );

// Test case passed
                results.add(
                        new ModuleExecutionResult(
                                moduleName,
                                testCase,
                                "PASS",
                                "Executed Successfully",
                                ""
                        )
                );

                completedTestCases++;

                logger.info(
                        "========== COMPLETED EXCEL TEST CASE: {} ==========",
                        testCase
                );

            } catch (Exception e) {

            String errorMessage = e.getMessage();

            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = e.getClass().getSimpleName();
            }

            String failedStep =
                    WorkflowDataStore.get("FAILED_STEP");

            String failedError =
                    WorkflowDataStore.get("FAILED_ERROR");

            if (failedStep == null || failedStep.trim().isEmpty()) {
                failedStep = "Unknown Step";
            }

            if (failedError != null && !failedError.trim().isEmpty()) {
                errorMessage = failedError;
            }

            logger.error(
                    "FAILED TEST CASE: {} | Step: {} | Error: {}",
                    testCase,
                    failedStep,
                    errorMessage,
                    e
            );

            results.add(new ModuleExecutionResult(
                    moduleName,
                    testCase,
                    "FAIL",
                    errorMessage,
                    failedStep
            ));

            completedTestCases++;
        } finally {

            WorkflowDataStore.clear();
        }
    }
        executionRunning = false;
        currentTestCase = "";

    return results;
}

// =========================
// EXECUTION PROGRESS GETTERS
// =========================

    public int getTotalTestCases() {
        return totalTestCases;
    }

    public int getCompletedTestCases() {
        return completedTestCases;
    }

    public String getCurrentTestCase() {
        return currentTestCase;
    }

    public String getCurrentModule() {
        return currentModule;
    }

    public boolean isExecutionRunning() {
        return executionRunning;
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

            case "TREE_PRUNING":

                workflowExecutor.executeWorkflow(
                        "test-config/requestService/tree_pruning_workflow.json",
                        "test-config/requestService/tree_pruning_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "MOBILE_TOILET":

                workflowExecutor.executeWorkflow(
                        "test-config/requestService/mobile_toilet_workflow.json",
                        "test-config/requestService/mobile_toilet_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "STREET_VENDING":

                workflowExecutor.executeWorkflow(
                        "test-config/streetVending/street_vending_workflow.json",
                        "test-config/streetVending/street_vending_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "TRADE_LICENSE":

                workflowExecutor.executeWorkflow(
                        "test-config/tradeLicense/trade_license_workflow.json",
                        "test-config/tradeLicense/trade_license_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "ADVERTISEMENT":

                workflowExecutor.executeWorkflow(
                        "test-config/advertisement/adv_workflow.json",
                        "test-config/advertisement/adv_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "PROPERTY_TAX":

                workflowExecutor.executeWorkflow(
                        "test-config/propertyTax/property_tax_workflow.json",
                        "test-config/propertyTax/property_tax_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "PUBLIC_GRIEVANCE_REDRESSAL":

                workflowExecutor.executeWorkflow(
                        "test-config/pgr/pgr_workflow.json",
                        "test-config/pgr/pgr_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM":

                workflowExecutor.executeWorkflow(
                        "test-config/obpas/obpas_workflow.json",
                        "test-config/obpas/obpas_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";


            case "COMMUNITY_HALL_BOOKING":

                workflowExecutor.executeWorkflow(
                        "test-config/chb/chb_workflow.json",
                        "test-config/chb/chb_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";


            case "CONSTRUCTION_AND_DEMOLITION":

                workflowExecutor.executeWorkflow(
                        "test-config/cnd/cnd_workflow.json",
                        "test-config/cnd/cnd_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";


            case "WATER_AND_SEWERAGE":

                workflowExecutor.executeWorkflow(
                        "test-config/waterAndSewerage/water_and_sewerage_workflow.json",
                        "test-config/waterAndSewerage/water_and_sewerage_stakeholder_module.json",
                        citizenUrl
                );

                return "Workflow Executed";

            case "ASSET_MANAGEMENT_SYSTEM":

                workflowExecutor.executeWorkflow(
                        "test-config/asset/asset_workflow.json",
                        "test-config/asset/asset_stakeholder_module.json",
                        citizenUrl.replace("/citizen/login", "/employee/login")
                );

                return "Workflow Executed";

            case "GARBAGE_COLLECTION":
                workflowExecutor.executeWorkflow(
                        "test-config/garbageCollection/gc_workflow.json",
                        "test-config/garbageCollection/gc_stakeholder_module.json",
                        citizenUrl
                );
                return "Workflow Executed";

            case "ESTATE_MANAGEMENT":
                workflowExecutor.executeWorkflow(
                        "test-config/estateManagement/estateManagement_workflow_module.json",
                        "test-config/estateManagement/estateManagement_stakeholder_module.json",
                        citizenUrl
                );
                return "Workflow Executed";

            case "CHALLAN_GENERATION":
                workflowExecutor.executeWorkflow(
                        "test-config/challanGeneration/cg_workflow_module.json",
                        "test-config/challanGeneration/cg_stakeholder_module.json",
                        citizenUrl
                );
                return "Workflow Executed";


            case "NO_DUE_CERTIFICATE":
                workflowExecutor.executeWorkflow(
                        "test-config/noDueCertificate/ndc_workflow.json",
                        "test-config/noDueCertificate/ndc_stakeholder_module.json",
                        citizenUrl
                );
                return "Workflow Executed";




            default:

                logger.info(
                        "DEFAULT CASE HIT : " + moduleName
                );

                return "Unsupported Module : " + moduleName;
        }
    }
}