package org.egov.garbageservice.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.kafka.Producer;
import org.egov.garbageservice.repository.GarbageRepository;
import org.egov.garbageservice.repository.builder.GarbageQueryBuilder;
import org.egov.garbageservice.repository.rowmapper.GarbageRowMapper;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GarbageRepository executing JDBC queries against consolidated tables
 * and publishing persistence events to Kafka topics.
 */
@Repository
@Slf4j
public class GarbageRepositoryImpl implements GarbageRepository {

    private final Producer producer;
    private final GarbageQueryBuilder queryBuilder;
    private final GarbageRowMapper rowMapper;
    private final JdbcTemplate jdbcTemplate;
    private final GarbageServiceConfig config;



    @Autowired
    public GarbageRepositoryImpl(Producer producer, GarbageQueryBuilder queryBuilder,
                                 GarbageRowMapper rowMapper, JdbcTemplate jdbcTemplate,
                                 GarbageServiceConfig config) {
        this.producer = producer;
        this.queryBuilder = queryBuilder;
        this.rowMapper = rowMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    @Override
    public void saveGarbageApplication(GarbageRequest garbageRequest) {
        log.info("Publishing save-garbage-application event for applications count: {}",
                garbageRequest.getGarbageApplications() != null ? garbageRequest.getGarbageApplications().size() : 0);
        producer.push(config.getSaveGarbageApplicationTopic(), garbageRequest);
    }

    @Override
    public void updateGarbageApplication(GarbageRequest garbageRequest) {
        log.info("Publishing update-garbage-application event for applications count: {}",
                garbageRequest.getGarbageApplications() != null ? garbageRequest.getGarbageApplications().size() : 0);
        producer.push(config.getUpdateGarbageApplicationTopic(), garbageRequest);
    }

    @Override
    public void saveConnectionLog(GarbageConnectionLog connectionLog) {
        log.info("Publishing save-garbage-connection-log for requestNo: {}", connectionLog.getRequestNo());
        producer.push(config.getSaveConnectionLogTopic(), connectionLog);
    }

    @Override
    public void saveMonthlyBill(GarbageMonthlyRentPayment monthlyRentPayment) {
        log.info("Publishing save-garbage-monthly-bill for billNo: {}", monthlyRentPayment.getBillNo());
        producer.push(config.getMonthlyRentPaymentSaveTopic(), monthlyRentPayment);
    }

    @Override
    public void saveBillGenerate(GarbageBillGenerate billGenerate) {
        log.info("Publishing save-garbage-scheduler-log for garbageId: {}", billGenerate.getGarbageId());
        producer.push(config.getSchedulerLogTopic(), billGenerate);
    }

    @Override
    public List<GarbageApplication> getGarbageApplications(GarbageSearchCriteria criteria) {
        List<Object> params = new ArrayList<>();
        String query = queryBuilder.buildSearchQuery(criteria, params);
        log.info("Executing getGarbageApplications query: {} with params: {}", query, params);
        return jdbcTemplate.query(query, rowMapper, params.toArray());
    }

    @Override
    public Integer getApplicationsCount(GarbageSearchCriteria criteria) {
        List<Object> params = new ArrayList<>();
        String query = queryBuilder.buildCountQuery(criteria, params);
        log.info("Executing count query: {} with params: {}", query, params);
        return jdbcTemplate.queryForObject(query, Integer.class, params.toArray());
    }

    @Override
    public List<GarbageConnectionLog> getConnectionLogs(String propertyId, String applicationNo) {
        StringBuilder query = new StringBuilder("SELECT * FROM ug_gc_connection_log WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(propertyId)) {
            query.append(" AND property_id = ? ");
            params.add(propertyId);
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(applicationNo)) {
            query.append(" AND application_no = ? ");
            params.add(applicationNo);
        }
        query.append(" ORDER BY created_time DESC");
        return jdbcTemplate.query(query.toString(), (rs, rowNum) -> {
            java.sql.Date effFrom = rs.getDate("effective_from");
            java.sql.Date effTo = rs.getDate("effective_to");
            return GarbageConnectionLog.builder()
                    .id(rs.getString("id"))
                    .garbageId(rs.getLong("garbage_id"))
                    .applicationNo(rs.getString("application_no"))
                    .requestNo(rs.getString("request_no"))
                    .requestType(rs.getString("request_type"))
                    .scope(rs.getString("scope"))
                    .propertyId(rs.getString("property_id"))
                    .workflowStatus(rs.getString("workflow_status"))
                    .requestedByUuid(rs.getString("requested_by_uuid"))
                    .approvedByUuid(rs.getString("approved_by_uuid"))
                    .approvedTime(rs.getLong("approved_time"))
                    .rejectionReason(rs.getString("rejection_reason"))
                    .status(rs.getString("status"))
                    .reason(rs.getString("reason"))
                    .reasonDetails(rs.getString("reason_details"))
                    .effectiveFrom(effFrom != null ? effFrom.toLocalDate() : null)
                    .effectiveTo(effTo != null ? effTo.toLocalDate() : null)
                    .feeCharged(rs.getBigDecimal("fee_charged"))
                    .auditDetails(AuditDetails.builder()
                            .createdBy(rs.getString("created_by"))
                            .createdTime(rs.getLong("created_time"))
                            .lastModifiedBy(rs.getString("last_modified_by"))
                            .lastModifiedTime(rs.getLong("last_modified_time"))
                            .build())
                    .build();
        }, params.toArray());
    }

    @Override
    public Long getNextSequence() {
        return jdbcTemplate.queryForObject(GarbageQueryBuilder.SELECT_NEXT_SEQUENCE, Long.class);
    }
}
