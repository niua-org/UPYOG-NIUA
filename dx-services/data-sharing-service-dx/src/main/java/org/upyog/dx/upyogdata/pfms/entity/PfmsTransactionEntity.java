package org.upyog.dx.upyogdata.pfms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.upyog.dx.upyogdata.pfms.enums.Status;
import org.upyog.dx.upyogdata.pfms.models.PFMSTransaction;
import org.upyog.dx.upyogdata.pfms.models.PFMSTransactionFormData;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persistence and Kafka representation of a PFMS transaction.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PfmsTransactionEntity {

    private String id;
    private String correlationId;
    private String ulbCodeOrPFMSAgencyCode;
    private String voucherNumber;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate voucherDate;

    private String voucherType;
    private String financialYear;
    private PfmsTransactionAdditionalDetails additionalDetails;
    private Status status;
    private LocalDateTime ingestionDate;

    /**
     * Flat view used only for PFMS outbound form mapping until FormData is entity-native.
     */
    public PFMSTransaction toPfmsPushView() {
        PFMSTransaction view = new PFMSTransaction();
        view.setId(id);
        view.setCorrelationId(correlationId);
        view.setUlbCodeOrPFMSAgencyCode(ulbCodeOrPFMSAgencyCode);
        view.setVoucherNumber(voucherNumber);
        view.setVoucherDate(voucherDate);
        view.setVoucherType(voucherType);
        view.setFinancialYear(financialYear);
        view.setStatus(status);
        view.setIngestionDate(ingestionDate);

        if (additionalDetails != null) {
            view.setAccountHeadCode(additionalDetails.getAccountHeadCode());
            view.setFunctionCode(additionalDetails.getFunctionCode());
            view.setSchemeCode(additionalDetails.getSchemeCode());
            view.setDebitAmount(additionalDetails.getDebitAmount());
            view.setCreditAmount(additionalDetails.getCreditAmount());
            view.setNarrationOrDescription(additionalDetails.getNarrationOrDescription());
            view.setVoucherStatus(additionalDetails.getVoucherStatus());
            view.setUlbBankAccountNumber(additionalDetails.getUlbBankAccountNumber());
            view.setUlbIFSCCode(additionalDetails.getUlbIFSCCode());
            view.setInstrumentReference(additionalDetails.getInstrumentReference());
            view.setModeOfTransaction(additionalDetails.getModeOfTransaction());
            view.setBeneficiaryOrPayeeName(additionalDetails.getBeneficiaryOrPayeeName());
            view.setBeneficiaryAccountNumber(additionalDetails.getBeneficiaryAccountNumber());
            view.setBeneficiaryIFSCCode(additionalDetails.getBeneficiaryIFSCCode());
            view.setBeneficiaryType(additionalDetails.getBeneficiaryType());
            view.setChallanNumber(additionalDetails.getChallanNumber());
            view.setFromAccount(additionalDetails.getFromAccount());
            view.setToAccount(additionalDetails.getToAccount());
            view.setContraNature(additionalDetails.getContraNature());
            view.setTransferInstructionReference(additionalDetails.getTransferInstructionReference());
            view.setReferenceVoucherNumber(additionalDetails.getReferenceVoucherNumber());
            view.setAdjustmentType(additionalDetails.getAdjustmentType());
            view.setRelatedAssetOrLiabilityCode(additionalDetails.getRelatedAssetOrLiabilityCode());
            view.setDebtorOrCreditorReference(additionalDetails.getDebtorOrCreditorReference());
            view.setBasisOfAdjustment(additionalDetails.getBasisOfAdjustment());
            view.setPeriodCovered(additionalDetails.getPeriodCovered());
        }
        return view;
    }

    public PFMSTransactionFormData toFormData(String clientIp) {
        return PFMSTransactionFormData.from(toPfmsPushView(), clientIp);
    }
}
