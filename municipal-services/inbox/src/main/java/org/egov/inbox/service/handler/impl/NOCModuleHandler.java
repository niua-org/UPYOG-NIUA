package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.NOCInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.NocConstants.*;

@Slf4j
@Service
public class NOCModuleHandler implements ModuleInboxHandler {

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
        ctx.getCriteria().getModuleSearchCriteria().put(NOC_APPLICATION_NUMBER_PARAM, ids);
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
