package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation managing citizen profile demographics, attached identity documents,
 * and declared assets synchronization in the {@code egov-user} core registry.
 *
 * <p>Coordinates batch synchronization across family members, delegating each member to an isolated
 * transactional processor ({@link MemberAssetProcessor}) with {@code REQUIRES_NEW} propagation to prevent
 * partial database writes on failure.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class MemberAssetServiceImpl implements MemberAssetService {

    private final MemberAssetProcessor memberAssetProcessor;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting the isolated transactional {@link MemberAssetProcessor} and configuration.
     *
     * @param memberAssetProcessor per-member isolated transactional processor
     * @param userConfiguration   centralized user configuration
     */
    @Autowired
    public MemberAssetServiceImpl(MemberAssetProcessor memberAssetProcessor,
                                 @Autowired(required = false) UserConfiguration userConfiguration) {
        this.memberAssetProcessor = memberAssetProcessor;
        this.userConfiguration = userConfiguration;
    }

    public MemberAssetServiceImpl(MemberAssetProcessor memberAssetProcessor) {
        this(memberAssetProcessor, null);
    }

    /**
     * Backwards-compatible constructor for existing tests and components.
     *
     * @param userRepository     repository for querying and updating user demographic records
     * @param propertyRepository repository for persisting 3NF property assets and ownership
     * @param incomeRepository   repository for persisting citizen income declarations
     * @param vehicleRepository  repository for persisting declared vehicle assets
     * @param documentRepository repository for persisting user documents and verification statuses
     */
    public MemberAssetServiceImpl(UserRepository userRepository,
                                  UserPropertyRepository propertyRepository,
                                  UserIncomeRepository incomeRepository,
                                  UserVehicleRepository vehicleRepository,
                                  UserDocumentRepository documentRepository) {
        this(new MemberAssetProcessor(userRepository, propertyRepository, incomeRepository, vehicleRepository, documentRepository), null);
    }

    /**
     * Synchronizes demographics and declared assets for all members in the incoming sync request.
     *
     * <p>Each member is processed in an independent {@code REQUIRES_NEW} transaction. If any member fails,
     * their database writes are cleanly rolled back without affecting other members. Top-level status reflects
     * the aggregated result ({@code SUCCESS}, {@code PARTIAL_SUCCESS}, or {@code FAILED}) and internal errors
     * are sanitized before reaching API callers.</p>
     *
     * @param request the member asset sync request payload
     * @return synchronization response envelope detailing per-member results and overall status
     * @throws CustomException if request payload is null or caller is missing when required
     */
    @Override
    public MemberAssetSyncResponse syncMemberAssets(MemberAssetSyncRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, UserConstants.MSG_MEMBER_ASSET_SYNC_PAYLOAD_NULL);
        }

        // Fix for PR Comment #4 (Audit actor): Validate caller presence if configured to require caller
        if (userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getRequireCallerForSync())) {
            if (request.getRequestInfo() == null || request.getRequestInfo().getUserInfo() == null) {
                log.error("[MEMBER-ASSET-SERVICE] Caller userInfo in RequestInfo is mandatory but was not provided");
                throw new CustomException(UserConstants.ERROR_CALLER_INFO_REQUIRED, UserConstants.MSG_CALLER_INFO_REQUIRED);
            }
        }

        List<MemberAssetDetail> members = request.getNormalizedMembers();
        if (CollectionUtils.isEmpty(members)) {
            log.warn("[MEMBER-ASSET-SERVICE] Received empty member list for synchronization");
            return MemberAssetSyncResponse.builder()
                    .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                    .status(UserConstants.STATUS_SUCCESS)
                    .results(Collections.emptyList())
                    .build();
        }

        List<MemberAssetSyncResult> results = new ArrayList<>();

        int memberIndex = 0;
        for (MemberAssetDetail memberDetail : members) {
            memberIndex++;
            String userUuid = memberDetail != null ? memberDetail.resolveUserUuid() : null;
            if (StringUtils.isBlank(userUuid)) {
                // Security / PII protection: Log only the member payload index, never log the entire payload or PII
                log.error("[MEMBER-ASSET-SERVICE] Missing userUuid for member at index {}", memberIndex);
                results.add(MemberAssetSyncResult.builder()
                        .userUuid(UserConstants.UNKNOWN_USER_UUID)
                        .status(UserConstants.STATUS_FAILED)
                        .message(UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED)
                        .build());
                continue;
            }

            try {
                // Delegate to isolated transaction processor (REQUIRES_NEW per member)
                MemberAssetSyncResult result = memberAssetProcessor.processSingleMember(memberDetail, request);
                results.add(result);
            } catch (Exception e) {
                log.error("[MEMBER-ASSET-SERVICE] Synchronization failed for user UUID {}: {}", userUuid, e.getMessage(), e);
                // Sanitize error message to prevent leaking raw SQL/internal exception details
                String safeMessage = sanitizeErrorMessage(e, userUuid);
                results.add(MemberAssetSyncResult.builder()
                        .userUuid(userUuid)
                        .status(UserConstants.STATUS_FAILED)
                        .message(safeMessage)
                        .build());
            }
        }

        // Determine accurate top-level status based on member results: SUCCESS, PARTIAL_SUCCESS, or FAILED
        long successCount = results.stream().filter(r -> UserConstants.STATUS_SUCCESS.equalsIgnoreCase(r.getStatus())).count();
        long failedCount = results.stream().filter(r -> UserConstants.STATUS_FAILED.equalsIgnoreCase(r.getStatus())).count();

        String overallStatus;
        if (failedCount == 0) {
            overallStatus = UserConstants.STATUS_SUCCESS;
        } else if (successCount == 0) {
            overallStatus = UserConstants.STATUS_FAILED;
        } else {
            overallStatus = UserConstants.STATUS_PARTIAL_SUCCESS;
        }

        log.info("[MEMBER-ASSET-SERVICE] Synchronization completed with overall status: {}. Total: {}, Success: {}, Failed: {}",
                overallStatus, results.size(), successCount, failedCount);

        return MemberAssetSyncResponse.builder()
                .responseInfo(ResponseInfo.builder().status(overallStatus).build())
                .status(overallStatus)
                .results(results)
                .build();
    }

    /**
     * Sanitizes exception details to prevent leaking internal SQL syntax or database metadata to clients.
     *
     * @param e        thrown exception
     * @param userUuid citizen user UUID
     * @return safe, human-readable error description
     */
    private String sanitizeErrorMessage(Exception e, String userUuid) {
        if (e instanceof CustomException ce) {
            return StringUtils.isNotBlank(ce.getMessage()) ? ce.getMessage() : ce.getCode();
        }
        if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return "Synchronization failed for citizen " + userUuid + " due to an internal processing error";
    }
}
