package org.egov.inbox.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.*;
import org.egov.inbox.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.TLConstants.APPLICATION_NUMBER_PARAM;
import static org.egov.inbox.util.TLConstants.LOCALITY_PARAM;
import static org.egov.inbox.util.TLConstants.OFFSET_PARAM;
import static org.egov.inbox.util.TLConstants.STATUS_PARAM;
import static org.egov.inbox.util.TLConstants.MOBILE_NUMBER_PARAM;
import static org.egov.inbox.util.TLConstants.TL;
import static org.egov.inbox.util.BpaConstants.BPA;
import static org.egov.inbox.util.BpaConstants.BPAREG;
import static org.egov.inbox.util.BpaConstants.BPA_APPLICATION_NUMBER_PARAM;
import static org.egov.inbox.util.ChallanConstants.CHALLAN_GENERATION;
import static org.egov.inbox.util.CommunityHallConstants.CHB;
import static org.egov.inbox.util.CommunityHallConstants.CHB_BOOKING_NO_PARAM;
import static org.egov.inbox.util.CNDServiceConstants.CND;
import static org.egov.inbox.util.CNDServiceConstants.APPLICATION_NO_PARAM;
import static org.egov.inbox.util.StreetVendingConstants.APPLICATION_STATUS;
import static org.egov.inbox.util.StreetVendingConstants.SV_APPLICATION_NUMBER_PARAM;
import static org.egov.inbox.util.StreetVendingConstants.SV_SERVICES;
import static org.egov.inbox.util.EwasteConstants.EWASTE;
import static org.egov.inbox.util.EwasteConstants.REQUEST_IDS;
import static org.egov.inbox.util.NdcConstants.NDC_MODULE;
import static org.egov.inbox.util.NdcConstants.NDC_APPLICATION_NO_PARAM;
import static org.egov.inbox.util.PTConstants.PT;
import static org.egov.inbox.util.PTConstants.ACKNOWLEDGEMENT_IDS_PARAM;
import static org.egov.inbox.util.PTRConstants.PTR;
import static org.egov.inbox.util.RequestServiceConstants.BOOKING_NO_PARAM;
import static org.egov.inbox.util.RequestServiceConstants.REQUEST_SERVICE_WATER_TANKER;
import static org.egov.inbox.util.RequestServiceConstants.REQUEST_SERVICE_MOBILE_TOILET;
import static org.egov.inbox.util.RequestServiceConstants.REQUEST_SERVICE_TREE_PRUNING;
import static org.egov.inbox.util.AssetConstants.ASSET;
import static org.egov.inbox.util.NocConstants.NOC;
import static org.egov.inbox.util.NocConstants.NOC_APPLICATION_NUMBER_PARAM;

// ─────────────────────────────────────────────────────────────────────────────
// TL Handler — Trade License & BPAREG
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class TLModuleHandler implements ModuleInboxHandler {

    @Autowired
    private TLInboxFilterService tlService;

    @Override
    public boolean supports(String moduleName) {
        return TL.equals(moduleName) || BPAREG.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = tlService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(APPLICATION_NUMBER_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return tlService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return APPLICATION_NUMBER_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BPA Handler — Building Plan Approval
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class BPAModuleHandler implements ModuleInboxHandler {

    @Autowired
    private BPAInboxFilterService bpaService;

    @Override
    public boolean supports(String moduleName) {
        return BPA.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = bpaService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(BPA_APPLICATION_NUMBER_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return bpaService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return BPA_APPLICATION_NUMBER_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, MOBILE_NUMBER_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PT Handler — Property Tax
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class PTModuleHandler implements ModuleInboxHandler {

    @Autowired
    private PtInboxFilterService ptService;

    @Override
    public boolean supports(String moduleName) {
        return PT.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ptService.fetchAcknowledgementIdsFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(ACKNOWLEDGEMENT_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return ptService.fetchAcknowledgementIdsCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return ACKNOWLEDGEMENT_IDS_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        // locality and offset are removed inside fetchApplicationIds directly
        return List.of(LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PTR Handler — Pet Registration
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class PTRModuleHandler implements ModuleInboxHandler {

    @Autowired
    private PtrInboxFilterService ptrService;

    @Override
    public boolean supports(String moduleName) {
        return PTR.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ptrService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(ACKNOWLEDGEMENT_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return ACKNOWLEDGEMENT_IDS_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NOC Handler — No Objection Certificate
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class NOCModuleHandler implements ModuleInboxHandler {

    @Autowired
    private NOCInboxFilterService nocService;

    @Override
    public boolean supports(String moduleName) {
        return NOC.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = nocService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(NOC_APPLICATION_NUMBER_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return nocService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return NOC_APPLICATION_NUMBER_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, MOBILE_NUMBER_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHB Handler — Community Hall Booking
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class CHBModuleHandler implements ModuleInboxHandler {

    @Autowired
    private CommunityHallInboxFilterService chbService;

    @Override
    public boolean supports(String moduleName) {
        return CHB.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = chbService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(CHB_BOOKING_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return CHB_BOOKING_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NDC Handler — No Dues Certificate
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class NDCModuleHandler implements ModuleInboxHandler {

    @Autowired
    private NDCInboxFilterService ndcService;

    @Override
    public boolean supports(String moduleName) {
        return NDC_MODULE.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ndcService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(NDC_APPLICATION_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return ndcService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return NDC_APPLICATION_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CND Handler — Construction & Demolition Waste
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class CNDModuleHandler implements ModuleInboxHandler {

    @Autowired
    private CNDInboxFilterService cndService;

    @Override
    public boolean supports(String moduleName) {
        return CND.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = cndService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(APPLICATION_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);

        // CND uses APPLICATION_STATUS key to set STATUS_PARAM
        if (ctx.getCriteria().getModuleSearchCriteria()
                .containsKey(APPLICATION_STATUS)) {
            ctx.getCriteria().getModuleSearchCriteria().put(
                    STATUS_PARAM,
                    ctx.getCriteria().getModuleSearchCriteria()
                            .get(APPLICATION_STATUS));
        }
    }

    @Override
    public String getApplicationIdParamKey() {
        return APPLICATION_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EWASTE Handler — Electronic Waste
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class EWASTEModuleHandler implements ModuleInboxHandler {

    @Autowired
    private EwasteInboxFilterService ewasteService;

    @Override
    public boolean supports(String moduleName) {
        return EWASTE.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ewasteService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(REQUEST_IDS, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return REQUEST_IDS;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ASSET Handler — Asset Management
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class ASSETModuleHandler implements ModuleInboxHandler {

    @Autowired
    private AssetInboxFilterService assetService;

    @Override
    public boolean supports(String moduleName) {
        return ASSET.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = assetService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(ACKNOWLEDGEMENT_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return ACKNOWLEDGEMENT_IDS_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        // Asset does not remove locality/offset per original logic
        return List.of();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SV Handler — Street Vending
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class SVModuleHandler implements ModuleInboxHandler {

    @Autowired
    private StreetVendingInboxFilterService svService;

    @Override
    public boolean supports(String moduleName) {
        return SV_SERVICES.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = svService.fetchApplicationIdsFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(SV_APPLICATION_NUMBER_PARAM, ids);
        ctx.addBusinessKeys(ids);

        // SV uses APPLICATION_STATUS key to set STATUS_PARAM
        if (ctx.getCriteria().getModuleSearchCriteria()
                .containsKey(APPLICATION_STATUS)) {
            ctx.getCriteria().getModuleSearchCriteria().put(
                    STATUS_PARAM,
                    ctx.getCriteria().getModuleSearchCriteria()
                            .get(APPLICATION_STATUS));
        }
    }

    @Override
    public String getApplicationIdParamKey() {
        return SV_APPLICATION_NUMBER_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(LOCALITY_PARAM, OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WT Handler — Water Tanker
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class WTModuleHandler implements ModuleInboxHandler {

    @Autowired
    private WTInboxFilterService wtService;

    @Override
    public boolean supports(String moduleName) {
        return REQUEST_SERVICE_WATER_TANKER.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = wtService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(BOOKING_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return BOOKING_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(LOCALITY_PARAM, OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MT Handler — Mobile Toilet
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class MTModuleHandler implements ModuleInboxHandler {

    @Autowired
    private MTInboxFilterService mtService;

    @Override
    public boolean supports(String moduleName) {
        return REQUEST_SERVICE_MOBILE_TOILET.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = mtService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(BOOKING_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return BOOKING_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(LOCALITY_PARAM, OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TP Handler — Tree Pruning
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class TPModuleHandler implements ModuleInboxHandler {

    @Autowired
    private TPInboxFilterService tpService;

    @Override
    public boolean supports(String moduleName) {
        return REQUEST_SERVICE_TREE_PRUNING.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = tpService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(BOOKING_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return BOOKING_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(LOCALITY_PARAM, OFFSET_PARAM, STATUS_PARAM);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PGR AI Handler — PGR Artificial Intelligence
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class PGRAiModuleHandler implements ModuleInboxHandler {

    @Autowired
    private PGRAiInboxFilterService pgrAiService;

    @Override
    public boolean supports(String moduleName) {
        return PGRAiConstants.PGR_MODULE.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = pgrAiService.fetchApplicationIdsFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return pgrAiService.fetchApplicationIdsCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return "serviceRequestId";
    }

    @Override
    public List<String> paramsToRemove() {
        // PGR AI does not remove any params
        return List.of();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Challan Handler — Challan Generation
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
class ChallanModuleHandler implements ModuleInboxHandler {

    @Autowired
    private ChallanInboxFilterService challanService;

    @Override
    public boolean supports(String moduleName) {
        return CHALLAN_GENERATION.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = challanService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        // Read applNosParam dynamically from srvMap — same as original logic
        String applNosParam = ctx.getSrvMap() != null
                ? ctx.getSrvMap().get("applNosParam")
                : null;
        if (applNosParam != null) {
            ctx.getCriteria().getModuleSearchCriteria().put(applNosParam, ids);
            // Also remove applsStatusParam per original logic
            String applsStatusParam = ctx.getSrvMap().get("applsStatusParam");
            if (applsStatusParam != null) {
                ctx.removeModuleSearchCriteria(applsStatusParam);
            }
        }
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return challanService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return "applNosParam"; // actual key resolved from srvMap at runtime
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}