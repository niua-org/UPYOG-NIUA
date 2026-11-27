package org.egov.user.web.contract.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.asset.UserIncome;

import java.math.BigDecimal;

/**
 * Request payload envelope for standalone citizen income CRUD operations (create, update, delete/deactivate).
 *
 * <p>Supports passing either a composite {@link UserIncome} or flat income fields
 * for seamless integration with member editing forms and administrative dashboards.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Request envelope for standalone citizen income CRUD and lifecycle mutations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IncomeRequest {

    /**
     * Standard UPYOG RequestInfo contract.
     */
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Complete income entity representing declared income details.
     */
    @Valid
    @JsonProperty("income")
    private UserIncome income;

    /**
     * Citizen {@code uuid} owning this income record.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Alternate citizen UUID alias.
     */
    @JsonProperty("uuid")
    private String uuid;

    /**
     * Income primary key ID (required for update and delete/deactivation operations).
     */
    @JsonProperty("incomeId")
    private String incomeId;

    /**
     * Alternate income primary key ID alias.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Tenant identifier scoping the income record.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Occupation classification code (e.g. SALARIED, SELF_EMPLOYED, BUSINESS, FARMER).
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
    private Boolean isVerified;

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
    private Boolean otpVerified;

    /**
     * Attached salary proof document contract.
     */
    @Valid
    @JsonProperty("salaryProofDocument")
    private UserDocument salaryProofDocument;

    /**
     * Attached tax proof document contract.
     */
    @Valid
    @JsonProperty("taxProofDocument")
    private UserDocument taxProofDocument;

    /**
     * Lifecycle status override (e.g. {@code ACTIVE} or {@code INACTIVE}).
     */
    @JsonProperty("status")
    private String status;

    /**
     * Extensible dynamic JSON metadata.
     */
    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    /**
     * Resolves citizen UUID from {@code userUuid}, {@code uuid}, or the embedded {@code income}.
     *
     * @return effective citizen user UUID string
     */
    public String resolveUserUuid() {
        if (StringUtils.isNotBlank(this.userUuid)) {
            return this.userUuid.trim();
        }
        if (StringUtils.isNotBlank(this.uuid)) {
            return this.uuid.trim();
        }
        if (this.income != null && StringUtils.isNotBlank(this.income.getUserUuid())) {
            return this.income.getUserUuid().trim();
        }
        return null;
    }

    /**
     * Resolves income primary key ID from {@code incomeId}, {@code id}, or the embedded {@code income}.
     *
     * @return effective income ID string
     */
    public String resolveIncomeId() {
        if (StringUtils.isNotBlank(this.incomeId)) {
            return this.incomeId.trim();
        }
        if (StringUtils.isNotBlank(this.id)) {
            return this.id.trim();
        }
        if (this.income != null && StringUtils.isNotBlank(this.income.getId())) {
            return this.income.getId().trim();
        }
        return null;
    }

    /**
     * Resolves tenant identifier from {@code tenantId} or embedded {@code income}.
     *
     * @return effective tenant ID string
     */
    public String resolveTenantId() {
        if (StringUtils.isNotBlank(this.tenantId)) {
            return this.tenantId.trim();
        }
        if (this.income != null && StringUtils.isNotBlank(this.income.getTenantId())) {
            return this.income.getTenantId().trim();
        }
        return null;
    }

    /**
     * Assembles and resolves a unified {@link UserIncome} domain model from the request attributes.
     *
     * @return populated {@link UserIncome} entity ready for validation and persistence
     */
    public UserIncome resolveIncome() {
        UserIncome target = this.income != null ? this.income : new UserIncome();

        String effectiveUserUuid = resolveUserUuid();
        if (StringUtils.isNotBlank(effectiveUserUuid)) {
            target.setUserUuid(effectiveUserUuid);
        }

        String effectiveIncomeId = resolveIncomeId();
        if (StringUtils.isNotBlank(effectiveIncomeId)) {
            target.setId(effectiveIncomeId);
        }

        String effectiveTenantId = resolveTenantId();
        if (StringUtils.isNotBlank(effectiveTenantId)) {
            target.setTenantId(effectiveTenantId);
        }

        if (StringUtils.isNotBlank(this.occupation)) {
            target.setOccupation(this.occupation.trim());
        }

        if (this.annualIncome != null) {
            target.setAnnualIncome(this.annualIncome);
        }

        if (StringUtils.isNotBlank(this.identifierType)) {
            target.setIdentifierType(this.identifierType.trim().toUpperCase());
        }

        if (StringUtils.isNotBlank(this.identifierValue)) {
            target.setIdentifierValue(this.identifierValue.trim().toUpperCase());
        }

        if (this.isVerified != null) {
            target.setIsVerified(this.isVerified);
        }

        if (StringUtils.isNotBlank(this.verificationSource)) {
            target.setVerificationSource(this.verificationSource.trim());
        }

        if (this.verificationDate != null) {
            target.setVerificationDate(this.verificationDate);
        }

        if (this.otpVerified != null) {
            target.setOtpVerified(this.otpVerified);
        }

        if (this.salaryProofDocument != null) {
            target.setSalaryProofDocument(this.salaryProofDocument);
        }

        if (this.taxProofDocument != null) {
            target.setTaxProofDocument(this.taxProofDocument);
        }

        if (StringUtils.isNotBlank(this.status)) {
            target.setStatus(this.status.trim().toUpperCase());
        }

        if (this.additionalDetails != null) {
            target.setAdditionalDetails(this.additionalDetails);
        }

        return target;
    }
}
