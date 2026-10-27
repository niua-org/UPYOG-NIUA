package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.service.document.UserDocumentService;
import org.egov.user.web.contract.document.UserDocumentRequest;
import org.egov.user.web.contract.document.UserDocumentResponse;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller exposing citizen document CRUD endpoints under {@code /user/document}.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport, serialization,
 *       and delegates business logic and egov-filestore integration to {@link UserDocumentService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies exclusively on injected service abstractions.</li>
 * </ul>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /user/document/_create} - Attach citizen documents</li>
 *   <li>{@code POST /user/document/_update} - Update document metadata / verification</li>
 *   <li>{@code POST /user/document/_search} - Search documents with resolved file download URLs</li>
 *   <li>{@code POST /user/document/_delete} - Soft-delete document</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "User Documents", description = "Citizen identity and address proof document management with egov-filestore integration")
@RestController
@RequestMapping("/document")
@Slf4j
public class UserDocumentController {

    private final UserDocumentService documentService;

    @Autowired
    public UserDocumentController(UserDocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Attaches one or more citizen documents.
     *
     * @param request document create request payload
     * @return response containing created documents
     */
    @Operation(
            summary = "Attach Citizen Documents",
            description = "Attaches one or more identity or address proof documents to a citizen record, linked via egov-filestore fileStoreIds."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Documents attached successfully",
                    content = @Content(schema = @Schema(implementation = UserDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing mandatory fields"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<UserDocumentResponse> createDocuments(@RequestBody @Valid UserDocumentRequest request) {
        log.info("[USER-DOC-CONTROLLER] Received document create request for uuid: {}",
                request != null ? request.getUuid() : null);

        List<UserDocument> created = documentService.createDocuments(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(HttpStatus.CREATED.toString()).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDocumentResponse(responseInfo, created));
    }

    /**
     * Updates document metadata or verification status.
     *
     * @param request document update request payload
     * @return response containing updated document
     */
    @Operation(
            summary = "Update Document",
            description = "Updates document metadata, document number, or verification status (VERIFIED/REJECTED/PENDING)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid document ID or request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<UserDocumentResponse> updateDocument(@RequestBody @Valid UserDocumentRequest request) {
        log.info("[USER-DOC-CONTROLLER] Received document update request for documentId: {}",
                request != null ? request.getDocumentId() : null);

        UserDocument updated = documentService.updateDocument(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new UserDocumentResponse(responseInfo, Collections.singletonList(updated)));
    }

    /**
     * Searches citizen documents and resolves egov-filestore URLs.
     *
     * @param criteria document search criteria payload
     * @return response containing list of matched documents with pre-signed download URLs
     */
    @Operation(
            summary = "Search Documents",
            description = "Searches citizen documents by user UUID, tenant ID, or document type, automatically resolving temporary download URLs from egov-filestore."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<UserDocumentResponse> searchDocuments(@RequestBody @Valid UserDocumentSearchCriteria criteria) {
        if (criteria != null && criteria.getCriteria() != null) {
            UserDocumentSearchCriteria inner = criteria.getCriteria();
            if (inner.getRequestInfo() == null) {
                inner.setRequestInfo(criteria.getRequestInfo());
            }
            criteria = inner;
        }

        log.info("[USER-DOC-CONTROLLER] Received document search request: uuid={}, docType={}",
                criteria != null ? criteria.getUuid() : null,
                criteria != null ? criteria.getDocumentType() : null);

        List<UserDocument> documents = documentService.searchDocuments(criteria, criteria != null ? criteria.getRequestInfo() : null);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new UserDocumentResponse(responseInfo, documents));
    }

    /**
     * Soft-deletes a citizen document.
     *
     * @param request document delete request payload
     * @return response containing soft-deleted document
     */
    @Operation(
            summary = "Delete Document",
            description = "Soft-deletes a citizen document record by setting active = false."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document deleted successfully",
                    content = @Content(schema = @Schema(implementation = UserDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid document ID or request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_delete")
    public ResponseEntity<UserDocumentResponse> deleteDocument(@RequestBody @Valid UserDocumentRequest request) {
        log.info("[USER-DOC-CONTROLLER] Received document delete request for documentId: {}",
                request != null ? request.getDocumentId() : null);

        UserDocument deleted = documentService.deleteDocument(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new UserDocumentResponse(responseInfo, Collections.singletonList(deleted)));
    }
}
