package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for adding a new member to an existing Family Unit in UPYOG.
 *
 * <p>Supports real-time Aadhaar OTP consent verification for adult members.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @Valid
    @NotNull(message = "Family member details cannot be null")
    @JsonProperty("member")
    private FamilyMember member;

    @JsonProperty("consentToken")
    private String consentToken;
}
