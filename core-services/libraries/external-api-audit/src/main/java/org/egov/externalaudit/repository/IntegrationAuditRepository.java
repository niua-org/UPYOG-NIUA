package org.egov.externalaudit.repository;

import lombok.extern.slf4j.Slf4j;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class IntegrationAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public IntegrationAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int markTimedOutInitiatedRequests(long requestTimeThreshold, long lastModifiedTime) {
        String sql = """
                UPDATE ug_external_api_message_detail
                SET status = ?, last_modified_time = ?
                WHERE status = ? AND request_time < ?
                """;
        int updatedRows = jdbcTemplate.update(sql,
                ExternalApiAuditConstants.STATUS_TIMED_OUT,
                lastModifiedTime,
                ExternalApiAuditConstants.STATUS_INITIATED,
                requestTimeThreshold);
        if (updatedRows > 0) {
            log.info("Marked {} stale integration audit records as TIMED_OUT", updatedRows);
        }
        return updatedRows;
    }

    public int deleteExpiredAuditRecords(long createdTimeThreshold) {
        int errors = jdbcTemplate.update("""
                DELETE FROM ug_external_api_error_detail e
                USING ug_external_api_message_detail m
                WHERE e.correlation_id = m.correlation_id AND m.created_time < ?
                """, createdTimeThreshold);
        int raw = jdbcTemplate.update("""
                DELETE FROM ug_external_api_message_raw_detail r
                USING ug_external_api_message_detail m
                WHERE r.correlation_id = m.correlation_id AND m.created_time < ?
                """, createdTimeThreshold);
        int messages = jdbcTemplate.update("""
                DELETE FROM ug_external_api_message_detail
                WHERE created_time < ?
                """, createdTimeThreshold);
        int total = errors + raw + messages;
        if (total > 0) {
            log.info("Deleted expired integration audit rows. errors={}, raw={}, messages={}", errors, raw, messages);
        }
        return total;
    }
}
