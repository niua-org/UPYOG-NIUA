package org.egov.nationaldashboardingest.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.nationaldashboardingest.service.BatchIngestionProcessor;
import org.egov.nationaldashboardingest.service.BulkFileProcessorService;
import org.egov.nationaldashboardingest.utils.ExcelStreamingBatchReader;
import org.egov.nationaldashboardingest.utils.S3FileDownloader;
import org.egov.nationaldashboardingest.web.models.BulkIngestInitDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Production implementation of {@link BulkFileProcessorService}.
 * <p>
 * Orchestrates the end-to-end processing pipeline for bulk ingestion files:
 * <ol>
 *   <li>Downloads remote AWS S3 object identified by {@link BulkIngestInitDetail#getFileName()}
 *       to a temporary local disk file via {@link S3FileDownloader#downloadFileFromS3}.</li>
 *   <li>Parses Excel sheets in streaming batches of 100 rows using {@link ExcelStreamingBatchReader#processExcelFileInBatches},
 *       preventing OutOfMemory errors on large multi-megabyte datasets.</li>
 *   <li>Passes each batch to {@link BatchIngestionProcessor#processBatchWithFallback} to execute
 *       per-row inbound API audit logging and Elasticsearch indexing.</li>
 *   <li>Guarantees deletion of the temporary file in a {@code finally} block via {@link Files#deleteIfExists}.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
public class BulkFileProcessorServiceImpl implements BulkFileProcessorService {

    @Autowired
    private S3FileDownloader s3FileDownloader;

    @Autowired
    private ExcelStreamingBatchReader excelStreamingBatchReader;

    @Autowired
    private BatchIngestionProcessor batchIngestionProcessor;

    /**
     * Downloads an S3 file object, streams its rows, ingests metrics per row, and cleans up local disk resources.
     * <p>
     * Validates input details, coordinates disk transfer, drives batch consumption through lambda callbacks,
     * and logs aggregate statistics upon completion.
     * </p>
     *
     * @param bulkIngestInitDetail request DTO specifying the S3 file path and target state code
     */
    @Override
    public void processBulkFile(BulkIngestInitDetail bulkIngestInitDetail) {
        if (bulkIngestInitDetail == null || bulkIngestInitDetail.getFileName() == null) {
            log.error("Invalid bulk ingest init detail provided for processing");
            return;
        }

        File tempFile = null;
        String tenantId = bulkIngestInitDetail.getStateCode();

        List<Map<String, Object>> failedDatesList = new ArrayList<>();
        int totalRowsProcessed = 0;

        try {
            // STEP 1: Download File from S3 to Disk
            tempFile = s3FileDownloader.downloadFileFromS3(bulkIngestInitDetail.getFileName());

            // STEP 2: Stream Read Rows & Ingest each row using row-level RequestInfo
            totalRowsProcessed = excelStreamingBatchReader.processExcelFileInBatches(tempFile, batchDataList -> {
                batchIngestionProcessor.processBatchWithFallback(batchDataList, null, failedDatesList);
            });

            log.info("Finished processing bulk file: {}. Total rows processed: {}, Failures: {}",
                    bulkIngestInitDetail.getFileName(), totalRowsProcessed, failedDatesList.size());

        } catch (Exception e) {
            log.error("Error processing bulk ingest init file: {}", bulkIngestInitDetail.getFileName(), e);
        } finally {
            // STEP 3: Cleanup Temporary File
            if (tempFile != null && tempFile.exists()) {
                try {
                    boolean deleted = Files.deleteIfExists(tempFile.toPath());
                    log.info("Temporary file deleted from /tmp: {}, status: {}", tempFile.getAbsolutePath(), deleted);
                } catch (Exception e) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }
}
