package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

import java.util.Map;

/**
 * Request DTO for resyncing citizen profile with Aadhaar via dedicated UI trigger.
 */
@Schema(description = "Request payload for explicitly triggering Aadhaar e-KYC re-synchronization")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResyncRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Tenant identifier", example = "pg.citya", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("tenantId")
    @JsonAlias({"tenant_id"})
    @NotBlank(message = "tenantId is mandatory for profile re-sync")
    private String tenantId;

    @Schema(description = "Citizen UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("uuid")
    @JsonAlias({"userUuid", "user_uuid", "citizenUuid"})
    private String uuid;

    @Schema(description = "Internal user numeric ID", example = "1024")
    @JsonProperty("id")
    @JsonAlias({"userId", "user_id"})
    private Long id;

    @Schema(description = "Citizen mobile number", example = "9876543210")
    @JsonProperty("mobileNumber")
    @JsonAlias({"mobile", "mobile_number", "phone"})
    private String mobileNumber;

    @Schema(description = "Citizen Aadhaar number", example = "XXXXXXXX1234")
    @JsonProperty("aadhaarNumber")
    @JsonAlias({"aadhaar", "aadhaar_number", "aadhaarNo"})
    private String aadhaarNumber;

    @Schema(description = "UIDAI OTP transaction ID", example = "txn-1234-abcd")
    @JsonProperty("txnId")
    @JsonAlias({"referenceId", "refId", "transactionId", "txn_id"})
    private String txnId;

    @Schema(description = "UIDAI OTP submitted by citizen for e-KYC consent", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("otp")
    @NotBlank(message = "otp is mandatory to authenticate Aadhaar re-sync consent")
    private String otp;

    @Schema(description = "Nested citizen profile payload supporting standard UPYOG request contracts")
    @JsonProperty("user")
    @JsonAlias({"profile", "User"})
    private Map<String, Object> user;

    @JsonSetter("user")
    @JsonAlias({"profile", "User"})
    public void setUser(Map<String, Object> userMap) {
        this.user = userMap;
        unpackUserIfPresent();
    }

    public void unpackUserIfPresent() {
        if (this.user != null) {
            Map<String, Object> userMap = this.user;
            if (StringUtils.isBlank(this.uuid)) {
                if (userMap.get("uuid") != null) this.uuid = String.valueOf(userMap.get("uuid"));
                else if (userMap.get("userUuid") != null) this.uuid = String.valueOf(userMap.get("userUuid"));
                else if (userMap.get("user_uuid") != null) this.uuid = String.valueOf(userMap.get("user_uuid"));
            }
            if (this.id == null && userMap.get("id") != null) {
                try {
                    this.id = Long.parseLong(String.valueOf(userMap.get("id")));
                } catch (Exception ignored) {}
            }
            if (StringUtils.isBlank(this.tenantId) && userMap.get("tenantId") != null) {
                this.tenantId = String.valueOf(userMap.get("tenantId"));
            }
            if (StringUtils.isBlank(this.mobileNumber) && userMap.get("mobileNumber") != null) {
                this.mobileNumber = String.valueOf(userMap.get("mobileNumber"));
            }
            if (StringUtils.isBlank(this.aadhaarNumber) && userMap.get("aadhaarNumber") != null) {
                this.aadhaarNumber = String.valueOf(userMap.get("aadhaarNumber"));
            }
            if (StringUtils.isBlank(this.txnId)) {
                if (userMap.get("txnId") != null) this.txnId = String.valueOf(userMap.get("txnId"));
                else if (userMap.get("referenceId") != null) this.txnId = String.valueOf(userMap.get("referenceId"));
                else if (userMap.get("refId") != null) this.txnId = String.valueOf(userMap.get("refId"));
            }
            if (StringUtils.isBlank(this.otp) && userMap.get("otp") != null) {
                this.otp = String.valueOf(userMap.get("otp"));
            }
        }

        // RequestInfo fallback
        if (StringUtils.isBlank(this.uuid) && this.requestInfo != null && this.requestInfo.getUserInfo() != null) {
            this.uuid = this.requestInfo.getUserInfo().getUuid();
        }
        if (this.id == null && this.requestInfo != null && this.requestInfo.getUserInfo() != null) {
            this.id = this.requestInfo.getUserInfo().getId();
        }
        if (StringUtils.isBlank(this.tenantId) && this.requestInfo != null && this.requestInfo.getUserInfo() != null) {
            this.tenantId = this.requestInfo.getUserInfo().getTenantId();
        }
    }

    public String getUuid() {
        if (StringUtils.isNotBlank(this.uuid)) {
            return this.uuid;
        }
        if (this.requestInfo != null && this.requestInfo.getUserInfo() != null && StringUtils.isNotBlank(this.requestInfo.getUserInfo().getUuid())) {
            return this.requestInfo.getUserInfo().getUuid();
        }
        return this.uuid;
    }

    public Long getId() {
        if (this.id != null) {
            return this.id;
        }
        if (this.requestInfo != null && this.requestInfo.getUserInfo() != null && this.requestInfo.getUserInfo().getId() != null) {
            return this.requestInfo.getUserInfo().getId();
        }
        return this.id;
    }

    public String getTenantId() {
        if (StringUtils.isNotBlank(this.tenantId)) {
            return this.tenantId;
        }
        if (this.requestInfo != null && this.requestInfo.getUserInfo() != null && StringUtils.isNotBlank(this.requestInfo.getUserInfo().getTenantId())) {
            return this.requestInfo.getUserInfo().getTenantId();
        }
        return this.tenantId;
    }
}
