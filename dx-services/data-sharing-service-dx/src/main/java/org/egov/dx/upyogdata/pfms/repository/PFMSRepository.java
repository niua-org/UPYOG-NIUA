package org.egov.dx.upyogdata.pfms.repository;

import lombok.RequiredArgsConstructor;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.models.SchedulerLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PFMSRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Checks whether any of the given voucher numbers already exist in the database
     * for a specific voucher date.
     *
     * This is used as a pre-Kafka-push duplicate check. Since the persister service
     * consumes from Kafka asynchronously, any duplicate would silently fail there.
     * By querying the DB here (before pushing), we catch duplicates early and return
     * a proper error to the caller.
     *
     * Example scenario: State pushes V001 for date 29/09/2026. If V001 already
     * exists in pfms_transactions for that date, this method returns ["V001"],
     * and the service throws an error before touching Kafka.
     *
     * @param voucherNumbers list of voucher numbers from the incoming request
     * @param voucherDate    the voucher date to check against
     * @return list of voucher numbers that already exist in DB for the given date,
     *         empty list if no duplicates found
     */
    public List<String> findExistingVoucherNumbers(List<String> voucherNumbers, LocalDate voucherDate) {
        String sql = """
                SELECT voucher_number FROM public.ug_pfms_transactions
                WHERE voucher_number = ANY(?) AND voucher_date = ?
                """;
        return jdbcTemplate.queryForList(sql, String.class,
                voucherNumbers.toArray(new String[0]), voucherDate);
    }

    /**
     * Fetches all transactions with INITIATED status in batches.
     * These are records received from state but not yet forwarded to PFMS.
     *
     * @param batchSize max number of records to fetch in one scheduler cycle
     * @return list of transaction UUIDs with INITIATED status
     */
    public List<String> fetchInitiatedTransactionIds(int batchSize) {
        String sql = """
            SELECT id FROM public.ug_pfms_transactions
            WHERE status = 'INITIATED'
            ORDER BY created_time ASC
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql, String.class, batchSize);
    }

    /**
     * Fetches full transaction details by ID.
     * Used by scheduler to load each INITIATED transaction before pushing to PFMS.
     *
     * @param id transaction UUID
     * @return PFMSTransaction object mapped from DB row
     */
    public PFMSTransaction fetchTransactionById(String id) {
        String sql = "SELECT * FROM public.ug_pfms_transactions WHERE id = ?::UUID";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
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
        }, id);
    }

    /**
     * Updates only status and ingestion_date of a transaction.
     * Called after PFMS API responds — success or failure.
     *
     * status column mein PFMS ka raw response string save hota hai as-is
     * (e.g. "Success", "FAILED" on exception).
     *
     * @param id            transaction UUID
     * @param status        PFMS raw response string ya "PROCESSING" / "FAILED"
     * @param ingestionDate timestamp when PFMS responded, null for PROCESSING
     */
    public void updateTransactionStatus(String id, String status, LocalDateTime ingestionDate) {
        String sql = """
        UPDATE public.ug_pfms_transactions
        SET status = ?, ingestion_date = ?, last_modified_time = CURRENT_TIMESTAMP
        WHERE id = ?::UUID
        """;
        jdbcTemplate.update(sql, status, ingestionDate, id);
    }

    public List<String> fetchFailedTransactionIds(int batchSize) {
        String sql = """
        SELECT id FROM public.ug_pfms_transactions
        WHERE status = 'FAILED'
        ORDER BY last_modified_time ASC
        LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, String.class, batchSize);
    }

    public void saveSchedulerLog(SchedulerLog log) {
        String sql = """
    INSERT INTO public.ug_pfms_scheduler_log
        (scheduler_type, started_at, ended_at, duration_ms, status,
         total_picked, success_count, failed_count, created_by, last_modified_time)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
        jdbcTemplate.update(sql,
                log.getSchedulerType(), log.getStartedAt(), log.getEndedAt(),
                log.getDurationMs(), log.getStatus(),
                log.getTotalPicked(), log.getSuccessCount(), log.getFailedCount(),
                log.getCreatedBy(), log.getStartedAt());
    }

}
