package org.egov.user.domain.service.asset;

import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;

/**
 * Service orchestrating transactional synchronization of citizen demographics,
 * attached documents, and asset declarations (properties, income, vehicles) into UPYOG core registry.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface MemberAssetService {

    /**
     * Synchronizes demographics and declared assets for each family member in the request payload.
     *
     * @param request member asset synchronization request containing RequestInfo and member payloads
     * @return response summarizing synchronization results for each citizen
     */
    MemberAssetSyncResponse syncMemberAssets(MemberAssetSyncRequest request);
}
