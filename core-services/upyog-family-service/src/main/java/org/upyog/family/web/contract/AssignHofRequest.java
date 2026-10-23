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
 * Request payload for transferring or assigning the Head of Family (HoF) role in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignHofRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "New Head User UUID is mandatory")
    @JsonProperty("newHeadUserUuid")
    private String newHeadUserUuid;

    @JsonProperty("reason")
    private String reason;
}
