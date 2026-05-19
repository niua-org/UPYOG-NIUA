package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.PtInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.PTConstants.*;

@Slf4j
@Service
public class PTModuleHandler implements ModuleInboxHandler {

    @Autowired
    private PtInboxFilterService ptService;

    @Override
    public boolean supports(String moduleName) {
        return PT.equals(moduleName);
    }

    @Override
    public boolean isWorkflowNearingSlaCountRequired() {
        return false;
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ptService.fetchAcknowledgementIdsFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria().put(ACKNOWLEDGEMENT_IDS_PARAM, ids);
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
        return List.of(LOCALITY_PARAM, OFFSET_PARAM);
    }
}
