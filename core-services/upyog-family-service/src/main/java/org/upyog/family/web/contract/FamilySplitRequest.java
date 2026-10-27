package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for splitting an existing family into a new household branch in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilySplitRequest {

    @NotNull(message = "RequestInfo is mandatory")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "parentFamilyId is mandatory")
    @JsonProperty("parentFamilyId")
    private String parentFamilyId;

    @NotBlank(message = "newFamilyHeadUserUuid is mandatory")
    @JsonProperty("newFamilyHeadUserUuid")
    private String newFamilyHeadUserUuid;

    @NotEmpty(message = "splittingMemberUserUuids cannot be empty")
    @JsonProperty("splittingMemberUserUuids")
    @Builder.Default
    private List<String> splittingMemberUserUuids = new ArrayList<>();

    @JsonProperty("reason")
    private String reason;

    @Valid
    @JsonProperty("documents")
    @Builder.Default
    private List<FamilyDocument> documents = new ArrayList<>();
}
