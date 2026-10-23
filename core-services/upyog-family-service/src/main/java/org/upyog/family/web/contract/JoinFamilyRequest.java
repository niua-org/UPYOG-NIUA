package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for a citizen requesting to join an existing Family Unit in UPYOG.
 *
 * <p>Requires authorization consent from the current Head of Family (HoF).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinFamilyRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "Applicant userUuid is mandatory")
    @JsonProperty("applicantUserUuid")
    private String applicantUserUuid;

    @NotBlank(message = "Relationship with Head is mandatory")
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    @NotBlank(message = "HoF approval OTP or consent token is mandatory")
    @JsonProperty("hofConsentOtp")
    private String hofConsentOtp;
}
