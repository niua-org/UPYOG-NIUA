package org.egov.user.domain.model.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.user.domain.model.UserDocument;

import java.math.BigDecimal;

/**
 * Domain entity representing citizen income and tax identifier declarations in {@code ug_user_income}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserIncome {

    /**
     * Unique internal UUID primary key for the income record in {@code ug_user_income}.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Citizen {@code uuid} owning this income record.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Tenant identifier scoping the income record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Occupation classification code (dynamically validated against MDMS).
     */
    @JsonProperty("occupation")
    private String occupation;

    /**
     * Declared annual income figure.
     */
    @JsonProperty("annualIncome")
    private BigDecimal annualIncome;

    /**
     * Fiscal identifier type: {@code PAN}, {@code TAN}, or {@code GSTIN}.
     */
    @JsonProperty("identifierType")
    private String identifierType;

    /**
     * Fiscal identifier string value (e.g. PAN number).
     */
    @JsonProperty("identifierValue")
    private String identifierValue;

    /**
     * Flag indicating whether income has been digitally authenticated.
     */
    @JsonProperty("isVerified")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Verification provider/source (e.g. DIGILOCKER, GSTN).
     */
    @JsonProperty("verificationSource")
    private String verificationSource;

    /**
     * Epoch millisecond timestamp when digital verification occurred.
     */
    @JsonProperty("verificationDate")
    private Long verificationDate;

    /**
     * Flag indicating whether OTP or identity verification was authenticated.
     */
    @JsonProperty("otpVerified")
    @Builder.Default
    private Boolean otpVerified = false;

    /**
     * Foreign key reference to salary slip proof in {@code ug_user_document}.
     */
    @JsonProperty("salaryProofDocId")
    private String salaryProofDocId;

    /**
     * Attached salary proof document contract.
     */
    @Valid
    @JsonProperty("salaryProofDocument")
    private UserDocument salaryProofDocument;

    /**
     * Foreign key reference to tax assessment proof in {@code ug_user_document}.
     */
    @JsonProperty("taxProofDocId")
    private String taxProofDocId;

    /**
     * Attached tax proof document contract.
     */
    @Valid
    @JsonProperty("taxProofDocument")
    private UserDocument taxProofDocument;

    /**
     * Lifecycle status of the income record (defaults to {@code ACTIVE}).
     */
    @JsonProperty("status")
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Dynamic extensible JSON metadata stored as JSONB.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Actor UUID who created this record.
     */
    @JsonProperty("createdBy")
    private String createdBy;

    /**
     * Epoch millisecond timestamp of record creation.
     */
    @JsonProperty("createdTime")
    private Long createdTime;

    /**
     * Actor UUID who last modified this record.
     */
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    /**
     * Epoch millisecond timestamp of last modification.
     */
    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;
}
