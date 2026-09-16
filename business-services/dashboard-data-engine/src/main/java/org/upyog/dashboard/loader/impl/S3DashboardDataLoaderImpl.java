package org.upyog.dashboard.loader.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.upyog.dashboard.api.DashboardIngestionClient;
import org.upyog.dashboard.common.constants.DashboardConstants;
import org.upyog.dashboard.config.DashboardProperties;
import org.upyog.dashboard.loader.DashboardDataLoader;
import org.upyog.dashboard.model.DashboardData;
import org.upyog.dashboard.model.DashboardPayload;
import org.upyog.dashboard.model.IngestionResult;
import org.upyog.dashboard.service.IngestionRecordPersistenceService;
import org.upyog.dashboard.service.SXSSFExcelGeneratorService;
import org.upyog.dashboard.util.CommonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation of {@link DashboardDataLoader} that aggregates normalized
 * dashboard payload records into an Excel spreadsheet, uploads the file to AWS S3 storage,
 * and persists the ingestion audit records via {@link IngestionRecordPersistenceService}.
 * <p>
 * Key operations:
 * <ul>
 *   <li>Converts raw {@link DashboardPayload} domain records into Apache POI SXSSF streaming workbook format.</li>
 *   <li>Derives state/tenant codes dynamically from ULB identifiers or fallback configurations.</li>
 *   <li>Invokes {@link DashboardIngestionClient#uploadToS3} to store the file and trigger downstream bulk init.</li>
 *   <li>Persists execution logs (payload JSON, S3 key / error stack, status) via {@link IngestionRecordPersistenceService}.</li>
 *   <li>Guarantees deletion of local temporary spreadsheet files in a {@code finally} block.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component("s3DataLoader")
@RequiredArgsConstructor
public class S3DashboardDataLoaderImpl implements DashboardDataLoader {

    private final DashboardIngestionClient ingestionClient;
    private final SXSSFExcelGeneratorService excelGeneratorService;
    private final DashboardProperties properties;
    private final IngestionRecordPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    /**
     * Executes the loading pipeline for a dashboard payload by exporting data to Excel and uploading to S3.
     * <p>
     * Steps:
     * <ol>
     *   <li>Extracts module name and ingestion date from the first record of {@code payload.getData()}.</li>
     *   <li>Streams data rows into a temporary {@code .xlsx} file via {@link SXSSFExcelGeneratorService#generateExcelFile}.</li>
     *   <li>Resolves the tenant ID from the payload ULB or falls back to system configuration.</li>
     *   <li>Serializes the payload to JSON for audit trail tracking.</li>
     *   <li>Dispatches the file to S3 via {@link DashboardIngestionClient#uploadToS3}.</li>
     *   <li>Persists the audit record and returns the finalized {@link IngestionResult}.</li>
     * </ol>
     * </p>
     *
     * @param payload the collected and normalized metric records to load
     * @return {@link IngestionResult} detailing the outcome (SUCCESS or FAILURE), S3 keys, and error diagnostics
     */
    @Override
    public IngestionResult load(DashboardPayload payload) {
        String moduleName = "DASHBOARD";
        if (payload.getData() != null && !payload.getData().isEmpty() && payload.getData().get(0).getModule() != null) {
            moduleName = payload.getData().get(0).getModule();
        }

        int payloadDataSize = (payload.getData() != null) ? payload.getData().size() : 0;
        DashboardData firstElement = (payloadDataSize > 0) ? payload.getData().get(0) : null;
        String ingestionDateString = firstElement != null ? firstElement.getDate() : null;

        log.info("S3DashboardDataLoaderImpl | Executing S3 upload routing for module: {}", moduleName);
        File tempFile = null;
        String requestJson = null;
        try {
            List<Object> records = new ArrayList<>();
            if (payload.getData() != null) {
                for (DashboardData data : payload.getData()) {
                    records.add(data);
                }
            }

            tempFile = excelGeneratorService.generateExcelFile(moduleName, records, DashboardConstants.DAILY);

            String tenantId = properties.getTenantId();
            if (payload.getData() != null && !payload.getData().isEmpty()) {
                String payloadUlb = payload.getData().get(0).getUlb();
                if (payloadUlb != null && payloadUlb.contains(".")) {
                    tenantId = payloadUlb.split("\\.")[0];
                } else if (payloadUlb != null) {
                    tenantId = payloadUlb;
                }
            }

            try {
                requestJson = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                requestJson = "{\"moduleName\":\"" + moduleName + "\",\"tenantId\":\"" + tenantId + "\"}";
            }

            IngestionResult clientResult = ingestionClient.uploadToS3(tempFile, moduleName, tenantId);

            IngestionResult finalResult = IngestionResult.builder()
                    .ingestionStatus(clientResult.getIngestionStatus())
                    .responseData(clientResult.getResponseData())
                    .failureReason(clientResult.getFailureReason())
                    .date(ingestionDateString)
                    .moduleName(moduleName)
                    .ingestedAt(CommonUtils.getCurrentEpochMillis())
                    .build();

            pushIngestionRecord(payload, requestJson,
                    clientResult.getResponseData() != null ? clientResult.getResponseData() : clientResult.getFailureReason(),
                    clientResult.getIngestionStatus());

            return finalResult;
        } catch (Exception exception) {
            log.error("S3DashboardDataLoaderImpl | Failed to generate and upload Excel file for module {}", moduleName, exception);
            String failureReason = "Exception during S3 routing: " + exception.getMessage();
            IngestionResult failureResult = IngestionResult.builder()
                    .ingestionStatus(DashboardConstants.STATUS_FAILURE)
                    .failureReason(failureReason)
                    .date(ingestionDateString)
                    .moduleName(moduleName)
                    .ingestedAt(CommonUtils.getCurrentEpochMillis())
                    .build();

            pushIngestionRecord(payload, requestJson, failureReason, DashboardConstants.STATUS_FAILURE);
            return failureResult;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Safely pushes ingestion run details to the configured persistence service (JDBC or Kafka).
     * <p>
     * Catches and logs any persistence exceptions to prevent logging failures from aborting the primary workflow.
     * </p>
     *
     * @param payload        the original dashboard metrics payload
     * @param requestJson    serialized JSON string of the request payload
     * @param responseOrError response JSON string or failure error message
     * @param status         final ingestion status string (SUCCESS or FAILURE)
     */
    private void pushIngestionRecord(DashboardPayload payload, String requestJson, String responseOrError, String status) {
        try {
            persistenceService.pushIngestionRecord(payload, requestJson, responseOrError, status);
        } catch (Exception exception) {
            log.error("S3DashboardDataLoaderImpl | Failed to push ingestion record to persistence service", exception);
        }
    }
}
