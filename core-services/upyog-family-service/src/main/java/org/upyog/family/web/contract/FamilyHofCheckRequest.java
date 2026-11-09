package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request envelope for checking whether the authenticated caller is the Head of Family (HoF).
 *
 * <p>Contains the caller's {@link RequestInfo} (from which the logged-in user's UUID is extracted),
 * the target {@code familyId}, and an optional {@code tenantId}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyHofCheckRequest {

    /**
     * UPYOG Request context envelope holding caller authentication details.
     */
    @NotNull(message = "RequestInfo is mandatory")
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    /**
     * Unique family identifier to verify against.
     */
    @NotBlank(message = "familyId is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    /**
     * Optional tenant identifier scoping the jurisdiction.
     */
    @JsonProperty("tenantId")
    private String tenantId;
}
