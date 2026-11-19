package org.egov.user.web.contract.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Encapsulates household lookup and Head of Family (HoF) evaluation results
 * retrieved from {@code upyog-family-service}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyHofStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Flag indicating whether an active family was found for the target user.
     */
    private boolean familyFound;

    /**
     * Unique business identifier of the active family (e.g. {@code FAM-2026-0001}).
     */
    private String familyId;

    /**
     * Flag indicating whether the target user is the designated Head of Family (HoF).
     */
    private boolean isHof;

    /**
     * User UUID of the active household head recorded on the family aggregate.
     */
    private String headUserUuid;
}
