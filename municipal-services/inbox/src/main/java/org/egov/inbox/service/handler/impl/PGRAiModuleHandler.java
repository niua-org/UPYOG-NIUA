package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.PGRAiInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.egov.inbox.util.PGRAiConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    @Override
    public Map<String, Object> buildBusinessMap(
            JSONArray businessObjects,
            String businessIdParam) {

        return StreamSupport.stream(
                businessObjects.spliterator(), false)
                .collect(Collectors.toMap(
                        s -> ((JSONObject) s)
                                .getJSONObject("service")
                                .get(businessIdParam)
                                .toString(),
                        s -> s,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}