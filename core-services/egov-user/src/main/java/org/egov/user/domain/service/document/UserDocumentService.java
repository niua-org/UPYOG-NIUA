package org.egov.user.domain.service.document;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.web.contract.document.UserDocumentRequest;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;

import java.util.List;

/**
 * Service contract for citizen document attachments integrated with egov-filestore.
 *
 * Adheres to Dependency Inversion Principle (DIP).
 */
public interface UserDocumentService {

    /**
     * Registers and attaches one or more citizen documents.
     *
     * @param request the document creation request envelope containing document items
     * @return list of created user document records
     */
    List<UserDocument> createDocuments(UserDocumentRequest request);

    /**
     * Updates document metadata or verification status.
     *
     * @param request the document update request containing document details and target ID
     * @return updated user document entity
     */
    UserDocument updateDocument(UserDocumentRequest request);

    /**
     * Searches citizen documents and enriches file download URLs via egov-filestore.
     *
     * @param criteria    document search criteria (uuid, documentType, verified status)
     * @param requestInfo standard UPYOG request metadata
     * @return list of matching citizen documents with enriched signed download URLs
     */
    List<UserDocument> searchDocuments(UserDocumentSearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Soft-deletes a citizen document.
     *
     * @param request document request containing the document ID to soft-delete
     * @return soft-deleted document entity marked inactive
     */
    UserDocument deleteDocument(UserDocumentRequest request);
}
