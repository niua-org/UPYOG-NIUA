package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.egov.common.contract.request.RequestInfo;

/**
 * Standard UPYOG request envelope for family creation and mutation operations.
 *
 * <p>Wraps the target {@link Family} entity alongside platform {@link RequestInfo} metadata
 * containing caller authentication tokens, user roles, and trace headers.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRequest {

    /**
     * Standard platform request metadata envelope containing caller credentials and transaction headers.
     */
    @NotNull(message = "RequestInfo is mandatory")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * The core family aggregate payload being created or mutated.
     */
    @Valid
    @NotNull(message = "Family object is mandatory")
    @JsonProperty("Family")
    private Family family;
}
