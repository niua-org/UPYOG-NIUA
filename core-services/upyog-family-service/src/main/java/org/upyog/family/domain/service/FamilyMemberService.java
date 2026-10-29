package org.upyog.family.domain.service;

import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.LeaveFamilyRequest;

/**
 * Service contract governing individual family member lifecycle operations (Add, Join, Leave) in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyMemberService {

    /**
     * Adds a new verified member to an existing family unit with Aadhaar OTP consent.
     *
     * @param request member addition payload with consent token
     * @return updated Family aggregate
     */
    Family addMember(AddMemberRequest request);

    /**
     * Processes an independent citizen request to join an existing family upon HoF authorization.
     *
     * @param request join family request with HoF authorization
     * @return updated Family aggregate
     */
    Family joinFamily(JoinFamilyRequest request);

    /**
     * Processes a member exit from a family unit due to marriage, demise, or relocation.
     *
     * @param request leave family request
     * @return updated Family aggregate
     */
    Family leaveFamily(LeaveFamilyRequest request);
}
