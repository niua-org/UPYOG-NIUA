package org.egov.inbox.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.service.*;
import org.egov.inbox.util.*;
import org.egov.inbox.web.model.*;
import org.egov.inbox.web.model.workflow.*;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.jayway.jsonpath.JsonPath;
import org.springframework.web.client.RestTemplate;

import static org.egov.inbox.util.BSConstants.*;
import static org.egov.inbox.util.BpaConstants.BPA;
import static org.egov.inbox.util.BpaConstants.CITIZEN;
import static org.egov.inbox.util.FSMConstants.FSM_MODULE;
import static org.egov.inbox.util.PTConstants.PT;
import static org.egov.inbox.util.PTRConstants.PTR;
import static org.egov.inbox.util.AssetConstants.ASSET;
import static org.egov.inbox.util.EwasteConstants.EWASTE;
import static org.egov.inbox.util.SWConstants.SW;
import static org.egov.inbox.util.WSConstants.WS;
import static org.egov.inbox.util.TLConstants.REQUESTINFO_PARAM;
import static org.egov.inbox.util.TLConstants.SEARCH_CRITERIA_PARAM;
import static org.egov.inbox.util.TLConstants.TENANT_ID_PARAM;

@Slf4j
@Service
public class InboxOrchestrator {

    @Autowired private ModuleHandlerRegistry registry;
    @Autowired private WorkflowService workflowService;
    @Autowired private InboxAssembler inboxAssembler;
    @Autowired private StatusCountService statusCountService;
    @Autowired private InboxConfiguration config;
    @Autowired private BPAInboxFilterService bpaInboxFilterService;
    @Autowired private BillingAmendmentInboxFilterService billInboxFilterService;
    @Autowired private RestTemplate restTemplate;

    @Lazy
    @Autowired
    private InboxService inboxService;

    public InboxResponse fetchInboxData(
            InboxSearchCriteria criteria,
            RequestInfo requestInfo) {

        ProcessInstanceSearchCriteria processCriteria =
                criteria.getProcessSearchCriteria();
        processCriteria.setTenantId(criteria.getTenantId());

        int bsFlag = 0;
        if (BS_WS.equalsIgnoreCase(processCriteria.getModuleName())) {
            bsFlag = 1;
            processCriteria.setModuleName(BS_WS_MODULENAME);
        } else if (BS_SW.equalsIgnoreCase(processCriteria.getModuleName())) {
            bsFlag = 2;
            processCriteria.setModuleName(BS_SW_MODULENAME);
        }

        String moduleName = processCriteria.getModuleName();
        log.info("InboxOrchestrator moduleName: {}", moduleName);

        // ── Total count ───────────────────────────────────────────────────────
        Integer totalCount = 0;
        if (!(SW.equals(moduleName) || WS.equals(moduleName)))
            totalCount = workflowService.getProcessCount(
                    criteria.getTenantId(), requestInfo, processCriteria);

        // ── Nearing SLA count ─────────────────────────────────────────────────
        Integer nearingSlaCount = 0;
        if (!(PTR.equals(moduleName) || PT.equals(moduleName)
                || ASSET.equals(moduleName) || EWASTE.equals(moduleName)))
            nearingSlaCount = workflowService.getNearingSlaProcessCount(
                    criteria.getTenantId(), requestInfo, processCriteria);

        List<String> inputStatuses = CollectionUtils.isEmpty(
                processCriteria.getStatus())
                ? new ArrayList<>()
                : new ArrayList<>(processCriteria.getStatus());

        // ── DSO ID fetch for FSM DSO role ─────────────────────────────────────
        String dsoId = null;
        if (requestInfo.getUserInfo().getRoles().get(0).getCode()
                .equals(FSMConstants.FSM_DSO)) {
            Map<String, Object> searcherRequest = new HashMap<>();
            Map<String, Object> searchCriteria = new HashMap<>();
            searchCriteria.put(TENANT_ID_PARAM, criteria.getTenantId());
            searchCriteria.put(FSMConstants.OWNER_ID,
                    requestInfo.getUserInfo().getUuid());
            searcherRequest.put(REQUESTINFO_PARAM, requestInfo);
            searcherRequest.put(SEARCH_CRITERIA_PARAM, searchCriteria);
            StringBuilder dsoUri = new StringBuilder();
            dsoUri.append(config.getSearcherHost())
                  .append(config.getFsmInboxDSoIDEndpoint());
            Object dsoResult = restTemplate.postForObject(
                    dsoUri.toString(), searcherRequest, Map.class);
            dsoId = JsonPath.read(dsoResult, "$.vendor[0].id");
        }

        StringBuilder assigneeUuid = new StringBuilder();
        if (!ObjectUtils.isEmpty(processCriteria.getAssignee())) {
            assigneeUuid.append(processCriteria.getAssignee());
            processCriteria.setStatus(null);
        }

        processCriteria.setAssignee(null);
        processCriteria.setStatus(null);

        List<String> roles = requestInfo.getUserInfo().getRoles()
                .stream().map(Role::getCode).collect(Collectors.toList());

        List<HashMap<String, Object>> statusCountMap =
                workflowService.getProcessStatusCount(requestInfo, processCriteria);

        processCriteria.setModuleName(moduleName);
        processCriteria.setStatus(inputStatuses);
        processCriteria.setAssignee(assigneeUuid.toString());

        List<String> businessServiceName =
                processCriteria.getBusinessService();

        if (CollectionUtils.isEmpty(businessServiceName))
            throw new CustomException(
                    ErrorConstants.MODULE_SEARCH_INVLAID,
                    "Business Service is mandatory");

        Map<String, String> srvMap =
                inboxService.fetchAppropriateServiceMapPublic(
                        businessServiceName, moduleName);

        HashMap<String, Object> moduleSearchCriteria =
                criteria.getModuleSearchCriteria();

        Map<String, Long> businessServiceSlaMap = new HashMap<>();
        List<Inbox> inboxes = new ArrayList<>();
        InboxResponse response = new InboxResponse();

        if (CollectionUtils.isEmpty(moduleSearchCriteria)) {

            processCriteria.setOffset(criteria.getOffset());
            processCriteria.setLimit(criteria.getLimit());

            ProcessInstanceResponse piResp =
                    workflowService.getProcessInstance(processCriteria, requestInfo);

            Map<String, ProcessInstance> piMap =
                    piResp.getProcessInstances().stream()
                            .collect(Collectors.toMap(
                                    ProcessInstance::getBusinessId,
                                    Function.identity()));

            HashMap<String, Object> emptySearchCriteria = new HashMap<>();
            emptySearchCriteria.put(
                    srvMap.get("applNosParam"),
                    StringUtils.arrayToDelimitedString(
                            piMap.keySet().toArray(), ","));
            emptySearchCriteria.put("tenantId", criteria.getTenantId());
            emptySearchCriteria.put("limit", "-1");

            JSONArray bObjs = inboxService.fetchModuleObjectsPublic(
                    emptySearchCriteria,
                    businessServiceName,
                    criteria.getTenantId(),
                    requestInfo,
                    srvMap);

            String bizIdParam = srvMap.get("businessIdProperty");

            Map<String, Object> bizMap =
                    StreamSupport.stream(bObjs.spliterator(), false)
                            .collect(Collectors.toMap(
                                    s -> ((JSONObject) s).get(bizIdParam).toString(),
                                    s -> s));

            if (bObjs.length() > 0 && !piMap.isEmpty()) {
                for (String k : piMap.keySet()) {
                    Inbox inbox = new Inbox();
                    inbox.setProcessInstance(piMap.get(k));
                    inbox.setBusinessObject(
                            InboxService.toMap((JSONObject) bizMap.get(k)));
                    inboxes.add(inbox);
                }
            }

        } else {

            moduleSearchCriteria.put("tenantId", criteria.getTenantId());
            moduleSearchCriteria.put("offset", criteria.getOffset());
            moduleSearchCriteria.put("limit", criteria.getLimit());

            // Fetch business services and build SLA map
            List<BusinessService> bServices = new ArrayList<>();
            for (String bs : businessServiceName) {
                BusinessService bSrv = workflowService.getBusinessService(
                        criteria.getTenantId(), requestInfo, bs);
                bServices.add(bSrv);
                businessServiceSlaMap.put(
                        bSrv.getBusinessService(),
                        bSrv.getBusinessServiceSla());
            }

            HashMap<String, String> statusIdNameMap =
                    workflowService.getActionableStatusesForRole(
                            requestInfo, bServices, processCriteria);

            String appStatusParam = srvMap.getOrDefault(
                    "applsStatusParam", "applicationStatus");

            // Populate applicationStatus param in moduleSearchCriteria
            if (!statusIdNameMap.isEmpty()) {
                if (!CollectionUtils.isEmpty(processCriteria.getStatus())) {
                    List<String> statuses = processCriteria.getStatus().stream()
                            .map(statusIdNameMap::get)
                            .collect(Collectors.toList());
                    moduleSearchCriteria.put(
                            appStatusParam,
                            StringUtils.arrayToDelimitedString(
                                    statuses.toArray(), ","));
                } else {
                    moduleSearchCriteria.put(
                            appStatusParam,
                            StringUtils.arrayToDelimitedString(
                                    statusIdNameMap.values().toArray(), ","));
                }
            }

            // BPA citizen multi-tenant status count
            Map<String, List<String>> tenantAndApplnNumbersMap = new HashMap<>();

            statusCountMap = statusCountService.handleBpaCitizenStatusCount(
                    criteria, processCriteria, statusIdNameMap,
                    statusCountMap, tenantAndApplnNumbersMap,
                    roles, requestInfo, workflowService);

            // BPA locality-based status count
            statusCountMap = statusCountService.handleBpaLocalityStatusCount(
                    criteria, processCriteria, statusIdNameMap,
                    statusCountMap, inputStatuses,
                    bpaInboxFilterService, requestInfo);

            // Build context for module handler
            InboxContext ctx = InboxContext.builder()
                    .criteria(criteria)
                    .requestInfo(requestInfo)
                    .statusIdNameMap(statusIdNameMap)
                    .srvMap(srvMap)
                    .totalCount(totalCount)
                    .build();

            // ── Module handler registry (TL, BPA, PT, CHB, NDC, ...) ──────────
            boolean handled = false;

            if (!ObjectUtils.isEmpty(moduleName)
                    && registry.hasHandler(moduleName)) {

                ModuleInboxHandler handler =
                        registry.getHandler(moduleName).get();

                int handlerCount = handler.fetchCount(ctx);
                if (handlerCount >= 0)
                    totalCount = handlerCount;

                handler.fetchApplicationIds(ctx);

                handler.paramsToRemove()
                        .forEach(ctx::removeModuleSearchCriteria);

                handled = true;
            }

            // ── BS WS billing path ────────────────────────────────────────────
            if (!handled
                    && BS_WS_MODULENAME.equals(moduleName)
                    && bsFlag == 1) {

                processCriteria.setModuleName(BS_WS);

                totalCount = billInboxFilterService
                        .fetchApplicationCountFromSearcher(
                                criteria, statusIdNameMap, requestInfo);

                Map<String, List<String>> map =
                        billInboxFilterService.fetchConsumerNumbersFromSearcher(
                                criteria, statusIdNameMap, requestInfo);

                List<String> consumerCodes = map.get("consumerCodes");
                List<String> amendmentIds  = map.get("amendmentIds");

                if (!CollectionUtils.isEmpty(consumerCodes)) {
                    moduleSearchCriteria.put(BS_CONSUMER_NO_PARAM, consumerCodes);
                    ctx.addBusinessKeys(amendmentIds);
                    moduleSearchCriteria.put(BS_BUSINESS_SERVICE_PARAM, "WS");
                    moduleSearchCriteria.remove(MOBILE_NUMBER_PARAM);
                    moduleSearchCriteria.remove(ASSIGNEE_PARAM);
                    moduleSearchCriteria.remove(LOCALITY_PARAM);
                    moduleSearchCriteria.remove(OFFSET_PARAM);
                } else {
                    ctx.setSearchResultEmpty(true);
                }

                moduleSearchCriteria.put("isPropertyDetailsRequired", true);
                processCriteria.setModuleName(BS_WS_MODULENAME);
            }

            // ── BS SW billing path ────────────────────────────────────────────
            if (!handled
                    && BS_SW_MODULENAME.equals(moduleName)
                    && bsFlag == 2) {

                processCriteria.setModuleName(BS_SW);

                totalCount = billInboxFilterService
                        .fetchApplicationCountFromSearcher(
                                criteria, statusIdNameMap, requestInfo);

                Map<String, List<String>> map =
                        billInboxFilterService.fetchConsumerNumbersFromSearcher(
                                criteria, statusIdNameMap, requestInfo);

                List<String> consumerCodes = map.get("consumerCodes");
                List<String> amendmentIds  = map.get("amendmentIds");

                if (!CollectionUtils.isEmpty(consumerCodes)) {
                    moduleSearchCriteria.put(BS_CONSUMER_NO_PARAM, consumerCodes);
                    ctx.addBusinessKeys(amendmentIds);
                    moduleSearchCriteria.put(BS_BUSINESS_SERVICE_PARAM, "SW");
                    moduleSearchCriteria.remove(MOBILE_NUMBER_PARAM);
                    moduleSearchCriteria.remove(ASSIGNEE_PARAM);
                    moduleSearchCriteria.remove(LOCALITY_PARAM);
                    moduleSearchCriteria.remove(OFFSET_PARAM);
                } else {
                    ctx.setSearchResultEmpty(true);
                }

                moduleSearchCriteria.put("isPropertyDetailsRequired", true);
                processCriteria.setModuleName(BS_SW_MODULENAME);
            }

            // build the final inbox list
            inboxes = inboxAssembler.assemble(
                    ctx,
                    processCriteria,
                    businessServiceName,
                    srvMap,
                    businessServiceSlaMap,
                    tenantAndApplnNumbersMap,
                    roles,
                    bsFlag,
                    totalCount,
                    requestInfo,
                    workflowService);

            // Update totalCount from ctx if assembler modified it
            if (ctx.getTotalCount() != null) {
                totalCount = ctx.getTotalCount();
            }

            if (!ObjectUtils.isEmpty(moduleName)
                    && moduleName.equalsIgnoreCase(FSM_MODULE)) {

                statusCountMap = statusCountService.enrichFsmStatusCount(
                        criteria,
                        requestInfo,
                        statusCountMap,
                        inputStatuses,
                        inboxes,
                        moduleSearchCriteria,
                        processCriteria,
                        businessServiceName,
                        srvMap,
                        inboxService,
                        workflowService);

                totalCount = statusCountService.getFsmTotalCount(
                        statusCountMap, inputStatuses, totalCount);
            }
        }

        log.info("statusCountMap size: {}", statusCountMap.size());

        response.setTotalCount(totalCount);
        response.setNearingSlaCount(nearingSlaCount);
        response.setStatusMap(statusCountMap);
        response.setItems(inboxes);

        return response;
    }
}