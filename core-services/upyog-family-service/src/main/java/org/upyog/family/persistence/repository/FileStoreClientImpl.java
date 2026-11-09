package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.config.FamilyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Default implementation of {@link FileStoreClient} interacting with UPYOG filestore.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class FileStoreClientImpl implements FileStoreClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    /**
     * Constructs a FileStoreClientImpl with RestTemplate and FamilyConfiguration dependencies.
     *
     * @param restTemplate Spring RestTemplate for remote HTTP requests to egov-filestore
     * @param configuration family service configuration for filestore URLs and paths
     */
    @Autowired
    public FileStoreClientImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * Validates whether a file exists in the UPYOG FileStore for the given tenant and fileStoreId.
     *
     * @param tenantId tenant identifier representing the city/state jurisdiction
     * @param fileStoreId unique identifier of the uploaded document in FileStore
     * @return true if file exists or filestore service is in offline/mock mode; false otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean validateFileExists(String tenantId, String fileStoreId) {
        if (StringUtils.isBlank(fileStoreId)) {
            return false;
        }

        if (configuration == null || StringUtils.isBlank(configuration.getFileStoreHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.filestore.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getFileStoreUrlPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "egov.filestore.url.path is not configured");
        }

        String stateTenant = tenantId != null && tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
        String url = configuration.getFileStoreHost() + configuration.getFileStoreUrlPath()
                + "?tenantId=" + stateTenant + "&fileStoreIds=" + fileStoreId;

        log.debug("[UPYOG-FILESTORE-CLIENT] Validating fileStoreId: {} via URL: {}", fileStoreId, url);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey(fileStoreId)) {
                String fileUrl = (String) response.get(fileStoreId);
                return StringUtils.isNotBlank(fileUrl);
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[UPYOG-FILESTORE-CLIENT] filestore verification failed: {}", e.getMessage(), e);
            throw new CustomException(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, "Failed to verify document in filestore: " + e.getMessage());
        }

        return false;
    }
}
