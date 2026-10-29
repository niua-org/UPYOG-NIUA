package org.egov.user.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserServiceConstants;

import java.util.Map;

/**
 * Request DTO for initializing a lightweight citizen profile.
 */
@Schema(description = "Request payload for initializing a lightweight citizen profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInitRequest {

    @Schema(description = "Standard UPYOG RequestInfo contract")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @Schema(description = "Tenant identifier", example = "pg.citya", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "tenantId is mandatory for profile initialization")
    @Pattern(regexp = UserServiceConstants.PATTERN_TENANT, message = "Invalid tenantId format")
    @Size(max = 50)
    private String tenantId;

    @Schema(description = "Citizen 10-digit mobile number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "mobileNumber is mandatory for citizen onboarding")
    @Pattern(regexp = UserServiceConstants.PATTERN_MOBILE, message = "Invalid mobile number format")
    private String mobileNumber;

    @Schema(description = "Citizen full name", example = "Aarav Sharma")
    @Size(max = 100)
    private String name;

    @Schema(description = "Citizen email address", example = "aarav.sharma@example.com")
    @Email(message = "Invalid emailId format")
    @Size(max = 128)
    private String emailId;

    @Schema(description = "One-time password for mobile verification", example = "123456")
    private String otp;

    @Schema(description = "OTP reference ID returned during OTP generation", example = "otp-ref-98765")
    private String otpReference;

    @Schema(description = "Nested citizen profile payload supporting standard UPYOG request contracts")
    @JsonProperty("user")
    @JsonAlias({"profile", "User"})
    private Map<String, Object> user;

    public String getTenantId() {
        if (this.tenantId != null && !this.tenantId.trim().isEmpty()) {
            return this.tenantId;
        }
        if (this.user != null && this.user.get("tenantId") != null) {
            return String.valueOf(this.user.get("tenantId"));
        }
        return this.tenantId;
    }

    public String getMobileNumber() {
        if (this.mobileNumber != null && !this.mobileNumber.trim().isEmpty()) {
            return this.mobileNumber;
        }
        if (this.user != null && this.user.get("mobileNumber") != null) {
            return String.valueOf(this.user.get("mobileNumber"));
        }
        return this.mobileNumber;
    }

    @JsonProperty("user")
    @JsonAlias({"profile", "User"})
    public Map<String, Object> getUser() {
        return this.user;
    }

    @JsonSetter("user")
    @JsonAlias({"profile", "User"})
    public void setUser(Map<String, Object> userMap) {
        this.user = userMap;
        unpackUserIfPresent();
    }

    public void unpackUserIfPresent() {
        if (this.user == null) return;
        Map<String, Object> userMap = this.user;
        if ((this.tenantId == null || this.tenantId.trim().isEmpty()) && userMap.get("tenantId") != null) {
            this.tenantId = String.valueOf(userMap.get("tenantId"));
        }
        if ((this.mobileNumber == null || this.mobileNumber.trim().isEmpty()) && userMap.get("mobileNumber") != null) {
            this.mobileNumber = String.valueOf(userMap.get("mobileNumber"));
        }
        if ((this.name == null || this.name.trim().isEmpty()) && userMap.get("name") != null) {
            this.name = String.valueOf(userMap.get("name"));
        }
        if ((this.emailId == null || this.emailId.trim().isEmpty()) && userMap.get("emailId") != null) {
            this.emailId = String.valueOf(userMap.get("emailId"));
        }
        if ((this.otp == null || this.otp.trim().isEmpty()) && userMap.get("otp") != null) {
            this.otp = String.valueOf(userMap.get("otp"));
        }
        if ((this.otpReference == null || this.otpReference.trim().isEmpty()) && userMap.get("otpReference") != null) {
            this.otpReference = String.valueOf(userMap.get("otpReference"));
        }
    }
}
