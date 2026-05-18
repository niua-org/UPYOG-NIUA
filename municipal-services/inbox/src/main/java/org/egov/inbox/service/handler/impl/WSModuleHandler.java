package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.service.BillingAmendmentInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.egov.inbox.util.BSConstants.*;

/**
 * Handles WS billing amendment (bsWs-service) and SW billing amendment (bsSw-service).
 *
 * bsFlag convention (mirrors the original InboxOrchestrator logic):
 *   0 = not a billing module
 *   1 = WS billing  (bsWs-service  → internal moduleName WS)
 *   2 = SW billing  (bsSw-service  → internal moduleName SW)
 *
 * bsFlag is stored in InboxContext so InboxAssembler can read it if needed.
 */

@Slf4j
@Service
public class WSModuleHandler implements ModuleInboxHandler {

    @Autowired
    private BillingAmendmentInboxFilterService billService;

    @Autowired
    private InboxConfiguration config;

    // bsFlag: 1 = WS billing, 2 = SW billing (matches original InboxOrchestrator convention)
    public static final int BS_FLAG_WS = 1;
    public static final int BS_FLAG_SW = 2;

    @Override
    public boolean supports(String moduleName) {
        return BS_WS.equalsIgnoreCase(moduleName) || BS_SW.equalsIgnoreCase(moduleName);
    }

    @Override
    public boolean workflowTotalCount() {
        return false;
    }

    /**
     * Returns bsFlag for the given raw module name.
     *   bsWs-service → 1 (BS_FLAG_WS)
     *   bsSw-service → 2 (BS_FLAG_SW)
     *   anything else → 0
     */
    public int resolveBsFlag(String moduleName) {
        if (BS_WS.equalsIgnoreCase(moduleName)) return BS_FLAG_WS;
        if (BS_SW.equalsIgnoreCase(moduleName)) return BS_FLAG_SW;
        return 0;
    }

    /**
     * Remaps bsWs-service → WS  /  bsSw-service → SW.
     */
    public String resolveInternalModuleName(String moduleName) {
        if (BS_WS.equalsIgnoreCase(moduleName)) return BS_WS_MODULENAME;
        if (BS_SW.equalsIgnoreCase(moduleName)) return BS_SW_MODULENAME;
        return moduleName;
    }

    /**
     * Reverse map: WS → bsWs-service  /  SW → bsSw-service.
     * Used to temporarily restore processCriteria.moduleName before
     * calling the billing searcher (which checks equalsIgnoreCase(BS_WS)).
     */
    public String resolveOriginalModuleName(String internalModuleName) {
        if (BS_WS_MODULENAME.equals(internalModuleName)) return BS_WS;
        if (BS_SW_MODULENAME.equals(internalModuleName)) return BS_SW;
        return internalModuleName;
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        ProcessInstanceSearchCriteria processCriteria =
                ctx.getCriteria().getProcessSearchCriteria();

        String saved = processCriteria.getModuleName();
        processCriteria.setModuleName(resolveOriginalModuleName(saved));

        int count = billService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());

        processCriteria.setModuleName(saved);
        return count;
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        ProcessInstanceSearchCriteria processCriteria =
                ctx.getCriteria().getProcessSearchCriteria();
        HashMap<String, Object> moduleSearchCriteria =
                ctx.getCriteria().getModuleSearchCriteria();

        String saved = processCriteria.getModuleName();

        // Set bsFlag on context so InboxAssembler knows WS(1) vs SW(2)
        ctx.setBsFlag(resolveBsFlag(resolveOriginalModuleName(saved)));

        processCriteria.setModuleName(resolveOriginalModuleName(saved));

        Map<String, List<String>> result = billService.fetchConsumerNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());

        processCriteria.setModuleName(saved);

        List<String> consumerCodes = result.get("consumerCodes");
        List<String> amendmentIds  = result.get("amendmentIds");

        if (!CollectionUtils.isEmpty(consumerCodes)) {
            String businessService = BS_WS_MODULENAME.equals(saved) ? "WS" : "SW";
            moduleSearchCriteria.put(BS_CONSUMER_NO_PARAM, consumerCodes);
            moduleSearchCriteria.put(BS_BUSINESS_SERVICE_PARAM, businessService);
            moduleSearchCriteria.put("isPropertyDetailsRequired", true);
            moduleSearchCriteria.remove(MOBILE_NUMBER_PARAM);
            moduleSearchCriteria.remove(ASSIGNEE_PARAM);
            moduleSearchCriteria.remove(LOCALITY_PARAM);
            moduleSearchCriteria.remove(OFFSET_PARAM);
            ctx.addBusinessKeys(amendmentIds);
        } else {
            ctx.setSearchResultEmpty(true);
        }
    }

    @Override
    public String getApplicationIdParamKey() {
        return BS_CONSUMER_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of();  // removals handled inside fetchApplicationIds
    }

    // Looks up bsServiceSearchMapping for WS/SW connection search (billing amendment)
    public Map<String, String> fetchServiceSearchMap(String businessServiceName) {
        StringBuilder appropriateKey = new StringBuilder();
        for (String key : config.getBsServiceSearchMapping().keySet()) {
            if (key.contains(businessServiceName)) {
                appropriateKey.append(key);
                break;
            }
        }
        if (ObjectUtils.isEmpty(appropriateKey))
            throw new CustomException("EG_INBOX_SEARCH_ERROR",
                    "Inbox service is not configured for the provided business services");
        return config.getBsServiceSearchMapping().get(appropriateKey.toString());
    }
}
