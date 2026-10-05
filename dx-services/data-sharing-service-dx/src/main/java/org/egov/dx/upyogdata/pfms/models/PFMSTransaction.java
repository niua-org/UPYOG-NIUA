package org.egov.dx.upyogdata.pfms.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.dx.upyogdata.pfms.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a PFMS transaction received from a state.
 *
 * <p>The id, correlationId, status and ingestionDate fields are
 * managed internally by the UPYOG data exchange service and are
 * not expected from the state payload.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PFMSTransaction {

    private String id;

    private String correlationId;

    @NotBlank
    private String ulbCodeOrPFMSAgencyCode;

    @NotBlank
    private String voucherNumber;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate voucherDate;

    @NotBlank
    private String voucherType;

    @NotBlank
    private String financialYear;

    @NotBlank
    private String accountHeadCode;

    @NotBlank
    private String functionCode;

    @NotBlank
    private String schemeCode;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;

    private String narrationOrDescription;

    private String voucherStatus;

    private String ulbBankAccountNumber;

    private String ulbIFSCCode;

    private String instrumentReference;

    private String modeOfTransaction;

    private String beneficiaryOrPayeeName;

    private String beneficiaryAccountNumber;

    private String beneficiaryIFSCCode;

    private String beneficiaryType;

    private String challanNumber;

    private String fromAccount;

    private String toAccount;

    private String contraNature;

    private String transferInstructionReference;

    private String referenceVoucherNumber;

    private String adjustmentType;

    private String relatedAssetOrLiabilityCode;

    private String debtorOrCreditorReference;

    private String basisOfAdjustment;

    private String periodCovered;

    /*
     * These fields are populated internally by the DX service
     * before the message is published to Kafka.
     */
    private Status status;

    private LocalDateTime ingestionDate;
}