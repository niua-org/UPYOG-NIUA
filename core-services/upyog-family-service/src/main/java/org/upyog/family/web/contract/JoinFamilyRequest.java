package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;

/**
 * Request payload for a citizen requesting to join an existing Family Unit in UPYOG.
 *
 * <p>Requires authorization consent from the current Head of Family (HoF).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinFamilyRequest {

    @NotNull(message = "RequestInfo cannot be null")
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @NotBlank(message = "Family ID is mandatory")
    @JsonProperty("familyId")
    private String familyId;

    @NotBlank(message = "Tenant ID is mandatory")
    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("applicantUserUuid")
    private String applicantUserUuid;

    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Resolves and retrieves the applicant's user UUID from either applicantUserUuid or userUuid for backward compatibility.
     *
     * @return applicant user UUID, preferring applicantUserUuid if present, otherwise userUuid
     */
    public String getApplicantUserUuid() {
        if (StringUtils.isNotBlank(applicantUserUuid)) {
            return applicantUserUuid;
        }
        return userUuid;
    }

    @NotBlank(message = "Relationship with Head is mandatory")
    @JsonProperty("relationshipWithHead")
    private String relationshipWithHead;

    @NotBlank(message = "Transaction identifier (txnId) is mandatory")
    @JsonProperty("txnId")
    private String txnId;
}
