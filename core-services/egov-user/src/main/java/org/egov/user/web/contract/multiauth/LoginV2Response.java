package org.egov.user.web.contract.multiauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.web.contract.auth.User;

import java.io.Serializable;
import java.util.List;

/**
 * Standard token & profile response payload returned upon successful loginv2 authentication.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Schema(description = "Standard authentication response containing OAuth tokens and authenticated citizen details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoginV2Response implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Standard UPYOG ResponseInfo contract")
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @Schema(description = "OAuth2 access token for authenticating subsequent API requests", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "OAuth2 refresh token for renewing expired tokens", example = "f9e8d7c6-b5a4-3210-fedc-ba0987654321")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @Schema(description = "Token type standard prefix", example = "Bearer")
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token validity in seconds", example = "3600")
    @JsonProperty("expires_in")
    private Integer expiresIn;

    @Schema(description = "Authorized OAuth scopes", example = "read write")
    @JsonProperty("scope")
    private String scope;

    @Schema(description = "Authenticated user profile details")
    @JsonProperty("UserRequest")
    private User userRequest;

    @Schema(description = "Channel through which citizen was authenticated", example = "MOBILE_OTP")
    @JsonProperty("authenticatedVia")
    private AuthType authenticatedVia;

    @Schema(description = "Flag indicating whether this is a newly registered user", example = "false")
    @JsonProperty("isNewUser")
    private Boolean isNewUser;

    @Schema(description = "Flag indicating whether multiple citizen profiles were found for this mobile number requiring selection", example = "false")
    @JsonProperty("multipleUsersFound")
    private Boolean multipleUsersFound;

    @Schema(description = "Flag indicating whether citizen profile has all mandatory Section 8 fields completed", example = "true")
    @JsonProperty("isProfileCompleted")
    private Boolean isProfileCompleted;

    @Schema(description = "List of citizen user accounts linked to this mobile number, returned when multipleUsersFound is true")
    @JsonProperty("userAccounts")
    private List<UserAccountSummary> userAccounts;
}
