package org.upyog.family.domain.service;

import org.upyog.family.web.contract.AssignHofRequest;
import org.upyog.family.web.contract.Family;

/**
 * Service contract governing Head of Family (HoF) succession and role transfers in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyHeadService {

    /**
     * Transfers or assigns the Head of Family role to an existing active family member.
     *
     * @param request HoF assignment payload
     * @return updated Family aggregate
     */
    Family assignHeadOfFamily(AssignHofRequest request);
}
