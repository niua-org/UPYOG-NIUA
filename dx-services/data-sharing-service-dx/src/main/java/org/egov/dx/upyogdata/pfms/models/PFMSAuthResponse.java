package org.egov.dx.upyogdata.pfms.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for PFMS authentication API.
 * Token from AccessToken field is used as Bearer in subsequent data push calls.
 */
@Data
@NoArgsConstructor
public class PFMSAuthResponse {

    @JsonProperty("AccessToken")
    private String accessToken;

    @JsonProperty("AccessTokenExpiry")
    private String accessTokenExpiry;

    @JsonProperty("RefreshToken")
    private String refreshToken;

    @JsonProperty("RefreshTokenExpiry")
    private String refreshTokenExpiry;

    @JsonProperty("UserName")
    private String userName;
}
