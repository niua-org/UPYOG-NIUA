package org.upyog.family.persistence.repository;

/**
 * Client interface for interacting with UPYOG filestore to verify document accessibility.
 * Follows Dependency Inversion Principle (DIP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FileStoreClient {

    /**
     * Verifies that an uploaded document proof exists and is accessible in the UPYOG FileStore service.
     *
     * @param tenantId target tenant identifier
     * @param fileStoreId unique filestore identifier
     * @return true if document exists; false otherwise
     */
    boolean validateFileExists(String tenantId, String fileStoreId);
}
