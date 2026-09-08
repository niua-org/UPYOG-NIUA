package org.upyog.Automation.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.upyog.Automation.Service.ModuleTestService;
import org.upyog.Automation.Utils.ExcelDataReader;
import org.upyog.Automation.model.ModuleExecutionResult;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.core.io.FileSystemResource;
import java.io.File;
import org.springframework.web.multipart.MultipartFile;
import org.upyog.Automation.Utils.ExcelResultGenerator;

import java.util.Map;
import java.util.HashMap;


import java.util.List;

@RestController
@RequestMapping("/api/module")
public class ModuleTestController {

    @Autowired
    private ModuleTestService moduleTestService;

    private File latestResultFile;

    @PostMapping("/run")
    public ResponseEntity<List<ModuleExecutionResult>> runModule(
            @RequestBody ModuleRequest request) {

        List<ModuleExecutionResult> result =
                moduleTestService.runModule(request);

        try {

            /*
             * Generate result Excel only when
             * an uploaded Excel file is available.
             */
            if (ExcelDataReader.hasUploadedExcelFile()) {

                File sourceExcel =
                        ExcelDataReader.getUploadedExcelFile();

                String moduleName =
                        request.getModuleName();

                /*
                 * Currently handling the first selected module.
                 * We will extend this to multiple sheets next.
                 */
                String firstModule =
                        moduleName.split(",")[0].trim();

                String sheetName =
                        getSheetName(firstModule);

                File outputFile =
                        File.createTempFile(
                                "upyog-test-result-",
                                ".xlsx"
                        );

                latestResultFile =
                        ExcelResultGenerator.generateResultExcel(
                                sourceExcel,
                                sheetName,
                                result,
                                outputFile
                        );

            }

        } catch (Exception e) {

            e.printStackTrace();

            /*
             * Test execution result should still be returned
             * even if result Excel generation fails.
             */
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getExecutionProgress() {

        Map<String, Object> progress = new HashMap<>();

        progress.put(
                "totalTestCases",
                moduleTestService.getTotalTestCases()
        );

        progress.put(
                "completedTestCases",
                moduleTestService.getCompletedTestCases()
        );

        progress.put(
                "currentTestCase",
                moduleTestService.getCurrentTestCase()
        );

        progress.put(
                "currentModule",
                moduleTestService.getCurrentModule()
        );

        progress.put(
                "executionRunning",
                moduleTestService.isExecutionRunning()
        );

        return ResponseEntity.ok(progress);
    }

    @GetMapping("/download-template")
    public ResponseEntity<Resource> downloadTemplate() {

        ClassPathResource resource =
                new ClassPathResource("test-data/test-data.xlsx");

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"UPYOG_Test_Data.xlsx\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(resource);
    }

    @GetMapping("/download-result")
    public ResponseEntity<Resource> downloadResult() {

        if (latestResultFile == null || !latestResultFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource =
                new FileSystemResource(latestResultFile);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"UPYOG_Test_Result.xlsx\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(resource);
    }

    private String getSheetName(String moduleName) {

        if ("PET_REGISTRATION".equalsIgnoreCase(moduleName)) {
            return "PET_Test_Data";
        }

        if ("PUBLIC_GRIEVANCE_REDRESSAL".equalsIgnoreCase(moduleName)) {
            return "PGR_Test_Data";
        }

        if ("NO_DUE_CERTIFICATE".equalsIgnoreCase(moduleName)) {
            return "NDC_Test_Data";
        }

        return moduleName + "_Test_Data";
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<String> uploadExcel(
            @RequestParam("file") MultipartFile file) {

        try {

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Please upload an Excel file.");
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null ||
                    !fileName.toLowerCase().endsWith(".xlsx")) {

                return ResponseEntity.badRequest()
                        .body("Only .xlsx Excel files are supported.");
            }

            File tempFile =
                    File.createTempFile(
                            "upyog-test-data-",
                            ".xlsx"
                    );

            file.transferTo(tempFile);

            ExcelDataReader.setUploadedExcelFile(tempFile);

            return ResponseEntity.ok(
                    "Excel uploaded successfully: "
                            + fileName
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(
                            "Failed to upload Excel: "
                                    + e.getMessage()
                    );
        }
    }

    public static class ModuleRequest {

        private String moduleName;

        private String baseUrl;

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}