package org.upyog.dx.upyogdata.pfms.repository.row_mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.upyog.dx.upyogdata.pfms.entity.PfmsTransactionAdditionalDetails;
import org.upyog.dx.upyogdata.pfms.entity.PfmsTransactionEntity;
import org.upyog.dx.upyogdata.pfms.enums.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
@Component
@RequiredArgsConstructor
public class PFMSTransactionRowMapper implements RowMapper<PfmsTransactionEntity> {

    private final ObjectMapper objectMapper;

    @Override
    public PfmsTransactionEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        PfmsTransactionEntity entity = PfmsTransactionEntity.builder()
                .id(rs.getString("id"))
                .correlationId(rs.getString("correlation_id"))
                .ulbCodeOrPFMSAgencyCode(rs.getString("agencyCode"))
                .voucherNumber(rs.getString("voucher_number"))
                .voucherDate(rs.getDate("voucher_date").toLocalDate())
                .voucherType(rs.getString("voucherType"))
                .financialYear(rs.getString("financial_year"))
                .build();

        String status = rs.getString("status");
        if (status != null) {
            try {
                entity.setStatus(Status.valueOf(status));
            } catch (IllegalArgumentException ex) {
                entity.setStatus(null);
            }
        }

        var ingestionTimestamp = rs.getTimestamp("ingestion_date");
        if (ingestionTimestamp != null) {
            entity.setIngestionDate(ingestionTimestamp.toLocalDateTime());
        }

        String json = rs.getString("additionaldetails");
        if (json != null && !json.isBlank()) {
            try {
                entity.setAdditionalDetails(objectMapper.readValue(json, PfmsTransactionAdditionalDetails.class));
            } catch (Exception e) {
                throw new SQLException("Failed to parse additionaldetails JSON", e);
            }
        }
        return entity;
    }
}
