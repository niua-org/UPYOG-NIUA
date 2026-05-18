package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.TLInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.TLConstants.*;
import static org.egov.inbox.util.BpaConstants.BPAREG;

@Slf4j
@Service
public class TLModuleHandler implements ModuleInboxHandler {

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
        ctx.getCriteria().getModuleSearchCriteria().put(APPLICATION_NUMBER_PARAM, ids);
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
