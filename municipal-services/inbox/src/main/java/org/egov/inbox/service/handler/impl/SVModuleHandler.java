package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.StreetVendingInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.StreetVendingConstants.*;
@Slf4j
@Service
public class SVModuleHandler implements ModuleInboxHandler {

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
        ctx.getCriteria().getModuleSearchCriteria().put(SV_APPLICATION_NUMBER_PARAM, ids);
        ctx.addBusinessKeys(ids);

        if (ctx.getCriteria().getModuleSearchCriteria().containsKey(APPLICATION_STATUS)) {
            ctx.getCriteria().getModuleSearchCriteria().put(
                    STATUS_PARAM,
                    ctx.getCriteria().getModuleSearchCriteria().get(APPLICATION_STATUS));
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
