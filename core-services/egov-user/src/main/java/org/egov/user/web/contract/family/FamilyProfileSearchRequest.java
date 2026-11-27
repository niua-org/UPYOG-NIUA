package org.egov.user.web.contract.family;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for searching complete family unit details and member assets.
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
public class FamilyProfileSearchRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "familyId is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("tenantId")
    private String tenantId;
}
