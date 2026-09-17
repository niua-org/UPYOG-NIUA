package org.upyog.dashboard.service.impl;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.upyog.dashboard.client.DashboardFeignClient;
import org.upyog.dashboard.config.DashboardProperties;
import org.upyog.dashboard.common.constants.DashboardConstants;
import org.upyog.dashboard.model.BulkIngestDetails;
import org.upyog.dashboard.model.BulkIngestRequest;
import org.upyog.dashboard.model.RequestInfo;
import org.upyog.dashboard.model.UserInfo;
import org.upyog.dashboard.service.BulkIngestionInitService;
import org.upyog.dashboard.service.OAuthTokenService;
import org.upyog.dashboard.util.CommonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implementation of {@link BulkIngestionInitService}.
 * <p>
 * Orchestrates the downstream notification workflow after files are staged in AWS S3:
 * <ol>
 *   <li>Retrieves active OAuth authentication credentials and system user metadata from {@link OAuthTokenService}.</li>
 *   <li>Assembles standard UPYOG {@link RequestInfo} with API ID, auth token, and epoch message ID.</li>
 *   <li>Constructs {@link BulkIngestDetails} linking the S3 file key with the state tenant code.</li>
 *   <li>Wraps both in a {@link BulkIngestRequest} envelope and serializes the JSON body via {@link ObjectMapper}.</li>
 *   <li>Dispatches the HTTP POST request to the national dashboard endpoint using {@link DashboardFeignClient#ingestMetrics}.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkIngestionInitServiceImpl implements BulkIngestionInitService {

    private final DashboardFeignClient dashboardFeignClient;
    private final OAuthTokenService oAuthTokenService;
    private final DashboardProperties dashboardProperties;
    private final ObjectMapper objectMapper;

    /**
     * Dispatches the bulk ingestion initialization request to the downstream national dashboard ingest engine.
     * <p>
     * Fetches current OAuth credentials, builds the UPYOG request body containing the S3 key,
     * serializes the request to JSON, and sends an HTTP POST request to {@code dashboardProperties.getBulkInitUrl()}.
     * Handles exceptions gracefully and logs diagnostics.
     * </p>
     *
     * @param fileStoreId complete S3 key/path of the uploaded dataset file
     * @return downstream API response body as JSON string, or {@code null} if an exception occurs
     */
    @Override
    public String initializeBulkIngestion(String fileStoreId) {
        String bulkInitUrl = dashboardProperties.getBulkInitUrl();
        String stateCode = dashboardProperties.getTenantId();
        log.info("BulkIngestionInitServiceImpl | Initializing bulk ingestion at: {} for fileName: {} and stateCode: {}",
                bulkInitUrl, fileStoreId, stateCode);

        try {
            String oauthToken = oAuthTokenService != null ? oAuthTokenService.getToken() : null;
            UserInfo userInfo = oAuthTokenService != null ? oAuthTokenService.getUserInfo() : null;

            RequestInfo requestInfo = RequestInfo.builder()
                    .apiId(DashboardConstants.API_ID_RAINMAKER)
                    .authToken(oauthToken)
                    .userInfo(userInfo)
                    .msgId(CommonUtils.getCurrentEpochMillis() + DashboardConstants.LOCALE_EN_IN_SUFFIX)
                    .build();

            BulkIngestDetails details = BulkIngestDetails.builder()
                    .fileName(fileStoreId)
                    .stateCode(stateCode)
                    .build();

            BulkIngestRequest requestPayload = BulkIngestRequest.builder()
                    .requestInfo(requestInfo)
                    .details(details)
                    .build();

            String payloadJson = objectMapper.writeValueAsString(requestPayload);
            log.info("BulkIngestionInitServiceImpl | Bulk ingest init request payload: {}", payloadJson);

            String responseJson = dashboardFeignClient.ingestMetrics(URI.create(bulkInitUrl), payloadJson);
            log.info("BulkIngestionInitServiceImpl | Bulk ingest init response: {}", responseJson);
            return responseJson;
        } catch (Exception exception) {
            log.error("BulkIngestionInitServiceImpl | Failed to call bulk ingest init API at {}", bulkInitUrl, exception);
            return null;
        }
    }
}
