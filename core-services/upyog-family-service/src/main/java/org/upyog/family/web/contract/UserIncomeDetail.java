package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Income and employment declaration model for a family member in UPYOG.
 *
 * <p>Encapsulates annual income figures, fiscal identity proofs (PAN, GSTIN),
 * third-party digital verifications (DigiLocker, GSTN), and salary documentation.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIncomeDetail {

    /**
     * Unique internal UUID primary key for the income record.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Citizen {@code uuid} of the family member declaring income.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier inherited from the parent household unit.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Occupation code dynamically validated against MDMS (e.g. {@code SALARIED}, {@code BUSINESS}).
     */
    @JsonProperty("occupation")
    private String occupation;

    /**
     * Declared annual household or individual income amount.
     */
    @JsonProperty("annualIncome")
    private BigDecimal annualIncome;

    /**
     * Tax identity document type: {@code PAN}, {@code GSTIN}, etc.
     */
    @JsonProperty("identifierType")
    private String identifierType;

    /**
     * Masked or verified fiscal identifier string (e.g. {@code AFZPK7190K}).
     */
    @JsonProperty("identifierValue")
    private String identifierValue;

    /**
     * Flag indicating whether the declared income source has been digitally verified.
     */
    @JsonProperty("isVerified")
    private Boolean isVerified;

    /**
     * Verification provider: {@code DIGILOCKER}, {@code GSTN}, {@code INCOME_TAX_PORTAL}, etc.
     */
    @JsonProperty("verificationSource")
    private String verificationSource;

    /**
     * Flag indicating whether identity or OTP verification was successfully completed with verification source.
     */
    @JsonProperty("otpVerified")
    private Boolean otpVerified;

    /**
     * Attached salary slip or employer certificate proof.
     */
    @Valid
    @JsonProperty("salaryProofDocument")
    private UserDocument salaryProofDocument;

    /**
     * Attached PAN card, GST registration certificate, or tax assessment proof.
     */
    @Valid
    @JsonProperty("taxProofDocument")
    private UserDocument taxProofDocument;

    /**
     * Dynamic JSON metadata for employer details or industry sub-categories.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Standard UPYOG audit metadata tracking creation and modification actor UUIDs and timestamps.
     */
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
