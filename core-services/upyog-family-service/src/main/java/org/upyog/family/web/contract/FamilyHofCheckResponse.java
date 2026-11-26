package org.upyog.family.web.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.contract.response.ResponseInfo;

/**
 * Response envelope indicating whether the authenticated caller is the Head of Family (HoF).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyHofCheckResponse {

    /**
     * Standard UPYOG API response header.
     */
    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    /**
     * The verified family identifier.
     */
    @JsonProperty("familyId")
    private String familyId;

    /**
     * The unique identifier (UUID) of the caller making the request.
     */
    @JsonProperty("userUuid")
    private String userUuid;

    /**
     * Flag indicating whether the caller is the designated Head of Family.
     */
    @JsonProperty("isHof")
    private Boolean isHof;

    /**
     * The unique identifier (UUID) of the current Head of Family.
     */
    @JsonProperty("headUserUuid")
    private String headUserUuid;
}
