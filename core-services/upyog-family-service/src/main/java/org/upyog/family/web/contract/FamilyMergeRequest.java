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

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for merging two existing families into a unified household in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMergeRequest {

    @NotNull(message = "RequestInfo is mandatory")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "survivingFamilyId is mandatory")
    @JsonProperty("survivingFamilyId")
    private String survivingFamilyId;

    @NotBlank(message = "mergingFamilyId is mandatory")
    @JsonProperty("mergingFamilyId")
    private String mergingFamilyId;

    @NotBlank(message = "commonHeadUserUuid is mandatory")
    @JsonProperty("commonHeadUserUuid")
    private String commonHeadUserUuid;

    @JsonProperty("reason")
    private String reason;

    @Valid
    @JsonProperty("documents")
    @Builder.Default
    private List<FamilyDocument> documents = new ArrayList<>();
}
