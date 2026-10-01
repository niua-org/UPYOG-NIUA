package org.egov.dx.upyogdata.pfms.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

/**
 * Represents the form data sent to the PFMS data push API.
 * Field names are in PascalCase as required by the PFMS API.
 * Use the from() method to build this from a PFMSTransaction object.
 */
@Getter
@Setter
@Builder
public class PFMSTransactionFormData {

    private String ULBCodeOrPFMSAgencyCode;
    private String VoucherNumber;
    private String VoucherDate;
    private String VoucherType;
    private String FinancialYear;
    private String AccountHeadCode;
    private String FunctionCode;
    private String SchemeCode;
    private String DebitAmount;
    private String CreditAmount;
    private String NarrationOrDescription;
    private String VoucherStatus;
    private String ULBBankAccountNumber;
    private String ULBIFSCCode;
    private String InstrumentReference;
    private String ModeOfTransaction;
    private String BeneficiaryOrPayeeName;
    private String BeneficiaryAccountNumber;
    private String BeneficiaryIFSCCode;
    private String BeneficiaryType;
    private String ChallanNumber;
    private String FromAccount;
    private String ToAccount;
    private String ContraNature;
    private String TransferInstructionReference;
    private String ReferenceVoucherNumber;
    private String AdjustmentType;
    private String RelatedAssetOrLiabilityCode;
    private String DebtorOrCreditorReference;
    private String BasisOfAdjustment;
    private String PeriodCovered;
    private String ClientIP;

    private static final DateTimeFormatter PFMS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Builds a PFMSTransactionFormData from a PFMSTransaction and the client IP.
     * Formats the voucher date as yyyy/MM/dd as expected by PFMS.
     *
     * @param t        the transaction to convert
     * @param clientIp the IP address of this service, required by PFMS
     * @return ready-to-send form data object
     */
    public static PFMSTransactionFormData from(PFMSTransaction t, String clientIp) {
        return PFMSTransactionFormData.builder()
                .ULBCodeOrPFMSAgencyCode(t.getUlbCodeOrPFMSAgencyCode())
                .VoucherNumber(t.getVoucherNumber())
                .VoucherDate(t.getVoucherDate().format(PFMS_DATE_FORMAT))
                .VoucherType(t.getVoucherType())
                .FinancialYear(t.getFinancialYear())
                .AccountHeadCode(t.getAccountHeadCode())
                .FunctionCode(t.getFunctionCode())
                .SchemeCode(t.getSchemeCode())
                .DebitAmount(t.getDebitAmount() == null ? "" : t.getDebitAmount().toString())
                .CreditAmount(t.getCreditAmount() == null ? "" : t.getCreditAmount().toString())
                .NarrationOrDescription(t.getNarrationOrDescription())
                .VoucherStatus(t.getVoucherStatus())
                .ULBBankAccountNumber(t.getUlbBankAccountNumber())
                .ULBIFSCCode(t.getUlbIFSCCode())
                .InstrumentReference(t.getInstrumentReference())
                .ModeOfTransaction(t.getModeOfTransaction())
                .BeneficiaryOrPayeeName(t.getBeneficiaryOrPayeeName())
                .BeneficiaryAccountNumber(t.getBeneficiaryAccountNumber())
                .BeneficiaryIFSCCode(t.getBeneficiaryIFSCCode())
                .BeneficiaryType(t.getBeneficiaryType())
                .ChallanNumber(t.getChallanNumber())
                .FromAccount(t.getFromAccount())
                .ToAccount(t.getToAccount())
                .ContraNature(t.getContraNature())
                .TransferInstructionReference(t.getTransferInstructionReference())
                .ReferenceVoucherNumber(t.getReferenceVoucherNumber())
                .AdjustmentType(t.getAdjustmentType())
                .RelatedAssetOrLiabilityCode(t.getRelatedAssetOrLiabilityCode())
                .DebtorOrCreditorReference(t.getDebtorOrCreditorReference())
                .BasisOfAdjustment(t.getBasisOfAdjustment())
                .PeriodCovered(t.getPeriodCovered())
                .ClientIP(clientIp)
                .build();
    }
}
