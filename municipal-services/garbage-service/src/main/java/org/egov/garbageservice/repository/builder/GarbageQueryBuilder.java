package org.egov.garbageservice.repository.builder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.garbageservice.web.models.garbage.GarbageSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Query builder targeting the consolidated relational database schema:
 * ug_gc_application, ug_gc_specification, ug_gc_document, and ug_gc_connection_log.
 */
@Component
@Slf4j
public class GarbageQueryBuilder {

    public static final String SELECT_NEXT_SEQUENCE = "SELECT nextval('seq_ug_gc_application_id')";

    private static final String BASE_QUERY = "SELECT "
            + "app.id as app_id, app.uuid as app_uuid, app.owner_uuid as app_owner_uuid, app.tenant_uuid as app_tenant_uuid, "
            + "app.application_no as app_application_no, app.parent_application_no as app_parent_application_no, "
            + "app.old_garbage_no as app_old_garbage_no, app.garbage_no as app_garbage_no, "
            + "app.property_id as app_property_id, app.floor_no as app_floor_no, app.status as app_status, "
            + "app.connection_status as app_connection_status, app.arrears as app_arrears, app.tenant as app_tenant, "
            + "app.business_service as app_business_service, app.channel as app_channel, app.due_date as app_due_date, "
            + "app.payment_status as app_payment_status, app.payment_amount as app_payment_amount, app.approval_date as app_approval_date, "
            + "app.additional_detail as app_additional_detail, app.created_by as app_created_by, app.created_time as app_created_time, "
            + "app.last_modified_by as app_last_modified_by, app.last_modified_time as app_last_modified_time, "
            + "spec.id as spec_id, spec.garbage_id as spec_garbage_id, spec.old_garbage_no as spec_old_garbage_no, "
            + "spec.collection_type as spec_collection_type, spec.property_type as spec_property_type, "
            + "spec.sub_category as spec_sub_category, spec.sub_category_type as spec_sub_category_type, "
            + "spec.category as spec_category, spec.no_of_units as spec_no_of_units, spec.special_category as spec_special_category, "
            + "spec.floor_no as spec_floor_no, spec.is_inheritance as spec_is_inheritance, "
            + "doc.document_detail_id as doc_document_detail_id, doc.garbage_id as doc_garbage_id, "
            + "doc.document_type as doc_document_type, doc.file_store_id as doc_file_store_id, "
            + "clog.id as clog_id, clog.garbage_id as clog_garbage_id, clog.application_no as clog_application_no, "
            + "clog.request_no as clog_request_no, clog.status as clog_status, clog.reason as clog_reason, "
            + "clog.request_type as clog_request_type, clog.scope as clog_scope, clog.property_id as clog_property_id, "
            + "clog.workflow_status as clog_workflow_status, clog.requested_by_uuid as clog_requested_by_uuid, "
            + "clog.approved_by_uuid as clog_approved_by_uuid, clog.approved_time as clog_approved_time, "
            + "clog.rejection_reason as clog_rejection_reason, "
            + "clog.reason_details as clog_reason_details, "
            + "clog.effective_from as clog_effective_from, clog.effective_to as clog_effective_to, "
            + "clog.fee_charged as clog_fee_charged, clog.created_by as clog_created_by, clog.created_time as clog_created_time "
            + "FROM ug_gc_application app "
            + "LEFT OUTER JOIN ug_gc_specification spec ON spec.garbage_id = app.id "
            + "LEFT OUTER JOIN ug_gc_document doc ON doc.garbage_id = app.id "
            + "LEFT OUTER JOIN ug_gc_connection_log clog ON clog.garbage_id = app.id ";

    private static final String COUNT_QUERY = "SELECT count(DISTINCT app.id) FROM ug_gc_application app "
            + "LEFT OUTER JOIN ug_gc_specification spec ON spec.garbage_id = app.id ";

    public String buildSearchQuery(GarbageSearchCriteria criteria, List<Object> params) {
        StringBuilder query = new StringBuilder(BASE_QUERY);
        buildWhereClause(query, criteria, params);

        query.append(" ORDER BY app.created_time ").append(criteria.getOrderBy() != null ? criteria.getOrderBy() : "DESC");

        if (!Boolean.TRUE.equals(criteria.getIsSchedulerCall())) {
            long limit = criteria.getLimit() != null ? criteria.getLimit() : 50;
            long offset = criteria.getOffset() != null ? criteria.getOffset() : 0;
            query.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
        }

        return query.toString();
    }

    public String buildCountQuery(GarbageSearchCriteria criteria, List<Object> params) {
        StringBuilder query = new StringBuilder(COUNT_QUERY);
        buildWhereClause(query, criteria, params);
        return query.toString();
    }

    private void buildWhereClause(StringBuilder query, GarbageSearchCriteria criteria, List<Object> params) {
        query.append(" WHERE 1=1 ");

        if (StringUtils.isNotBlank(criteria.getTenantId())) {
            query.append(" AND app.tenant = ? ");
            params.add(criteria.getTenantId());
        }

        List<String> appNos = criteria.getResolvedApplicationNumbers();
        if (!CollectionUtils.isEmpty(appNos)) {
            query.append(" AND app.application_no IN (")
                    .append(createQueryPlaceholders(appNos.size()))
                    .append(") ");
            params.addAll(appNos);
        }

        List<String> grbgNos = criteria.getResolvedGarbageNumbers();
        if (!CollectionUtils.isEmpty(grbgNos)) {
            query.append(" AND (app.garbage_no IN (")
                    .append(createQueryPlaceholders(grbgNos.size()))
                    .append(") OR app.old_garbage_no IN (")
                    .append(createQueryPlaceholders(grbgNos.size()))
                    .append(") OR spec.old_garbage_no IN (")
                    .append(createQueryPlaceholders(grbgNos.size()))
                    .append(")) ");
            params.addAll(grbgNos);
            params.addAll(grbgNos);
            params.addAll(grbgNos);
        }

        List<String> propIds = criteria.getResolvedPropertyIds();
        if (!CollectionUtils.isEmpty(propIds)) {
            query.append(" AND app.property_id IN (")
                    .append(createQueryPlaceholders(propIds.size()))
                    .append(") ");
            params.addAll(propIds);
        }

        List<String> ownerUuids = criteria.getResolvedOwnerUuids();
        List<String> tenantUuids = criteria.getResolvedTenantUuids();

        String resolvedOwnerType = criteria.getResolvedOwnerType();
        if ("OWNER".equalsIgnoreCase(resolvedOwnerType)) {
            if (!CollectionUtils.isEmpty(ownerUuids)) {
                query.append(" AND app.owner_uuid IN (")
                        .append(createQueryPlaceholders(ownerUuids.size()))
                        .append(") ");
                params.addAll(ownerUuids);
            }
        } else if ("TENANT".equalsIgnoreCase(resolvedOwnerType)) {
            if (!CollectionUtils.isEmpty(tenantUuids)) {
                query.append(" AND (app.tenant_uuid IN (")
                        .append(createQueryPlaceholders(tenantUuids.size()))
                        .append(") ");
                params.addAll(tenantUuids);
                if (StringUtils.isNotBlank(criteria.getMobileNumber())) {
                    query.append(" OR (app.tenant_uuid IS NULL AND app.additional_detail::text LIKE ?) ");
                    params.add("%" + criteria.getMobileNumber() + "%");
                }
                query.append(") ");
            } else if (StringUtils.isNotBlank(criteria.getMobileNumber())) {
                query.append(" AND (app.additional_detail::text LIKE ?) ");
                params.add("%" + criteria.getMobileNumber() + "%");
            }
            query.append(" AND (spec.property_type = 'TENANT' OR app.tenant_uuid IS NOT NULL OR app.parent_application_no IS NOT NULL) ");
        } else {
            if (!CollectionUtils.isEmpty(ownerUuids) && !CollectionUtils.isEmpty(tenantUuids)) {
                query.append(" AND (app.owner_uuid IN (")
                        .append(createQueryPlaceholders(ownerUuids.size()))
                        .append(") OR app.tenant_uuid IN (")
                        .append(createQueryPlaceholders(tenantUuids.size()))
                        .append(")) ");
                params.addAll(ownerUuids);
                params.addAll(tenantUuids);
            } else if (!CollectionUtils.isEmpty(ownerUuids)) {
                query.append(" AND app.owner_uuid IN (")
                        .append(createQueryPlaceholders(ownerUuids.size()))
                        .append(") ");
                params.addAll(ownerUuids);
            } else if (!CollectionUtils.isEmpty(tenantUuids)) {
                query.append(" AND app.tenant_uuid IN (")
                        .append(createQueryPlaceholders(tenantUuids.size()))
                        .append(") ");
                params.addAll(tenantUuids);
            }
        }

        if (!CollectionUtils.isEmpty(criteria.getStatus())) {
            query.append(" AND app.status IN (")
                    .append(createQueryPlaceholders(criteria.getStatus().size()))
                    .append(") ");
            params.addAll(criteria.getStatus());
        }

        if (StringUtils.isNotBlank(criteria.getConnectionStatus())) {
            query.append(" AND app.connection_status = ? ");
            params.add(criteria.getConnectionStatus());
        }

        if (StringUtils.isNotBlank(criteria.getChannel())) {
            query.append(" AND app.channel = ? ");
            params.add(criteria.getChannel());
        }

        if (StringUtils.isNotBlank(criteria.getBusinessService())) {
            query.append(" AND app.business_service = ? ");
            params.add(criteria.getBusinessService());
        }

        if (criteria.getFromDate() != null) {
            query.append(" AND app.created_time >= ? ");
            params.add(criteria.getFromDate());
        }

        if (criteria.getToDate() != null) {
            query.append(" AND app.created_time <= ? ");
            params.add(criteria.getToDate());
        }

        if (!CollectionUtils.isEmpty(criteria.getCreatedBy())) {
            query.append(" AND app.created_by IN (")
                    .append(createQueryPlaceholders(criteria.getCreatedBy().size()))
                    .append(") ");
            params.addAll(criteria.getCreatedBy());
        }
    }

    private String createQueryPlaceholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append("?");
            if (i < count - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }
}
