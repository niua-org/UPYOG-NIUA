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
import org.upyog.family.domain.model.ExitReason;

/**
 * Request payload for a member exiting an existing Family Unit in UPYOG.
 *
 * <p>Valid exit reasons: MARRIAGE, DEMISE, RELOCATION, OTHER.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveFamilyRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "Member userUuid is mandatory")
    @JsonProperty("memberUserUuid")
    private String memberUserUuid;

    @NotNull(message = "Exit reason is mandatory")
    @JsonProperty("exitReason")
    private ExitReason exitReason;

    @JsonProperty("exitDate")
    private Long exitDate;

    @JsonProperty("notes")
    private String notes;
}
