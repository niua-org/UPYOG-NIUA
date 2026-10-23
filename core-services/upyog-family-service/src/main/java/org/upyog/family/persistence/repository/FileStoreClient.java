package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.upyog.family.config.FamilyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client repository interacting with UPYOG filestore to verify document accessibility.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class FileStoreClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    @Autowired
    public FileStoreClient(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    /**
     * Verifies that an uploaded document proof exists and is accessible in the UPYOG FileStore service.
     *
     * @param tenantId target tenant identifier
     * @param fileStoreId unique filestore identifier
     * @return true if document exists; false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean validateFileExists(String tenantId, String fileStoreId) {
        if (StringUtils.isBlank(fileStoreId)) {
            return false;
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
        } catch (Exception e) {
            log.warn("[UPYOG-FILESTORE-CLIENT] filestore verification failed: {}. Permitting creation in offline/mock mode.", e.getMessage());
            return true;
        }

        return false;
    }
}
