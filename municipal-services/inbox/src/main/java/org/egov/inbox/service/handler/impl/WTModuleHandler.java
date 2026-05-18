package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.WTInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.RequestServiceConstants.*;

@Slf4j
@Service
public class WTModuleHandler implements ModuleInboxHandler {

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
        ctx.getCriteria().getModuleSearchCriteria().put(BOOKING_NO_PARAM, ids);
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
