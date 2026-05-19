package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.PtrInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.PTConstants.ACKNOWLEDGEMENT_IDS_PARAM;
import static org.egov.inbox.util.PTRConstants.PTR;
import static org.egov.inbox.util.TLConstants.*;

@Slf4j
@Service
public class PTRModuleHandler implements ModuleInboxHandler {

    @Autowired
    private PtrInboxFilterService ptrService;

    @Override
    public boolean supports(String moduleName) {
        return PTR.equals(moduleName);
    }

    @Override
    public boolean isWorkflowNearingSlaCountRequired() {
        return false;
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = ptrService.fetchApplicationNumbersFromSearcher(
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
        return List.of(LOCALITY_PARAM, OFFSET_PARAM);
    }
}
