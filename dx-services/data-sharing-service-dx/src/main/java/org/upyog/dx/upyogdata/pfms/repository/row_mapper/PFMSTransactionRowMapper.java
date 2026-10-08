package org.upyog.dx.upyogdata.pfms.repository.row_mapper;

import org.upyog.dx.upyogdata.pfms.models.PFMSTransaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a database row from ug_pfms_transactions to a PFMSTransaction object.
 * Used by PFMSRepository when fetching a transaction by ID.
 */


@Component
public class PFMSTransactionRowMapper implements RowMapper<PFMSTransaction> {

    @Override
    public PFMSTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        PFMSTransaction t = new PFMSTransaction();
        t.setId(rs.getString("id"));
        t.setCorrelationId(rs.getString("correlation_id"));
        t.setUlbCodeOrPFMSAgencyCode(rs.getString("ulb_code_or_pfms_agency_code"));
        t.setVoucherNumber(rs.getString("voucher_number"));
        t.setVoucherDate(rs.getDate("voucher_date").toLocalDate());
        t.setVoucherType(rs.getString("voucher_type"));
        t.setFinancialYear(rs.getString("financial_year"));
        t.setAccountHeadCode(rs.getString("account_head_code"));
        t.setFunctionCode(rs.getString("function_code"));
        t.setSchemeCode(rs.getString("scheme_code"));
        t.setDebitAmount(rs.getBigDecimal("debit_amount"));
        t.setCreditAmount(rs.getBigDecimal("credit_amount"));
        t.setNarrationOrDescription(rs.getString("narration_or_description"));
        t.setVoucherStatus(rs.getString("voucher_status"));
        t.setUlbBankAccountNumber(rs.getString("ulb_bank_account_number"));
        t.setUlbIFSCCode(rs.getString("ulb_ifsc_code"));
        t.setInstrumentReference(rs.getString("instrument_reference"));
        t.setModeOfTransaction(rs.getString("mode_of_transaction"));
        t.setBeneficiaryOrPayeeName(rs.getString("beneficiary_or_payee_name"));
        t.setBeneficiaryAccountNumber(rs.getString("beneficiary_account_number"));
        t.setBeneficiaryIFSCCode(rs.getString("beneficiary_ifsc_code"));
        t.setBeneficiaryType(rs.getString("beneficiary_type"));
        t.setChallanNumber(rs.getString("challan_number"));
        t.setFromAccount(rs.getString("from_account"));
        t.setToAccount(rs.getString("to_account"));
        t.setContraNature(rs.getString("contra_nature"));
        t.setTransferInstructionReference(rs.getString("transfer_instruction_reference"));
        t.setReferenceVoucherNumber(rs.getString("reference_voucher_number"));
        t.setAdjustmentType(rs.getString("adjustment_type"));
        t.setRelatedAssetOrLiabilityCode(rs.getString("related_asset_or_liability_code"));
        t.setDebtorOrCreditorReference(rs.getString("debtor_or_creditor_reference"));
        t.setBasisOfAdjustment(rs.getString("basis_of_adjustment"));
        t.setPeriodCovered(rs.getString("period_covered"));
        return t;
    }
}
