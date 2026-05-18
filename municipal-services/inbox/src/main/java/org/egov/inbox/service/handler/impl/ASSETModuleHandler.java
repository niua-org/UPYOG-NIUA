package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.AssetInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.AssetConstants.ASSET;
import static org.egov.inbox.util.PTConstants.ACKNOWLEDGEMENT_IDS_PARAM;

@Slf4j
@Service
public class ASSETModuleHandler implements ModuleInboxHandler {

    @Autowired
    private AssetInboxFilterService assetService;

    @Override
    public boolean supports(String moduleName) {
        return ASSET.equals(moduleName);
    }

    @Override
    public boolean workflowNearingSlaCount() {
        return false;
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = assetService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria().put(ACKNOWLEDGEMENT_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public String getApplicationIdParamKey() {
        return ACKNOWLEDGEMENT_IDS_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of();
    }
}
