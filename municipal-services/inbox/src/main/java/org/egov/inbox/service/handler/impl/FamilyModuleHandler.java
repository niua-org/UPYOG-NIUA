package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.FamilyInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.CommonConstants.*;
import static org.egov.inbox.util.FamilyConstants.*;

@Slf4j
@Service
public class FamilyModuleHandler implements ModuleInboxHandler {

    @Autowired
    private FamilyInboxFilterService familyService;

    @Override
    public boolean supports(String moduleName) {
        return FAMILY_SERVICE.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = familyService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria().put(FAMILY_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
        ctx.getCriteria().getModuleSearchCriteria().remove(STATUS_PARAM);
        if (ctx.getCriteria().getModuleSearchCriteria().containsKey(APPLICATION_STATUS)) {
            ctx.getCriteria().getModuleSearchCriteria().put(
                    STATUS_PARAM,
                    ctx.getCriteria().getModuleSearchCriteria().get(APPLICATION_STATUS));
        }
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return familyService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return FAMILY_IDS_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(APPLICATION_STATUS, LOCALITY_PARAM, OFFSET_PARAM, FAMILY_ID_PARAM);
    }
}

