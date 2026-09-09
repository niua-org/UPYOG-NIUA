package org.upyog.Automation.Controller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.upyog.Automation.Service.ModuleTestService;
import org.upyog.Automation.Utils.AutomationConstants;
import org.upyog.Automation.Utils.ExcelDataReader;
import org.upyog.Automation.Utils.ExcelResultGenerator;
import org.upyog.Automation.model.ModuleExecutionResult;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller responsible for module test execution,
 * Excel test-data upload/download, and execution progress tracking.
 *
 * <p>Provides endpoints to trigger test execution across various UPYOG modules,
 * track execution progress in real-time, upload custom test datasets, and download
 * execution templates and result workbooks.</p>
 */
@RestController
@RequestMapping(AutomationConstants.API_MODULE_BASE)
public class ModuleTestController {

    private static final Logger logger = LoggerFactory.getLogger(ModuleTestController.class);

    @Autowired
    private ModuleTestService moduleTestService;

    /**
     * Stores the latest generated result file so that it can be
     * downloaded through the /download-result endpoint.
     */
    private File latestResultFile;

    /**
     * Executes the selected UPYOG module or comma-separated batch of modules.
     *
     * <p>If an Excel test-data file has been uploaded, the execution
     * results are also written back to a temporary result workbook for downloading.</p>
     *
     * @param request the module execution request containing module name and base URL
     * @return ResponseEntity containing list of ModuleExecutionResult objects
     */
    @PostMapping(AutomationConstants.ENDPOINT_RUN)
    public ResponseEntity<List<ModuleExecutionResult>> runModule(@RequestBody ModuleRequest request) {
        logger.info("Received request to run module: [{}], Base URL: [{}]", 
                request != null ? request.getModuleName() : null, 
                request != null ? request.getBaseUrl() : null);

        // Execute module test service
        List<ModuleExecutionResult> result = moduleTestService.runModule(request);
        logger.info("Module execution completed with [{}] test case result(s).", result != null ? result.size() : 0);

        try {
            // Check if user uploaded a custom test-data Excel file
            if (ExcelDataReader.hasUploadedExcelFile()) {
                logger.debug("Uploaded Excel file detected. Preparing result workbook generation.");
                File sourceExcel = ExcelDataReader.getUploadedExcelFile();

                // Extract module name to locate target sheet
                String moduleName = request != null ? request.getModuleName() : "";
                String firstModule = (moduleName != null && moduleName.contains(",")) 
                        ? moduleName.split(",")[0].trim() 
                        : (moduleName != null ? moduleName.trim() : "");

                String sheetName = getSheetName(firstModule);
                logger.debug("Generating result Excel for sheet: [{}] from source: [{}]", sheetName, sourceExcel.getName());

                // Create a temporary file to store the result workbook
                File outputFile = File.createTempFile(
                        AutomationConstants.TEST_RESULT_FILE_PREFIX,
                        AutomationConstants.XLSX_EXTENSION
                );

                // Preserve uploaded workbook structure and append execution results
                latestResultFile = ExcelResultGenerator.generateResultExcel(
                        sourceExcel,
                        sheetName,
                        result,
                        outputFile
                );
                logger.info("Result Excel generated successfully at: [{}]", latestResultFile.getAbsolutePath());
            }

        } catch (Exception e) {
            // Result generation failure should not fail the API response
            logger.error("Failed to generate test result Excel for module execution: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Returns the current progress of the ongoing test execution.
     *
     * <p>The frontend periodically polls this endpoint to update
     * execution progress indicators (progress bar, active test case, etc.).</p>
     *
     * @return ResponseEntity containing progress details map
     */
    @GetMapping(AutomationConstants.ENDPOINT_PROGRESS)
    public ResponseEntity<?> getExecutionProgress() {
        Map<String, Object> progress = new HashMap<>();

        int total = moduleTestService.getTotalTestCases();
        int completed = moduleTestService.getCompletedTestCases();
        String currentTestCase = moduleTestService.getCurrentTestCase();
        String currentModule = moduleTestService.getCurrentModule();
        boolean isRunning = moduleTestService.isExecutionRunning();

        progress.put(AutomationConstants.KEY_TOTAL_TEST_CASES, total);
        progress.put(AutomationConstants.KEY_COMPLETED_TEST_CASES, completed);
        progress.put(AutomationConstants.KEY_CURRENT_TEST_CASE, currentTestCase);
        progress.put(AutomationConstants.KEY_CURRENT_MODULE, currentModule);
        progress.put(AutomationConstants.KEY_EXECUTION_RUNNING, isRunning);

        logger.debug("Progress check: Total=[{}], Completed=[{}], Active=[{}], Running=[{}]",
                total, completed, currentTestCase, isRunning);

        return ResponseEntity.ok(progress);
    }

    /**
     * Downloads the bundled master Excel test-data template.
     *
     * @return ResponseEntity with the template file resource
     */
    @GetMapping(AutomationConstants.ENDPOINT_DOWNLOAD_TEMPLATE)
    public ResponseEntity<Resource> downloadTemplate() {
        logger.info("Request received to download test data template: [{}]", AutomationConstants.TEST_DATA_FILE);

        ClassPathResource resource = new ClassPathResource(AutomationConstants.TEST_DATA_FILE);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        AutomationConstants.TEST_DATA_CONTENT_DISPOSITION
                )
                .contentType(
                        MediaType.parseMediaType(AutomationConstants.EXCEL_CONTENT_TYPE)
                )
                .body(resource);
    }

    /**
     * Downloads the most recently generated test-result workbook.
     *
     * @return ResponseEntity containing the result Excel resource, or 404 Not Found if unavailable
     */
    @GetMapping(AutomationConstants.ENDPOINT_DOWNLOAD_RESULT)
    public ResponseEntity<Resource> downloadResult() {
        logger.info("Request received to download latest test results.");

        if (latestResultFile == null || !latestResultFile.exists()) {
            logger.warn("Result download requested, but no generated result file is available.");
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(latestResultFile);
        logger.info("Serving test result file: [{}]", latestResultFile.getName());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        AutomationConstants.TEST_RESULT_CONTENT_DISPOSITION
                )
                .contentType(
                        MediaType.parseMediaType(AutomationConstants.EXCEL_CONTENT_TYPE)
                )
                .body(resource);
    }

    /**
     * Maps an automation module name to its corresponding Excel sheet name.
     *
     * <p>Explicit mappings are maintained for modules whose automation
     * names differ from their Excel sheet names.</p>
     *
     * @param moduleName the name of the automation module
     * @return the corresponding sheet name
     */
    private String getSheetName(String moduleName) {
        if (moduleName == null) {
            return AutomationConstants.SHEET_SUFFIX;
        }

        if (AutomationConstants.MODULE_PET_REGISTRATION.equalsIgnoreCase(moduleName)) {
            return AutomationConstants.SHEET_PET;
        }

        if (AutomationConstants.MODULE_PUBLIC_GRIEVANCE_REDRESSAL.equalsIgnoreCase(moduleName)) {
            return AutomationConstants.SHEET_PGR;
        }

        if (AutomationConstants.MODULE_NO_DUE_CERTIFICATE.equalsIgnoreCase(moduleName)) {
            return AutomationConstants.SHEET_NDC;
        }

        // Use standard naming convention for modules without an explicit mapping
        return moduleName + AutomationConstants.SHEET_SUFFIX;
    }

    /**
     * Uploads an Excel test-data workbook for the current execution session.
     *
     * <p>The uploaded workbook is stored temporarily and supplied to
     * ExcelDataReader instead of the bundled default test-data workbook.</p>
     *
     * @param file the multipart Excel file uploaded by the user
     * @return ResponseEntity containing success message or bad request error
     */
    @PostMapping(AutomationConstants.ENDPOINT_UPLOAD_EXCEL)
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            // Validate presence of uploaded file
            if (file == null || file.isEmpty()) {
                logger.warn("Upload rejected: empty or null file provided.");
                return ResponseEntity.badRequest()
                        .body(AutomationConstants.MSG_EMPTY_FILE);
            }

            String fileName = file.getOriginalFilename();
            logger.info("Processing uploaded Excel file: [{}] (Size: {} bytes)", fileName, file.getSize());

            // Validate file extension
            if (fileName == null || !fileName.toLowerCase().endsWith(AutomationConstants.XLSX_EXTENSION)) {
                logger.warn("Upload rejected: invalid file format for [{}]", fileName);
                return ResponseEntity.badRequest()
                        .body(AutomationConstants.MSG_UNSUPPORTED_EXCEL_FORMAT);
            }

            // Create temporary file to store the workbook
            File tempFile = File.createTempFile(
                    AutomationConstants.TEST_DATA_FILE_PREFIX,
                    AutomationConstants.XLSX_EXTENSION
            );

            file.transferTo(tempFile);
            ExcelDataReader.setUploadedExcelFile(tempFile);

            logger.info("Excel test-data file stored successfully at: [{}]", tempFile.getAbsolutePath());
            return ResponseEntity.ok(AutomationConstants.MSG_EXCEL_UPLOAD_SUCCESS + fileName);

        } catch (Exception e) {
            logger.error("Failed to upload Excel test-data file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(AutomationConstants.MSG_EXCEL_UPLOAD_FAILURE + e.getMessage());
        }
    }

    /**
     * Request object used to receive module execution parameters from the frontend.
     */
    @Getter
    @Setter
    @ToString
    public static class ModuleRequest {

        /**
         * Name of the module to execute, or comma-separated list of module names.
         */
        private String moduleName;

        /**
         * Target application base URL for the automation tests.
         */
        private String baseUrl;

    }
}