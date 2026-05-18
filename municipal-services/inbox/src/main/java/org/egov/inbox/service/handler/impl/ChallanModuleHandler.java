package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.ChallanInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.ChallanConstants.CHALLAN_GENERATION;
import static org.egov.inbox.util.TLConstants.*;

@Slf4j
@Service
public class ChallanModuleHandler implements ModuleInboxHandler {

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
        String applNosParam = ctx.getSrvMap() != null ? ctx.getSrvMap().get("applNosParam") : null;
        if (applNosParam != null) {
            ctx.getCriteria().getModuleSearchCriteria().put(applNosParam, ids);
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
        return "applNosParam";
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
    }
}
