package org.upyog.Automation.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.upyog.Automation.Utils.AutomationConstants;
import org.upyog.Automation.Utils.WorkflowDataStore;
import org.upyog.Automation.model.ModuleExecutionResult;
import org.upyog.Automation.model.ModuleRequest;
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

    public List<ModuleExecutionResult> runModule(ModuleRequest request) {

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
                                        AutomationConstants.STATUS_PASS,
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
                                    AutomationConstants.STATUS_FAIL,
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
                            AutomationConstants.STATUS_PASS,
                            "Executed Successfully"
                    )
            );

        } catch (Exception e) {

            return List.of(
                    new ModuleExecutionResult(
                            moduleName,
                            AutomationConstants.STATUS_FAIL,
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

            case AutomationConstants.MODULE_PET_REGISTRATION ->
                    AutomationConstants.SHEET_PET;

            case AutomationConstants.MODULE_PUBLIC_GRIEVANCE_REDRESSAL ->
                    AutomationConstants.SHEET_PGR;

            case AutomationConstants.MODULE_NO_DUE_CERTIFICATE ->
                    AutomationConstants.SHEET_NDC;

            case AutomationConstants.MODULE_PROPERTY_TAX ->
                    AutomationConstants.SHEET_PT;

            case AutomationConstants.MODULE_ADVERTISEMENT ->
                    AutomationConstants.SHEET_ADVERTISEMENT;

            case AutomationConstants.MODULE_STREET_VENDING ->
                    AutomationConstants.SHEET_STREET_VENDING;

            case AutomationConstants.MODULE_TRADE_LICENSE ->
                    AutomationConstants.SHEET_TRADE_LICENSE;

            case AutomationConstants.MODULE_TREE_PRUNING ->
                    AutomationConstants.SHEET_TREE_PRUNING;

            case AutomationConstants.MODULE_WATER_TANKER ->
                    AutomationConstants.SHEET_WATER_TANKER;

            case AutomationConstants.MODULE_MOBILE_TOILET ->
                    AutomationConstants.SHEET_MOBILE_TOILET;

            case AutomationConstants.MODULE_OBPAS ->
                    AutomationConstants.SHEET_OBPAS;

            case AutomationConstants.MODULE_OBPAS_OC ->
                    AutomationConstants.SHEET_OBPAS_OC_CREATE;

            case AutomationConstants.MODULE_EWASTE ->
                    AutomationConstants.SHEET_EWASTE;

            case AutomationConstants.MODULE_CHB ->
                    AutomationConstants.SHEET_CHB;

            case AutomationConstants.MODULE_CND ->
                    AutomationConstants.SHEET_CND;

            case AutomationConstants.MODULE_DESLUDGING ->
                    AutomationConstants.SHEET_DESLUDGING;

            case AutomationConstants.MODULE_DESLUDGING_PAYMENT ->
                    AutomationConstants.SHEET_DESLUDGING_PAYMENT;

            case AutomationConstants.MODULE_DESLUDGING_PAYMENT2 ->
                    AutomationConstants.SHEET_DESLUDGING_PAYMENT2;

            case AutomationConstants.MODULE_WATER_AND_SEWERAGE ->
                    AutomationConstants.SHEET_WATER_AND_SEWERAGE;

            case AutomationConstants.MODULE_GARBAGE_COLLECTION ->
                    AutomationConstants.SHEET_GC;

            case AutomationConstants.MODULE_GARBAGE_COLLECTION_PAYMENT ->
                    AutomationConstants.SHEET_GC_PAYMENT;

            case AutomationConstants.MODULE_ESTATE_MANAGEMENT ->
                    AutomationConstants.SHEET_ESTATE_MANAGEMENT;

            case AutomationConstants.MODULE_ASSET_MANAGEMENT ->
                    AutomationConstants.SHEET_ASSET;

            case AutomationConstants.MODULE_CHALLAN_GENERATION ->
                    AutomationConstants.SHEET_CHALLAN;

            case AutomationConstants.MODULE_DESLUDGING_EMP_UPDATE ->
                    AutomationConstants.SHEET_DESLUDGING_EMP_UPDATE;

            case AutomationConstants.MODULE_DESLUDGING_EMP_COMPLETE ->
                    AutomationConstants.SHEET_DESLUDGING_EMP_COMPLETE;

            default ->
                    moduleName + AutomationConstants.SHEET_SUFFIX;
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

                WorkflowDataStore.put(AutomationConstants.KEY_CURRENT_MODULE, moduleName);
                WorkflowDataStore.put(AutomationConstants.KEY_CURRENT_TEST_CASE, testCase);

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
                                AutomationConstants.STATUS_PASS,
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
                    WorkflowDataStore.get(AutomationConstants.KEY_FAILED_STEP);

            String failedError =
                    WorkflowDataStore.get(AutomationConstants.KEY_FAILED_ERROR);

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
                    AutomationConstants.STATUS_FAIL,
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

            case AutomationConstants.MODULE_DESLUDGING:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_DESLUDGING_WORKFLOW,
                        AutomationConstants.CONFIG_DESLUDGING_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_PET_REGISTRATION:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_PET_WORKFLOW,
                        AutomationConstants.CONFIG_PET_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_EWASTE:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_EWASTE_WORKFLOW,
                        AutomationConstants.CONFIG_EWASTE_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_WATER_TANKER:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_WATER_TANKER_WORKFLOW,
                        AutomationConstants.CONFIG_WATER_TANKER_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_TREE_PRUNING:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_TREE_PRUNING_WORKFLOW,
                        AutomationConstants.CONFIG_TREE_PRUNING_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_MOBILE_TOILET:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_MOBILE_TOILET_WORKFLOW,
                        AutomationConstants.CONFIG_MOBILE_TOILET_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_STREET_VENDING:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_STREET_VENDING_WORKFLOW,
                        AutomationConstants.CONFIG_STREET_VENDING_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_TRADE_LICENSE:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_TRADE_LICENSE_WORKFLOW,
                        AutomationConstants.CONFIG_TRADE_LICENSE_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_ADVERTISEMENT:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_ADVERTISEMENT_WORKFLOW,
                        AutomationConstants.CONFIG_ADVERTISEMENT_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_PROPERTY_TAX:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_PROPERTY_TAX_WORKFLOW,
                        AutomationConstants.CONFIG_PROPERTY_TAX_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_PUBLIC_GRIEVANCE_REDRESSAL:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_PGR_WORKFLOW,
                        AutomationConstants.CONFIG_PGR_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_OBPAS:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_OBPAS_WORKFLOW,
                        AutomationConstants.CONFIG_OBPAS_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;


            case AutomationConstants.MODULE_CHB:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_CHB_WORKFLOW,
                        AutomationConstants.CONFIG_CHB_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;


            case AutomationConstants.MODULE_CND:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_CND_WORKFLOW,
                        AutomationConstants.CONFIG_CND_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;


            case AutomationConstants.MODULE_WATER_AND_SEWERAGE:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_WATER_AND_SEWERAGE_WORKFLOW,
                        AutomationConstants.CONFIG_WATER_AND_SEWERAGE_STAKEHOLDER,
                        citizenUrl
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_ASSET_MANAGEMENT:

                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_ASSET_WORKFLOW,
                        AutomationConstants.CONFIG_ASSET_STAKEHOLDER,
                        citizenUrl.replace(AutomationConstants.CITIZEN_LOGIN_PATH, AutomationConstants.EMPLOYEE_LOGIN_PATH)
                );

                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_GARBAGE_COLLECTION:
                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_GARBAGE_COLLECTION_WORKFLOW,
                        AutomationConstants.CONFIG_GARBAGE_COLLECTION_STAKEHOLDER,
                        citizenUrl
                );
                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_ESTATE_MANAGEMENT:
                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_ESTATE_MANAGEMENT_WORKFLOW,
                        AutomationConstants.CONFIG_ESTATE_MANAGEMENT_STAKEHOLDER,
                        citizenUrl
                );
                return AutomationConstants.MSG_WORKFLOW_EXECUTED;

            case AutomationConstants.MODULE_CHALLAN_GENERATION:
                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_CHALLAN_WORKFLOW,
                        AutomationConstants.CONFIG_CHALLAN_STAKEHOLDER,
                        citizenUrl
                );
                return AutomationConstants.MSG_WORKFLOW_EXECUTED;


            case AutomationConstants.MODULE_NO_DUE_CERTIFICATE:
                workflowExecutor.executeWorkflow(
                        AutomationConstants.CONFIG_NDC_WORKFLOW,
                        AutomationConstants.CONFIG_NDC_STAKEHOLDER,
                        citizenUrl
                );
                return AutomationConstants.MSG_WORKFLOW_EXECUTED;




            default:

                logger.info(
                        "DEFAULT CASE HIT : " + moduleName
                );

                return "Unsupported Module : " + moduleName;
        }
    }
}