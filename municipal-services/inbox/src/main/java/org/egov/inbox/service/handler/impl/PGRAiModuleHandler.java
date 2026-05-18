package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.PGRAiInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.egov.inbox.util.PGRAiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class PGRAiModuleHandler implements ModuleInboxHandler {

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
        return List.of();
    }
}
