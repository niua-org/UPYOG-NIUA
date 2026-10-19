package org.egov.user.web.contract.multiauth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.RequestInfo;

import java.io.Serializable;

/**
 * Unified request DTO for the loginv2 multi-auth endpoint.
 *
 * <p>Pure Data Transfer Object (DTO) adhering to the Single Responsibility Principle (SRP).
 * All validation and strategy resolution is decoupled into dedicated service components.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Unified request payload for multi-channel login V2")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"otp", "aadhaarNumber"})
public class LoginV2Request implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Authentication channel: MOBILE_OTP, AADHAAR_OTP, or FAMILY_ID", example = "MOBILE_OTP")
    @JsonProperty("authType")
    private AuthType authType;

    @Schema(description = "Tenant identifier", example = "pg.citya", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("tenantId")
    @NotNull(message = "tenantId is mandatory for loginv2")
    private String tenantId;

    @Schema(description = "Citizen 10-digit mobile number", example = "9876543210")
    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @Schema(description = "One-time password", example = "123456")
    @JsonProperty("otp")
    private String otp;

    @Schema(description = "12-digit Aadhaar number or 16-digit Virtual ID", example = "XXXXXXXX1234")
    @JsonProperty("aadhaarNumber")
    private String aadhaarNumber;

    @Schema(description = "Transaction ID returned from OTP generation step", example = "txn-9a8b-1234")
    @JsonProperty("txnId")
    private String txnId;

    @Schema(description = "UIDAI reference ID", example = "ref-123456")
    @JsonProperty("referenceId")
    private String referenceId;

    @Schema(description = "Family ID / Ration Card number", example = "FAM-2026-9988")
    @JsonProperty("familyId")
    private String familyId;

    @Schema(description = "User full name (optional fallback for new users)", example = "Ravi Kumar")
    @JsonProperty("name")
    private String name;

    @Schema(description = "OAuth token scope", example = "read write")
    @JsonProperty("scope")
    @Builder.Default
    private String scope = "read write";

    @Schema(description = "UPYOG user type", example = "CITIZEN")
    @JsonProperty("userType")
    @Builder.Default
    private String userType = "CITIZEN";

    @Schema(description = "UUID of selected citizen user profile when multiple accounts are linked to this mobile number", example = "e5b881ef-2d88-4c91-b3b3-df7b06b6a482")
    @JsonProperty("selectedUserUuid")
    @JsonAlias({"userUuid", "uuid"})
    private String selectedUserUuid;
}
