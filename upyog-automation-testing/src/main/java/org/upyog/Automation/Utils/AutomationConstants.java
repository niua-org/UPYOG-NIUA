package org.upyog.Automation.Utils;

/**
 * Centralized constants used across the UPYOG automation framework.
 *
 * <p>This class contains reusable values related to Excel processing,
 * module test-config JSON paths, workflow types, workflow data keys,
 * controller endpoints, response messages, and application URLs.</p>
 */
public final class AutomationConstants {

    /**
     * Prevents instantiation of the constants utility class.
     */
    private AutomationConstants() {
        // Prevent object creation
    }

    // =========================================================================
    // Excel Configuration & MIME Constants
    // =========================================================================

    /** Path to default bundled test data template. */
    public static final String TEST_DATA_FILE =
            "test-data/test-data.xlsx";

    /** Content disposition header for downloading test data template. */
    public static final String TEST_DATA_CONTENT_DISPOSITION =
            "attachment; filename=\"UPYOG_Test_Data.xlsx\"";

    /** Content disposition header for downloading generated test results. */
    public static final String TEST_RESULT_CONTENT_DISPOSITION =
            "attachment; filename=\"UPYOG_Test_Result.xlsx\"";

    /** File extension for OpenXML Excel files. */
    public static final String XLSX_EXTENSION =
            ".xlsx";

    /** MIME content type for Excel OpenXML workbooks. */
    public static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Suffix appended to module names to derive default test data sheet names. */
    public static final String SHEET_SUFFIX =
            "_Test_Data";


    // =========================================================================
    // Temporary File Prefixes
    // =========================================================================

    /** Prefix for temporary result files generated during test execution. */
    public static final String TEST_RESULT_FILE_PREFIX =
            "upyog-test-result-";

    /** Prefix for temporary uploaded test data files. */
    public static final String TEST_DATA_FILE_PREFIX =
            "upyog-test-data-";


    // =========================================================================
    // Controller Endpoints & API Keys
    // =========================================================================

    /** Base path for module test execution APIs. */
    public static final String API_MODULE_BASE = "/api/module";

    /** Endpoint to trigger module execution. */
    public static final String ENDPOINT_RUN = "/run";

    /** Endpoint to poll current execution progress. */
    public static final String ENDPOINT_PROGRESS = "/progress";

    /** Endpoint to download the master test-data template. */
    public static final String ENDPOINT_DOWNLOAD_TEMPLATE = "/download-template";

    /** Endpoint to download generated test execution results. */
    public static final String ENDPOINT_DOWNLOAD_RESULT = "/download-result";

    /** Endpoint to upload custom Excel test-data. */
    public static final String ENDPOINT_UPLOAD_EXCEL = "/upload-excel";

    /** Progress map key for total test cases count. */
    public static final String KEY_TOTAL_TEST_CASES = "totalTestCases";

    /** Progress map key for completed test cases count. */
    public static final String KEY_COMPLETED_TEST_CASES = "completedTestCases";

    /** Progress map key for currently executing test case name. */
    public static final String KEY_CURRENT_TEST_CASE = "currentTestCase";

    /** Progress map key for currently executing module name. */
    public static final String KEY_CURRENT_MODULE = "currentModule";

    /** Progress map key indicating whether test execution is actively running. */
    public static final String KEY_EXECUTION_RUNNING = "executionRunning";


    // =========================================================================
    // API Response Messages
    // =========================================================================

    /** Error message returned when no file is uploaded. */
    public static final String MSG_EMPTY_FILE = "Please upload an Excel file.";

    /** Error message returned when uploaded file is not an OpenXML .xlsx workbook. */
    public static final String MSG_UNSUPPORTED_EXCEL_FORMAT = "Only .xlsx Excel files are supported.";

    /** Success message prefix for Excel upload. */
    public static final String MSG_EXCEL_UPLOAD_SUCCESS = "Excel uploaded successfully: ";

    /** Failure message prefix for Excel upload errors. */
    public static final String MSG_EXCEL_UPLOAD_FAILURE = "Failed to upload Excel: ";


    // =========================================================================
    // Citizen Module Test-Config JSON Paths
    // =========================================================================

    /** Path to Advertisement citizen module JSON config. */
    public static final String CONFIG_ADV_CITIZEN =
            "test-config/advertisement/adv_citizen_module.json";

    /** Path to Community Hall Booking citizen module JSON config. */
    public static final String CONFIG_CHB_CITIZEN =
            "test-config/chb/chb_citizen_module.json";

    /** Path to Construction and Demolition citizen module JSON config. */
    public static final String CONFIG_CND_CITIZEN =
            "test-config/cnd/cnd_citizen_module.json";

    /** Path to Desludging Service citizen module JSON config. */
    public static final String CONFIG_DESLUDGING_CITIZEN =
            "test-config/desludging/desludging_citizen_module.json";

    /** Path to Desludging Service first payment step JSON config. */
    public static final String CONFIG_DESLUDGING_PAYMENT =
            "test-config/desludging/desludging_citizenPayment_module.json";

    /** Path to Desludging Service second payment step JSON config. */
    public static final String CONFIG_DESLUDGING_PAYMENT2 =
            "test-config/desludging/desludging_citizenPayment2_module.json";

    /** Path to E-Waste Management citizen module JSON config. */
    public static final String CONFIG_EWASTE_CITIZEN =
            "test-config/ewaste/ewaste_citizen_module.json";

    /** Path to Estate Management citizen module JSON config. */
    public static final String CONFIG_ESTATE_CITIZEN =
            "test-config/estateManagement/estateManagement_citizen_module.json";

    /** Path to Garbage Collection citizen module JSON config. */
    public static final String CONFIG_GC_CITIZEN =
            "test-config/garbageCollection/gc_citizen_module.json";

    /** Path to Garbage Collection payment step JSON config. */
    public static final String CONFIG_GC_PAYMENT =
            "test-config/garbageCollection/gc_citizen_payment_module.json";

    /** Path to No Due Certificate citizen module JSON config. */
    public static final String CONFIG_NDC_CITIZEN =
            "test-config/noDueCertificate/ndc_citizen__module.json";

    /** Path to OBPAS citizen module JSON config. */
    public static final String CONFIG_OBPAS_CITIZEN =
            "test-config/obpas/obpas_citizen_module.json";

    /** Path to OBPAS Occupancy Certificate citizen module JSON config. */
    public static final String CONFIG_OBPAS_OC_CITIZEN =
            "test-config/obpas/obpas_oc_create.json";

    /** Path to Pet Registration citizen module JSON config. */
    public static final String CONFIG_PET_CITIZEN =
            "test-config/pet/pet_citizen_module.json";

    /** Path to Pet Registration CEMP citizen module JSON config. */
    public static final String CONFIG_PET_CEMP_CITIZEN =
            "test-config/pet/pet_cemp_create.json";

    /** Path to Property Tax citizen module JSON config. */
    public static final String CONFIG_PROPERTY_TAX_CITIZEN =
            "test-config/propertyTax/property_tax_citizen_module.json";

    /** Path to Public Grievance Redressal citizen module JSON config. */
    public static final String CONFIG_PGR_CITIZEN =
            "test-config/pgr/pgr_citizen_module.json";

    /** Path to Tree Pruning citizen module JSON config. */
    public static final String CONFIG_TREE_PRUNING_CITIZEN =
            "test-config/requestService/tree_pruning_citizen_module.json";

    /** Path to Water Tanker citizen module JSON config. */
    public static final String CONFIG_WATER_TANKER_CITIZEN =
            "test-config/requestService/water_tanker_citizen_module.json";

    /** Path to Mobile Toilet citizen module JSON config. */
    public static final String CONFIG_MOBILE_TOILET_CITIZEN =
            "test-config/requestService/mobile_toilet_citizen_module.json";

    /** Path to Street Vending citizen module JSON config. */
    public static final String CONFIG_STREET_VENDING_CITIZEN =
            "test-config/streetVending/street_vending_citizen_module.json";

    /** Path to Trade License citizen module JSON config. */
    public static final String CONFIG_TRADE_LICENSE_CITIZEN =
            "test-config/tradeLicense/trade_license_citizen_module.json";

    /** Path to Water and Sewerage citizen module JSON config. */
    public static final String CONFIG_WATER_SEWERAGE_CITIZEN =
            "test-config/waterAndSewerage/water_and_sewerage_citizen_module.json";


    // =========================================================================
    // Employee Module Test-Config JSON Paths
    // =========================================================================

    /** Path to Advertisement employee module JSON config. */
    public static final String CONFIG_ADV_EMPLOYEE =
            "test-config/advertisement/adv_employee_module.json";

    /** Path to Asset Management initiator role JSON config. */
    public static final String CONFIG_ASSET_INITIATOR =
            "test-config/asset/asset_initiator.json";

    /** Path to Asset Management verifier role JSON config. */
    public static final String CONFIG_ASSET_VERIFIER =
            "test-config/asset/asset_verifier.json";

    /** Path to Asset Management approver role JSON config. */
    public static final String CONFIG_ASSET_APPROVER =
            "test-config/asset/asset_approver.json";

    /** Path to Challan Generation employee module JSON config. */
    public static final String CONFIG_CHALLAN_GEN_EMPLOYEE =
            "test-config/challanGeneration/cg_employee_module.json";

    /** Path to Community Hall Booking employee module JSON config. */
    public static final String CONFIG_CHB_EMPLOYEE =
            "test-config/chb/chb_employee_module.json";

    /** Path to Construction and Demolition employee module JSON config. */
    public static final String CONFIG_CND_EMPLOYEE =
            "test-config/cnd/cnd_employee_module.json";

    /** Path to Desludging employee update action JSON config. */
    public static final String CONFIG_DESLUDGING_UPDATE =
            "test-config/desludging/desludging_employee_update.json";

    /** Path to Desludging employee complete action JSON config. */
    public static final String CONFIG_DESLUDGING_COMPLETE =
            "test-config/desludging/desludging_employee_complete.json";

    /** Path to Desludging assign PSSO action JSON config. */
    public static final String CONFIG_DESLUDGING_PSSO =
            "test-config/desludging/desludging_assign_psso.json";

    /** Path to Desludging FSTPO role action JSON config. */
    public static final String CONFIG_DESLUDGING_FSTPO =
            "test-config/desludging/desludging_fstpo.json";

    /** Path to E-Waste Management employee module JSON config. */
    public static final String CONFIG_EWASTE_EMPLOYEE =
            "test-config/ewaste/ewaste_employee_module.json";

    /** Path to Estate Management employee module JSON config. */
    public static final String CONFIG_ESTATE_EMPLOYEE =
            "test-config/estateManagement/estate_management_employee_module.json";

    /** Path to Garbage Collection employee module JSON config. */
    public static final String CONFIG_GC_EMPLOYEE =
            "test-config/garbageCollection/gc_employee_module.json";

    /** Path to No Due Certificate employee module JSON config. */
    public static final String CONFIG_NDC_EMPLOYEE =
            "test-config/noDueCertificate/ndc_employee_module.json";

    /** Path to OBPAS employee module JSON config. */
    public static final String CONFIG_OBPAS_EMPLOYEE =
            "test-config/obpas/obpas_employee_module.json";

    /** Path to OBPAS Occupancy Certificate employee module JSON config. */
    public static final String CONFIG_OBPAS_OC_EMPLOYEE =
            "test-config/obpas/obpas_oc_employee_module.json";

    /** Path to Pet Registration employee module JSON config. */
    public static final String CONFIG_PET_EMPLOYEE =
            "test-config/pet/pet_employee_module.json";

    /** Path to Property Tax employee module JSON config. */
    public static final String CONFIG_PROPERTY_TAX_EMPLOYEE =
            "test-config/propertyTax/property_tax_employee_module.json";

    /** Path to Public Grievance Redressal employee module JSON config. */
    public static final String CONFIG_PGR_EMPLOYEE =
            "test-config/pgr/pgr_employee_module.json";

    /** Path to Tree Pruning employee module JSON config. */
    public static final String CONFIG_TREE_PRUNING_EMPLOYEE =
            "test-config/requestService/tree_pruning_employee_module.json";

    /** Path to Tree Pruning verifier role JSON config. */
    public static final String CONFIG_TREE_PRUNING_VERIFIER =
            "test-config/requestService/tree_pruning_verifier_module.json";

    /** Path to Water Tanker employee module JSON config. */
    public static final String CONFIG_WATER_TANKER_EMPLOYEE =
            "test-config/requestService/water_tanker_employee_module.json";

    /** Path to Mobile Toilet employee module JSON config. */
    public static final String CONFIG_MOBILE_TOILET_EMPLOYEE =
            "test-config/requestService/mobile_toilet_employee_module.json";

    /** Path to Street Vending employee module JSON config. */
    public static final String CONFIG_STREET_VENDING_EMPLOYEE =
            "test-config/streetVending/street_vending_employee_module.json";

    /** Path to Trade License employee module JSON config. */
    public static final String CONFIG_TRADE_LICENSE_EMPLOYEE =
            "test-config/tradeLicense/trade_license_employee_module.json";

    /** Path to Water employee module JSON config. */
    public static final String CONFIG_WATER_EMPLOYEE =
            "test-config/waterAndSewerage/water_employee_module.json";

    /** Path to Sewerage employee module JSON config. */
    public static final String CONFIG_SEWERAGE_EMPLOYEE =
            "test-config/waterAndSewerage/sewerage_employee_module.json";


    // =========================================================================
    // Vendor Module Test-Config JSON Paths
    // =========================================================================

    /** Path to Construction and Demolition vendor module JSON config. */
    public static final String CONFIG_CND_VENDOR =
            "test-config/cnd/cnd_vendor_module.json";

    /** Path to Water Tanker vendor module JSON config. */
    public static final String CONFIG_WATER_TANKER_VENDOR =
            "test-config/requestService/water_tanker_vendor_module.json";

    /** Path to Mobile Toilet vendor module JSON config. */
    public static final String CONFIG_MOBILE_TOILET_VENDOR =
            "test-config/requestService/mobile_toilet_vendor_module.json";


    // =========================================================================
    // Module Names
    // =========================================================================

    /** Identifier for Pet Registration module. */
    public static final String MODULE_PET_REGISTRATION =
            "PET_REGISTRATION";

    /** Identifier for Public Grievance Redressal module. */
    public static final String MODULE_PUBLIC_GRIEVANCE_REDRESSAL =
            "PUBLIC_GRIEVANCE_REDRESSAL";

    /** Identifier for No Due Certificate module. */
    public static final String MODULE_NO_DUE_CERTIFICATE =
            "NO_DUE_CERTIFICATE";


    // =========================================================================
    // Excel Sheet Names
    // =========================================================================

    /** Sheet name in test data Excel for Pet Registration. */
    public static final String SHEET_PET =
            "PET_Test_Data";

    /** Sheet name in test data Excel for Public Grievance Redressal. */
    public static final String SHEET_PGR =
            "PGR_Test_Data";

    /** Sheet name in test data Excel for No Due Certificate. */
    public static final String SHEET_NDC =
            "NDC_Test_Data";


    // =========================================================================
    // Workflow Types & Roles
    // =========================================================================

    /** Workflow stakeholder type for Citizen. */
    public static final String WORKFLOW_TYPE_CITIZEN = "CITIZEN";

    /** Workflow stakeholder type for Employee. */
    public static final String WORKFLOW_TYPE_EMPLOYEE = "EMPLOYEE";

    /** Workflow stakeholder type for Vendor. */
    public static final String WORKFLOW_TYPE_VENDOR = "VENDOR";

    /** Workflow role type for Initiator. */
    public static final String WORKFLOW_ROLE_INITIATOR = "INITIATOR";


    // =========================================================================
    // Workflow Data Keys
    // =========================================================================

    /** Key in WorkflowDataStore for general application number. */
    public static final String APPLICATION_NO = "APPLICATION_NO";

    /** Key in WorkflowDataStore for water application number. */
    public static final String WATER_APPLICATION_NO = "WATER_APPLICATION_NO";

    /** Key in WorkflowDataStore for sewerage application number. */
    public static final String SEWERAGE_APPLICATION_NO =
            "SEWERAGE_APPLICATION_NO";


    // =========================================================================
    // Application URLs
    // =========================================================================

    /** Relative path for citizen portal login. */
    public static final String CITIZEN_LOGIN_PATH = "/citizen/login";

    /** Relative path for employee portal login. */
    public static final String EMPLOYEE_LOGIN_PATH = "/employee/login";

}