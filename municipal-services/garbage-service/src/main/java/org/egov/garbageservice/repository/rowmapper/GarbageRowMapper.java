package org.egov.garbageservice.repository.rowmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;
import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * RowMapper extracting consolidated relational entities into GarbageApplication objects.
 */
@Component
@Slf4j
public class GarbageRowMapper implements ResultSetExtractor<List<GarbageApplication>> {

    private final ObjectMapper objectMapper;

    public GarbageRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GarbageApplication> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<Long, GarbageApplication> applicationMap = new LinkedHashMap<>();
        Map<Long, Set<String>> documentIdMap = new HashMap<>();
        Map<Long, Set<String>> connectionLogIdMap = new HashMap<>();

        while (rs.next()) {
            Long appId = rs.getLong("app_id");
            if (rs.wasNull() || appId == null) {
                continue;
            }

            GarbageApplication currentApplication = applicationMap.get(appId);

            if (currentApplication == null) {
                Long approvalDate = rs.getLong("app_approval_date");
                if (rs.wasNull()) approvalDate = null;

                Date dueDate = rs.getDate("app_due_date");

                AuditDetails auditDetails = AuditDetails.builder()
                        .createdBy(rs.getString("app_created_by"))
                        .createdTime(rs.getLong("app_created_time"))
                        .lastModifiedBy(rs.getString("app_last_modified_by"))
                        .lastModifiedTime(rs.getLong("app_last_modified_time"))
                        .build();

                JsonNode additionalDetail = null;
                try {
                    PGobject pgObj = (PGobject) rs.getObject("app_additional_detail");
                    if (pgObj != null && StringUtils.isNotBlank(pgObj.getValue())) {
                        additionalDetail = objectMapper.readTree(pgObj.getValue());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse additionalDetail JSON: {}", e.getMessage());
                }

                currentApplication = GarbageApplication.builder()
                        .id(appId)
                        .uuid(rs.getString("app_uuid"))
                        .ownerUuid(rs.getString("app_owner_uuid"))
                        .tenantUuid(rs.getString("app_tenant_uuid"))
                        .applicationNo(rs.getString("app_application_no"))
                        .parentApplicationNo(rs.getString("app_parent_application_no"))
                        .oldGarbageNo(rs.getString("app_old_garbage_no"))
                        .garbageNo(rs.getString("app_garbage_no"))
                        .propertyId(rs.getString("app_property_id"))
                        .status(rs.getString("app_status"))
                        .connectionStatus(rs.getString("app_connection_status"))
                        .arrears(rs.getBigDecimal("app_arrears"))
                        .tenantId(rs.getString("app_tenant"))
                        .businessService(rs.getString("app_business_service"))
                        .channel(rs.getString("app_channel"))
                        .dueDate(dueDate != null ? dueDate.toLocalDate() : null)
                        .paymentStatus(rs.getString("app_payment_status"))
                        .paymentAmount(rs.getBigDecimal("app_payment_amount"))
                        .approvalDate(approvalDate)
                        .additionalDetail(additionalDetail)
                        .auditDetails(auditDetails)
                        .documents(new ArrayList<>())
                        .connectionLogs(new ArrayList<>())
                        .applicantDetails(new ArrayList<>())
                        .build();

                applicationMap.put(appId, currentApplication);
                documentIdMap.put(appId, new HashSet<>());
                connectionLogIdMap.put(appId, new HashSet<>());
            }

            // Extract Specification
            String specId = rs.getString("spec_id");
            if (StringUtils.isNotBlank(specId) && currentApplication.getGarbageSpecification() == null) {
                int noOfUnits = rs.getInt("spec_no_of_units");
                if (rs.wasNull()) noOfUnits = 1;

                GarbageSpecification spec = GarbageSpecification.builder()
                        .id(specId)
                        .garbageId(appId)
                        .oldGarbageNo(rs.getString("spec_old_garbage_no"))
                        .collectionType(rs.getString("spec_collection_type"))
                        .propertyType(rs.getString("spec_property_type"))
                        .subCategory(rs.getString("spec_sub_category"))
                        .subCategoryType(rs.getString("spec_sub_category_type"))
                        .category(rs.getString("spec_category"))
                        .no_of_units(noOfUnits)
                        .specialCategory(rs.getString("spec_special_category"))
                        .floorNo(StringUtils.isNotBlank(rs.getString("spec_floor_no")) ? rs.getString("spec_floor_no") : rs.getString("app_floor_no"))
                        .isInheritance(rs.getBoolean("spec_is_inheritance"))
                        .build();

                currentApplication.setGarbageSpecification(spec);
            } else if (currentApplication.getGarbageSpecification() == null && StringUtils.isNotBlank(rs.getString("app_floor_no"))) {
                currentApplication.setFloorNo(rs.getString("app_floor_no"));
            }

            // Extract Document
            String docId = rs.getString("doc_document_detail_id");
            if (StringUtils.isNotBlank(docId) && !documentIdMap.get(appId).contains(docId)) {
                GarbageDocument doc = GarbageDocument.builder()
                        .documentDetailId(docId)
                        .garbageId(appId)
                        .documentType(rs.getString("doc_document_type"))
                        .fileStoreId(rs.getString("doc_file_store_id"))
                        .build();

                currentApplication.getDocuments().add(doc);
                documentIdMap.get(appId).add(docId);
            }

            // Extract Connection Log
            String clogId = rs.getString("clog_id");
            if (StringUtils.isNotBlank(clogId) && !connectionLogIdMap.get(appId).contains(clogId)) {
                Date effFrom = rs.getDate("clog_effective_from");
                Date effTo = rs.getDate("clog_effective_to");

                AuditDetails clogAudit = AuditDetails.builder()
                        .createdBy(rs.getString("clog_created_by"))
                        .createdTime(rs.getLong("clog_created_time"))
                        .build();

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(clogId)
                        .garbageId(appId)
                        .applicationNo(rs.getString("clog_application_no"))
                        .requestNo(rs.getString("clog_request_no"))
                        .status(rs.getString("clog_status"))
                        .reason(rs.getString("clog_reason"))
                        .requestType(rs.getString("clog_request_type"))
                        .scope(rs.getString("clog_scope"))
                        .propertyId(rs.getString("clog_property_id"))
                        .workflowStatus(rs.getString("clog_workflow_status"))
                        .requestedByUuid(rs.getString("clog_requested_by_uuid"))
                        .approvedByUuid(rs.getString("clog_approved_by_uuid"))
                        .approvedTime(rs.getLong("clog_approved_time"))
                        .rejectionReason(rs.getString("clog_rejection_reason"))
                        .reasonDetails(rs.getString("clog_reason_details"))
                        .effectiveFrom(effFrom != null ? effFrom.toLocalDate() : null)
                        .effectiveTo(effTo != null ? effTo.toLocalDate() : null)
                        .feeCharged(rs.getBigDecimal("clog_fee_charged"))
                        .auditDetails(clogAudit)
                        .build();

                currentApplication.getConnectionLogs().add(connLog);
                connectionLogIdMap.get(appId).add(clogId);
            }
        }

        return new ArrayList<>(applicationMap.values());
    }
}
