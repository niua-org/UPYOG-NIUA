package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.EwasteInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.EwasteConstants.*;

@Slf4j
@Service
public class EWASTEModuleHandler implements ModuleInboxHandler {

    @Autowired
    private EwasteInboxFilterService ewasteService;

    @Override
    public boolean supports(String moduleName) {
        return EWASTE.equals(moduleName);
    }

    @Override
    public boolean workflowNearingSlaCount() {
        return false;
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ewasteService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria().put(REQUEST_IDS, ids);
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
