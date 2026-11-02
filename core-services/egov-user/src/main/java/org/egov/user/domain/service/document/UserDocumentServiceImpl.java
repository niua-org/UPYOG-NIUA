package org.egov.user.domain.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.document.UserDocumentRequest;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.egov.user.config.UserConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserDocumentService} handling citizen document attachments and egov-filestore resolution.
 */
@Service
@Slf4j
public class UserDocumentServiceImpl implements UserDocumentService {

    private final UserDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStoreRepository fileStoreRepository;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting repositories and configuration properties.
     *
     * @param documentRepository  repository for user document persistence
     * @param userRepository      repository for user lookups
     * @param fileStoreRepository repository for file store download URL resolution
     * @param userConfiguration   application configuration properties
     */
    @Autowired
    public UserDocumentServiceImpl(UserDocumentRepository documentRepository,
                                  UserRepository userRepository,
                                  FileStoreRepository fileStoreRepository,
                                  @Autowired(required = false) UserConfiguration userConfiguration) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileStoreRepository = fileStoreRepository;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Testing constructor without userConfiguration.
     *
     * @param documentRepository  repository for user document persistence
     * @param userRepository      repository for user lookups
     * @param fileStoreRepository repository for file store download URL resolution
     */
    public UserDocumentServiceImpl(UserDocumentRepository documentRepository,
                                  UserRepository userRepository,
                                  FileStoreRepository fileStoreRepository) {
        this(documentRepository, userRepository, fileStoreRepository, null);
    }

    @Override
    public List<UserDocument> createDocuments(UserDocumentRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getEffectiveDocuments())) {
            log.error("[USER-DOC-SERVICE] Create documents failed: request or documents list is empty");
            throw new CustomException(UserConstants.ERROR_INVALID_DOCUMENTS_REQUEST, UserConstants.MSG_DOCUMENTS_PAYLOAD_EMPTY);
        }

        Long resolvedUserId = resolveUserId(request);
        String userTenant = userRepository.getTenantIdByUserId(resolvedUserId);
        String defaultTenant = StringUtils.isNotBlank(userTenant)
                ? userTenant
                : (StringUtils.isNotBlank(request.getTenantId()) ? request.getTenantId() : "pb.amritsar");
        Long operatorId = resolveOperatorId(request.getRequestInfo(), resolvedUserId);

        log.info("[USER-DOC-SERVICE] Creating {} document(s) for userId: {}, tenant: {}",
                request.getEffectiveDocuments().size(), resolvedUserId, defaultTenant);

        String resolvedUuid = resolveUserUuid(request);
        if (StringUtils.isBlank(resolvedUuid)) {
            resolvedUuid = userRepository.getUuidByUserId(resolvedUserId);
        }

        List<UserDocument> createdList = new ArrayList<>();
        for (UserDocument doc : request.getEffectiveDocuments()) {
            doc.setUserId(resolvedUserId);
            doc.setUserUuid(resolvedUuid);
            if (StringUtils.isNotBlank(userTenant)) {
                doc.setTenantId(userTenant);
            } else if (StringUtils.isBlank(doc.getTenantId())) {
                doc.setTenantId(defaultTenant);
            }

            validateDocumentAttributes(doc);
            doc.setCreatedBy(operatorId);
            doc.setLastModifiedBy(operatorId);
            doc.setDocumentUid(maskSensitiveUid(doc.getDocumentType(), doc.getDocumentUid()));

            UserDocument saved = documentRepository.create(doc);
            saved.setUserUuid(resolvedUuid);
            createdList.add(saved);
        }

        log.info("[USER-DOC-SERVICE] Successfully persisted {} document(s) for userUuid: {}",
                createdList.size(), resolvedUuid);
        return createdList;
    }

    @Override
    public UserDocument updateDocument(UserDocumentRequest request) {
        if (request == null || request.getDocument() == null) {
            log.error("[USER-DOC-SERVICE] Update document failed: request or document body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_DOCUMENT_REQUEST, UserConstants.MSG_DOCUMENT_BODY_NULL);
        }

        UserDocument doc = request.getDocument();
        if (StringUtils.isBlank(doc.getId())) {
            log.error("[USER-DOC-SERVICE] Update document failed: document id is missing");
            throw new CustomException(UserConstants.ERROR_DOCUMENT_ID_REQUIRED, UserConstants.MSG_DOCUMENT_ID_REQUIRED);
        }

        UserDocumentSearchCriteria criteria = UserDocumentSearchCriteria.builder()
                .id(doc.getId())
                .build();
        List<UserDocument> existing = documentRepository.search(criteria);
        if (CollectionUtils.isEmpty(existing)) {
            log.error("[USER-DOC-SERVICE] Document not found with ID: {}", doc.getId());
            throw new CustomException(UserConstants.ERROR_DOCUMENT_NOT_FOUND, "Document not found with ID: " + doc.getId());
        }

        UserDocument existingDoc = existing.get(0);
        Long operatorId = resolveOperatorId(request.getRequestInfo(), existingDoc.getUserId());

        // SCD Type 2 Soft-versioning:
        // 1. Soft-deactivate the previous document version
        log.info("[USER-DOC-SERVICE] Soft-deactivating previous document ID: {} for userId: {}", existingDoc.getId(), existingDoc.getUserId());
        documentRepository.delete(existingDoc.getId(), operatorId);

        // 2. Insert new active document record with updated attributes
        if (StringUtils.isBlank(doc.getTenantId())) {
            doc.setTenantId(existingDoc.getTenantId());
        }
        validateDocumentAttributes(doc);
        doc.setId(UUID.randomUUID().toString());
        doc.setUserId(existingDoc.getUserId());
        String userUuid = resolveUserUuid(request);
        if (StringUtils.isBlank(userUuid)) {
            userUuid = existingDoc.getUserUuid();
        }
        if (StringUtils.isBlank(userUuid) && existingDoc.getUserId() != null) {
            userUuid = userRepository.getUuidByUserId(existingDoc.getUserId());
        }
        doc.setUserUuid(userUuid);
        doc.setActive(true);
        doc.setCreatedBy(operatorId);
        doc.setLastModifiedBy(operatorId);
        doc.setDocumentUid(maskSensitiveUid(doc.getDocumentType(), doc.getDocumentUid()));

        UserDocument saved = documentRepository.create(doc);
        saved.setUserUuid(userUuid);
        log.info("[USER-DOC-SERVICE] Successfully persisted new active document version ID: {} for userUuid: {}",
                saved.getId(), userUuid);

        return saved;
    }

    @Override
    public List<UserDocument> searchDocuments(UserDocumentSearchCriteria criteria, RequestInfo requestInfo) {
        if (criteria == null) {
            log.error("[USER-DOC-SERVICE] Search documents failed: criteria is null");
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, UserConstants.MSG_DOCUMENT_SEARCH_CRITERIA_NULL);
        }

        if (StringUtils.isNotBlank(criteria.getUserUuid()) && criteria.getUserId() == null) {
            Long userId = userRepository.getUserIdByUuid(criteria.getUserUuid());
            if (userId != null) {
                criteria.setUserId(userId);
            }
        }

        log.info("[USER-DOC-SERVICE] Searching documents: userId={}, userUuid={}, docType={}, tenantId={}",
                criteria.getUserId(), criteria.getUserUuid(), criteria.getDocumentType(), criteria.getTenantId());

        List<UserDocument> documents = documentRepository.search(criteria);
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        for (UserDocument d : documents) {
            if (StringUtils.isBlank(d.getUserUuid())) {
                if (StringUtils.isNotBlank(criteria.getUserUuid())) {
                    d.setUserUuid(criteria.getUserUuid());
                } else if (d.getUserId() != null) {
                    d.setUserUuid(userRepository.getUuidByUserId(d.getUserId()));
                }
            }
        }

        // Enrich with egov-filestore URLs
        enrichFileStoreUrls(documents, criteria.getTenantId());
        return documents;
    }

    @Override
    public UserDocument deleteDocument(UserDocumentRequest request) {
        if (request == null) {
            log.error("[USER-DOC-SERVICE] Delete document failed: request is null");
            throw new CustomException(UserConstants.ERROR_INVALID_DOCUMENT_REQUEST, UserConstants.MSG_DOCUMENT_DELETE_REQUEST_NULL);
        }

        String documentId = StringUtils.isNotBlank(request.getDocumentId())
                ? request.getDocumentId()
                : (request.getDocument() != null ? request.getDocument().getId() : null);

        if (StringUtils.isBlank(documentId)) {
            log.error("[USER-DOC-SERVICE] Delete document failed: documentId is missing");
            throw new CustomException(UserConstants.ERROR_DOCUMENT_ID_REQUIRED, UserConstants.MSG_DOCUMENT_DELETE_ID_REQUIRED);
        }

        UserDocumentSearchCriteria criteria = UserDocumentSearchCriteria.builder()
                .id(documentId)
                .build();
        List<UserDocument> existing = documentRepository.search(criteria);
        if (CollectionUtils.isEmpty(existing)) {
            log.error("[USER-DOC-SERVICE] Document not found with ID: {}", documentId);
            throw new CustomException(UserConstants.ERROR_DOCUMENT_NOT_FOUND, "Document not found with ID: " + documentId);
        }

        Long operatorId = resolveOperatorId(request.getRequestInfo(), existing.get(0).getUserId());
        log.info("[USER-DOC-SERVICE] Soft-deleting document ID: {}", documentId);
        documentRepository.delete(documentId, operatorId);

        UserDocument doc = existing.get(0);
        doc.setActive(false);
        if (StringUtils.isBlank(doc.getUserUuid()) && doc.getUserId() != null) {
            doc.setUserUuid(userRepository.getUuidByUserId(doc.getUserId()));
        }
        return doc;
    }

    /**
     * Validates that required document attributes (documentType, filestoreId) are present and valid.
     *
     * @param doc the user document to validate
     * @throws CustomException if required attributes are missing
     */
    private void validateDocumentAttributes(UserDocument doc) {
        if (StringUtils.isBlank(doc.getDocumentType())) {
            throw new CustomException(UserConstants.ERROR_DOCUMENT_TYPE_REQUIRED, UserConstants.MSG_DOCUMENT_TYPE_MANDATORY);
        }
        if (StringUtils.isBlank(doc.getFilestoreId())) {
            throw new CustomException(UserConstants.ERROR_FILESTORE_ID_REQUIRED, UserConstants.MSG_FILESTORE_ID_MANDATORY);
        }
        validateFileStoreId(doc.getTenantId(), doc.getFilestoreId());
    }

    /**
     * Verifies that the provided filestoreId exists in egov-filestore service for the given tenant.
     *
     * @param tenantId    tenant identifier
     * @param fileStoreId filestore identifier to verify
     * @throws CustomException if filestoreId is verified to not exist
     */
    private void validateFileStoreId(String tenantId, String fileStoreId) {
        if (StringUtils.isBlank(fileStoreId) || fileStoreRepository == null) {
            return;
        }
        if (userConfiguration != null && Boolean.FALSE.equals(userConfiguration.getFilestoreValidationEnabled())) {
            log.info("[USER-DOC-SERVICE] filestore.validation.enabled is false; skipping strict filestore validation for fileStoreId: {}", fileStoreId);
            return;
        }
        try {
            String tenant = resolveTenant(tenantId);
            Map<String, String> urlMap = fileStoreRepository.getUrlByFileStoreId(tenant, Collections.singletonList(fileStoreId));
            if (urlMap == null || !urlMap.containsKey(fileStoreId) || StringUtils.isBlank(urlMap.get(fileStoreId))) {
                log.error("[USER-DOC-SERVICE] Invalid filestoreId: {} not found in egov-filestore for tenant: {}", fileStoreId, tenant);
                throw new CustomException(UserConstants.ERROR_FILESTORE_NOT_FOUND, UserConstants.MSG_FILESTORE_NOT_FOUND + fileStoreId);
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("[USER-DOC-SERVICE] Could not verify filestoreId {} against egov-filestore ({}: {}). Skipping strict validation in unreachable environment.",
                    fileStoreId, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String resolveTenant(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId.trim();
        }
        if (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getStateLevelTenantId())) {
            return userConfiguration.getStateLevelTenantId().trim();
        }
        log.error("[USER-DOC-SERVICE] Tenant ID cannot be determined: state-level tenant ID is not configured");
        throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, "Tenant ID cannot be determined: state-level tenant ID is not configured");
    }

    /**
     * Calls egov-filestore to retrieve pre-signed temporary download URLs for all attached documents.
     *
     * @param documents        list of user documents to enrich
     * @param fallbackTenantId tenant ID to use if document lacks explicit tenant
     */
    private void enrichFileStoreUrls(List<UserDocument> documents, String fallbackTenantId) {
        try {
            String tenantId = StringUtils.isNotBlank(fallbackTenantId)
                    ? fallbackTenantId
                    : documents.get(0).getTenantId();

            List<String> fileStoreIds = documents.stream()
                    .map(UserDocument::getFilestoreId)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(fileStoreIds)) {
                Map<String, String> urlMap = fileStoreRepository.getUrlByFileStoreId(tenantId, fileStoreIds);
                if (urlMap != null) {
                    documents.forEach(doc -> {
                        if (doc.getFilestoreId() != null && urlMap.containsKey(doc.getFilestoreId())) {
                            doc.setFileStoreUrl(urlMap.get(doc.getFilestoreId()));
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("[USER-DOC-SERVICE] Could not resolve filestore URLs from egov-filestore: {}", e.getMessage());
        }
    }

    /**
     * Resolves the numeric internal user ID from the document request using UUID or direct userId.
     *
     * @param request the document request
     * @return numeric user ID
     * @throws CustomException if user cannot be found
     */
    private Long resolveUserId(UserDocumentRequest request) {
        String userUuid = resolveUserUuid(request);
        if (StringUtils.isNotBlank(userUuid)) {
            Long resolved = userRepository.getUserIdByUuid(userUuid);
            if (resolved != null) {
                return resolved;
            }
            log.error("[USER-DOC-SERVICE] User not found with uuid: {}", userUuid);
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, "No user found with uuid: " + userUuid);
        }
        if (request.getUserId() != null) {
            return request.getUserId();
        }
        if (!CollectionUtils.isEmpty(request.getEffectiveDocuments()) && request.getEffectiveDocuments().get(0).getUserId() != null) {
            return request.getEffectiveDocuments().get(0).getUserId();
        }
        throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_DOCUMENT_USER_UUID_MANDATORY);
    }

    /**
     * Extracts citizen UUID from the request envelope or nested documents.
     *
     * @param request the document request envelope
     * @return citizen UUID string or null
     */
    private String resolveUserUuid(UserDocumentRequest request) {
        if (request == null) return null;
        if (StringUtils.isNotBlank(request.getUuid())) {
            return request.getUuid();
        }
        if (StringUtils.isNotBlank(request.getUserUuid())) {
            return request.getUserUuid();
        }
        if (!CollectionUtils.isEmpty(request.getEffectiveDocuments())) {
            for (UserDocument doc : request.getEffectiveDocuments()) {
                if (StringUtils.isNotBlank(doc.getUuid())) {
                    return doc.getUuid();
                }
                if (StringUtils.isNotBlank(doc.getUserUuid())) {
                    return doc.getUserUuid();
                }
            }
        }
        return null;
    }

    /**
     * Resolves the operator / creator ID from the RequestInfo or defaults to fallback ID.
     *
     * @param requestInfo request metadata containing operator context
     * @param fallback    fallback ID if operator context is absent
     * @return operator user ID
     */
    private Long resolveOperatorId(RequestInfo requestInfo, Long fallback) {
        return (requestInfo != null && requestInfo.getUserInfo() != null && requestInfo.getUserInfo().getId() != null)
                ? requestInfo.getUserInfo().getId()
                : fallback;
    }

    /**
     * Masks sensitive document identifiers (such as Aadhaar numbers) showing only the last 4 digits.
     *
     * @param docType type of document (e.g. AADHAAR)
     * @param uid     raw document identifier
     * @return masked document identifier
     */
    private String maskSensitiveUid(String docType, String uid) {
        if (StringUtils.isBlank(uid)) return null;
        if ("AADHAAR".equalsIgnoreCase(docType)) {
            String clean = uid.replaceAll("\\s+", "");
            return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
        }
        return uid;
    }
}
