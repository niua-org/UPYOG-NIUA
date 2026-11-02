package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.upyog.family.domain.model.FamilyStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Core Family registry aggregate model in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Family {

    @JsonProperty("id")
    private String id;

    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @NotBlank(message = "headUserUuid is mandatory")
    @JsonProperty("headUserUuid")
    private String headUserUuid;

    @JsonProperty("category")
    private String category;

    @JsonProperty("status")
    private FamilyStatus status;

    @JsonProperty("additionalDetails")
    private Object additionalDetails;

    @Valid
    @NotEmpty(message = "Family must contain at least one member (Head of Family)")
    @JsonProperty("members")
    @Builder.Default
    private List<FamilyMember> members = new ArrayList<>();

    @Valid
    @JsonProperty("documents")
    @Builder.Default
    private List<FamilyDocument> documents = new ArrayList<>();

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
