package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.service.BPAInboxFilterService;
import org.egov.inbox.service.WorkflowService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.egov.inbox.web.model.Inbox;
import org.egov.inbox.web.model.workflow.ProcessInstanceResponse;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.egov.inbox.util.BpaConstants.*;
import static org.egov.inbox.util.FSMConstants.COUNT;

@Slf4j
@Service
public class BPAModuleHandler implements ModuleInboxHandler {

        @Autowired
        private BPAInboxFilterService bpaService;

        @Autowired
        private WorkflowService workflowService;

        @Override
        public boolean supports(String moduleName) {
                return BPA.equals(moduleName);
        }

        @Override
        public void fetchApplicationIds(InboxContext ctx) {
                List<String> ids = bpaService.fetchApplicationNumbersFromSearcher(
                                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());

                if (CollectionUtils.isEmpty(ids)) {
                        ctx.setSearchResultEmpty(true);
                        return;
                }

                ctx.getCriteria().getModuleSearchCriteria().put(BPA_APPLICATION_NUMBER_PARAM, ids);

                ctx.addBusinessKeys(ids);
        }

        @Override
        public int fetchCount(InboxContext ctx) {
                return bpaService.fetchApplicationCountFromSearcher(
                                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        }

        @Override
        public String getApplicationIdParamKey() {
                return BPA_APPLICATION_NUMBER_PARAM;
        }

        @Override
        public List<String> paramsToRemove() {
                return List.of(STATUS_PARAM, MOBILE_NUMBER_PARAM, LOCALITY_PARAM, OFFSET_PARAM);
        }

        @Override
        public List<HashMap<String, Object>> enrichStatusCountPreFetch(
                        InboxContext ctx,
                        List<HashMap<String, Object>> statusCountMap,
                        Map<String, List<String>> tenantAndApplnNumbersMap,
                        List<String> roles,
                        List<String> inputStatuses) {

                statusCountMap = handleBpaCitizenStatusCount(
                                ctx, statusCountMap, tenantAndApplnNumbersMap, roles);

                statusCountMap = handleBpaLocalityStatusCount(
                                ctx, statusCountMap, inputStatuses);

                return statusCountMap;
        }

        @Override
        public PostAssembleResult enrichStatusCountPostAssemble(
                        InboxContext ctx,
                        List<HashMap<String, Object>> statusCountMap,
                        List<String> inputStatuses,
                        List<Inbox> inboxes,
                        Integer totalCount) {

                return new PostAssembleResult(statusCountMap, totalCount);
        }

        @Override
        public ProcessInstanceResponse getProcessInstances(
                        ProcessInstanceSearchCriteria processCriteria,
                        RequestInfo requestInfo,
                        WorkflowService workflowService,
                        Map<String, List<String>> tenantAndApplnNumbersMap,
                        List<Object> businessIds,
                        List<String> roles) {

                if (!roles.contains(CITIZEN)) {
                        return workflowService.getProcessInstance(processCriteria, requestInfo);
                }

                Map<String, List<String>> tenantApplicationMap = new HashMap<>();

                for (Object businessId : businessIds) {

                        for (Map.Entry<String, List<String>> entry : tenantAndApplnNumbersMap.entrySet()) {

                                if (entry.getValue().contains(businessId)) {

                                        tenantApplicationMap
                                                        .computeIfAbsent(
                                                                        entry.getKey(),
                                                                        k -> new ArrayList<>())
                                                        .add(String.valueOf(businessId));
                                }
                        }
                }

                ProcessInstanceResponse mergedResponse = new ProcessInstanceResponse();

                for (Map.Entry<String, List<String>> entry : tenantApplicationMap.entrySet()) {

                        processCriteria.setTenantId(entry.getKey());

                        processCriteria.setBusinessIds(entry.getValue());

                        ProcessInstanceResponse response = workflowService.getProcessInstance(
                                        processCriteria,
                                        requestInfo);

                        mergedResponse.setResponseInfo(
                                        response.getResponseInfo());

                        if (mergedResponse.getProcessInstances() == null) {

                                mergedResponse.setProcessInstances(
                                                response.getProcessInstances());

                        } else {

                                mergedResponse.getProcessInstances()
                                                .addAll(response.getProcessInstances());
                        }
                }

                return mergedResponse;
        }

        private List<HashMap<String, Object>> handleBpaCitizenStatusCount(
                        InboxContext ctx,
                        List<HashMap<String, Object>> statusCountMap,
                        Map<String, List<String>> tenantAndApplnNumbersMap,
                        List<String> roles) {

                if (roles.contains(CITIZEN)) {

                        List<Map<String, String>> tenantWiseApplns = bpaService
                                        .fetchTenantWiseApplicationNumbersForCitizenInboxFromSearcher(
                                                        ctx.getCriteria(),
                                                        ctx.getStatusIdNameMap(),
                                                        ctx.getRequestInfo());

                        if (!CollectionUtils.isEmpty(tenantWiseApplns)) {

                                tenantWiseApplns.forEach(applicationMap -> {

                                        String tenant = applicationMap.get("tenantid");

                                        String applnNo = applicationMap.get("applicationno");

                                        if (tenantAndApplnNumbersMap.containsKey(tenant)) {

                                                List<String> applnNos = tenantAndApplnNumbersMap.get(tenant);

                                                applnNos.add(applnNo);

                                                tenantAndApplnNumbersMap.put(
                                                                tenant,
                                                                applnNos);

                                        } else {

                                                List<String> l = new ArrayList<>();

                                                l.add(applnNo);

                                                tenantAndApplnNumbersMap.put(
                                                                tenant,
                                                                l);
                                        }
                                });
                        }
                }

                if (!roles.contains(CITIZEN)
                                || tenantAndApplnNumbersMap.isEmpty())
                        return statusCountMap;

                List<HashMap<String, Object>> bpaCitizenMap = new ArrayList<>();

                var processCriteria = ctx.getCriteria().getProcessSearchCriteria();

                String savedTenant = processCriteria.getTenantId();

                List<String> savedBizIds = processCriteria.getBusinessIds();

                List<String> savedStatus = processCriteria.getStatus();

                if (!ctx.getStatusIdNameMap().isEmpty())
                        processCriteria.setStatus(
                                        new ArrayList<>(
                                                        ctx.getStatusIdNameMap().keySet()));

                for (Map.Entry<String, List<String>> entry : tenantAndApplnNumbersMap.entrySet()) {

                        processCriteria.setTenantId(entry.getKey());

                        processCriteria.setBusinessIds(entry.getValue());

                        List<HashMap<String, Object>> tenantCount = workflowService.getProcessStatusCount(
                                        ctx.getRequestInfo(),
                                        processCriteria);

                        if (bpaCitizenMap.isEmpty()) {

                                bpaCitizenMap.addAll(tenantCount);

                        } else {

                                for (HashMap<String, Object> tenantMap : tenantCount) {

                                        for (HashMap<String, Object> citizenMap : bpaCitizenMap) {

                                                if (citizenMap.containsValue(
                                                                tenantMap.get(STATUS_ID))) {

                                                        citizenMap.put(
                                                                        COUNT,
                                                                        Integer.parseInt(
                                                                                        String.valueOf(
                                                                                                        citizenMap.get(COUNT)))
                                                                                        + Integer.parseInt(
                                                                                                        String.valueOf(
                                                                                                                        tenantMap.get(COUNT))));
                                                }
                                        }
                                }
                        }
                }

                processCriteria.setTenantId(savedTenant);

                processCriteria.setBusinessIds(savedBizIds);

                processCriteria.setStatus(savedStatus);

                return bpaCitizenMap.isEmpty()
                                ? statusCountMap
                                : bpaCitizenMap;
        }

        private List<HashMap<String, Object>> handleBpaLocalityStatusCount(
                        InboxContext ctx,
                        List<HashMap<String, Object>> statusCountMap,
                        List<String> inputStatuses) {

                HashMap<String, Object> moduleSearchCriteria = ctx.getCriteria().getModuleSearchCriteria();

                if (moduleSearchCriteria.get(LOCALITY_PARAM) == null)
                        return statusCountMap;

                for (Map<String, Object> statusWiseCount : statusCountMap) {

                        List<String> statusList = Collections.singletonList(
                                        String.valueOf(
                                                        statusWiseCount.get(STATUS_ID)));

                        ctx.getCriteria()
                                        .getProcessSearchCriteria()
                                        .setStatus(statusList);

                        Integer count = bpaService.fetchApplicationCountFromSearcher(
                                        ctx.getCriteria(),
                                        ctx.getStatusIdNameMap(),
                                        ctx.getRequestInfo());

                        if (count == 0)
                                statusWiseCount.clear();

                        else
                                statusWiseCount.put(COUNT, count);
                }

                ctx.getCriteria()
                                .getProcessSearchCriteria()
                                .setStatus(inputStatuses);

                return statusCountMap.stream()
                                .filter(map -> !map.isEmpty())
                                .collect(Collectors.toList());
        }
}
