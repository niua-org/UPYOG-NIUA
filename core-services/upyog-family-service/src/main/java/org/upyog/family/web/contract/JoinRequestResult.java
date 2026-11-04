package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Outcome details for a citizen joining an existing Family Unit in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResult {

    @JsonProperty("familyId")
    private String familyId;

    @JsonProperty("userUuid")
    private String userUuid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("requestedOn")
    private Long requestedOn;
}
