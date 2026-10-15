package org.upyog.dx.upyogdata.pfms.repository.row_mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.upyog.dx.upyogdata.pfms.models.PFMSTransaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * Maps a database row from ug_pfms_transactions to a PFMSTransaction object.
 * Used by PFMSRepository when fetching a transaction by ID.
 */

@Component
@RequiredArgsConstructor
public class PFMSTransactionRowMapper implements RowMapper<PFMSTransaction> {

    private final ObjectMapper objectMapper;

    @Override
    public PFMSTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        PFMSTransaction pfmsTransaction = new PFMSTransaction();

        // Flat columns
        pfmsTransaction.setId(rs.getString("id"));
        pfmsTransaction.setCorrelationId(rs.getString("correlation_id"));
        pfmsTransaction.setUlbCodeOrPFMSAgencyCode(rs.getString("agencyCode"));
        pfmsTransaction.setVoucherNumber(rs.getString("voucher_number"));
        pfmsTransaction.setVoucherDate(rs.getDate("voucher_date").toLocalDate());
        pfmsTransaction.setVoucherType(rs.getString("voucherType"));
        pfmsTransaction.setFinancialYear(rs.getString("financial_year"));

        // Deserialize additionaldetails JSONB back into fields
        String json = rs.getString("additionaldetails");
        if (json != null) {
            try {
                Map<String, Object> details = objectMapper.readValue(json, new TypeReference<>() {});
                pfmsTransaction.setAccountHeadCode(getList(details, "accountHeadCode"));
                pfmsTransaction.setFunctionCode(getList(details, "functionCode"));
                pfmsTransaction.setSchemeCode(getList(details, "schemeCode"));
                pfmsTransaction.setDebitAmount(getBigDecimal(details, "debitAmount"));
                pfmsTransaction.setCreditAmount(getBigDecimal(details, "creditAmount"));
                pfmsTransaction.setNarrationOrDescription(getString(details, "narrationOrDescription"));
                pfmsTransaction.setVoucherStatus(getString(details, "voucherStatus"));
                pfmsTransaction.setUlbBankAccountNumber(getString(details, "ulbBankAccountNumber"));
                pfmsTransaction.setUlbIFSCCode(getString(details, "ulbIFSCCode"));
                pfmsTransaction.setInstrumentReference(getString(details, "instrumentReference"));
                pfmsTransaction.setModeOfTransaction(getString(details, "modeOfTransaction"));
                pfmsTransaction.setBeneficiaryOrPayeeName(getString(details, "beneficiaryOrPayeeName"));
                pfmsTransaction.setBeneficiaryAccountNumber(getString(details, "beneficiaryAccountNumber"));
                pfmsTransaction.setBeneficiaryIFSCCode(getString(details, "beneficiaryIFSCCode"));
                pfmsTransaction.setBeneficiaryType(getString(details, "beneficiaryType"));
                pfmsTransaction.setChallanNumber(getString(details, "challanNumber"));
                pfmsTransaction.setFromAccount(getString(details, "fromAccount"));
                pfmsTransaction.setToAccount(getString(details, "toAccount"));
                pfmsTransaction.setContraNature(getString(details, "contraNature"));
                pfmsTransaction.setTransferInstructionReference(getString(details, "transferInstructionReference"));
                pfmsTransaction.setReferenceVoucherNumber(getString(details, "referenceVoucherNumber"));
                pfmsTransaction.setAdjustmentType(getString(details, "adjustmentType"));
                pfmsTransaction.setRelatedAssetOrLiabilityCode(getString(details, "relatedAssetOrLiabilityCode"));
                pfmsTransaction.setDebtorOrCreditorReference(getString(details, "debtorOrCreditorReference"));
                pfmsTransaction.setBasisOfAdjustment(getString(details, "basisOfAdjustment"));
                pfmsTransaction.setPeriodCovered(getString(details, "periodCovered"));
            } catch (Exception e) {
                throw new SQLException("Failed to parse additionaldetails JSON", e);
            }
        }
        return pfmsTransaction;
    }

    /**
     * Retrieves a String value from the map for the specified key.
     *
     * @param map the map containing the key-value pairs; may be null or empty
     * @param key the key whose associated value is to be returned
     * @return the String representation of the value if present; {@code null} if the key is not found or the value is null
     */
    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * Retrieves a List of Strings from the map for the specified key.
     *
     * @param map the map containing the key-value pairs; may be null or empty
     * @param key the key whose associated list is to be returned
     * @return the value cast to a {@code List<String>} if it is an instance of {@code List}; an empty list otherwise
     */
    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<String>) val;
        return Collections.emptyList();
    }

    /**
     * Safely parses and retrieves a {@link BigDecimal} value from the map for the specified key.
     *
     * @param map the map containing the key-value pairs; may be null or empty
     * @param key the key whose associated value is to be converted to {@code BigDecimal}
     * @return the value as a {@code BigDecimal} if valid; {@code null} if the key is missing,
     *         the value is null, or string representation cannot be parsed into a {@code BigDecimal}
     */
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
