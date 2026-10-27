package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.upyog.family.domain.model.LineageActionType;

/**
 * Lineage audit model tracking household evolution (Split and Merge events) in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyLineage {

    @JsonProperty("id")
    private String id;

    @JsonProperty("parentFamilyId")
    private String parentFamilyId;

    @JsonProperty("childFamilyId")
    private String childFamilyId;

    @JsonProperty("actionType")
    private LineageActionType actionType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("createdTime")
    private Long createdTime;
}
