package org.upyog.family.web.contract.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Household family aggregate received from external State Family Registry (e.g., Haryana PPP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateFamily {

    @JsonProperty("stateCode")
    private String stateCode;

    @NotBlank(message = "stateFamilyId is mandatory from State Registry")
    @JsonProperty("stateFamilyId")
    private String stateFamilyId;

    @JsonProperty("familyName")
    private String familyName;

    @NotBlank(message = "tenantId is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("category")
    private String category;

    @JsonProperty("address")
    private StateAddress address;

    @Valid
    @NotEmpty(message = "State Family must contain at least one member")
    @JsonProperty("members")
    @Builder.Default
    private List<StateFamilyMember> members = new ArrayList<>();
}
