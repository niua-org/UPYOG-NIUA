package org.egov.inbox.service.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.repository.ElasticSearchRepository;
import org.egov.inbox.service.InboxService;
import org.egov.inbox.service.WorkflowService;
import org.egov.inbox.util.BpaConstants;
import org.egov.inbox.util.DSSConstants;
import org.egov.inbox.util.PGRAiConstants;
import org.egov.inbox.web.model.Inbox;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.workflow.ProcessInstance;
import org.egov.inbox.web.model.workflow.ProcessInstanceResponse;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.isNull;
import static org.egov.inbox.util.BSConstants.*;
import static org.egov.inbox.util.SWConstants.SW;
import static org.egov.inbox.util.WSConstants.WS;

@Slf4j
@Service
public class InboxAssembler {

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private ObjectMapper mapper;

    @Lazy
    @Autowired
    private InboxService inboxService;

    // ─────────────────────────────────────────────────────────────────────────
    // Main assemble method
    // ─────────────────────────────────────────────────────────────────────────

    public List<Inbox> assemble(
            InboxContext ctx,
            ProcessInstanceSearchCriteria processCriteria,
            List<String> businessServiceName,
            Map<String, String> srvMap,
            Map<String, Long> businessServiceSlaMap,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<String> roles,
            int bsFlag,
            Integer totalCount,
            RequestInfo requestInfo,
            WorkflowService workflowService) {

        List<Inbox> inboxes = new ArrayList<>();

        InboxSearchCriteria criteria   = ctx.getCriteria();
        String moduleName              = processCriteria.getModuleName();
        HashMap<String, Object> moduleSearchCriteria =
                criteria.getModuleSearchCriteria();
        String businessIdParam         = srvMap.get("businessIdProperty");
        List<String> businessKeys      = ctx.getBusinessKeys();

        // ─────────────────────────────────────────────────────────────────────
        // WS / SW — ElasticSearch path
        // ─────────────────────────────────────────────────────────────────────
        if (WS.equals(moduleName) || SW.equals(moduleName)) {

            List<Map<String, Object>> esResult =
                    fetchFromElasticSearch(criteria, businessServiceSlaMap, ctx);

            esResult.forEach(result -> {

                Inbox inbox = new Inbox();

                JsonNode jsonNode = mapper.convertValue(
                        result.get("Data"), JsonNode.class);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Data", jsonNode);
                jsonObject.put("serviceSLA", result.get("serviceSLA"));

                inbox.setBusinessObject(InboxService.toMap(jsonObject));
                inboxes.add(inbox);
            });

            return inboxes;
        }

        if (ctx.isSearchResultEmpty()) {
            return inboxes;
        }

        // ─────────────────────────────────────────────────────────────────────
        // Fetch business objects from module search API
        // ─────────────────────────────────────────────────────────────────────
        JSONArray businessObjects = inboxService.fetchModuleObjectsPublic(
                moduleSearchCriteria,
                businessServiceName,
                criteria.getTenantId(),
                requestInfo,
                srvMap);

        Map<String, Object> businessMap = buildBusinessMap(
                businessObjects, businessServiceName, businessIdParam);

        List<Object> businessIds = new ArrayList<>(businessMap.keySet());
        processCriteria.setBusinessIds((List) businessIds);
        processCriteria.setIsProcessCountCall(false);

        // ─────────────────────────────────────────────────────────────────────
        // WS / SW Amendment — service search objects
        // ─────────────────────────────────────────────────────────────────────
        Map<String, Object> serviceSearchMap = fetchServiceSearchMap(
                businessObjects,
                businessServiceName,
                moduleSearchCriteria,
                criteria,
                requestInfo,
                moduleName,
                bsFlag);

        // ─────────────────────────────────────────────────────────────────────
        // Fetch process instances
        // ─────────────────────────────────────────────────────────────────────
        ProcessInstanceResponse processResponse = getProcessInstances(
                processCriteria,
                requestInfo,
                moduleName,
                roles,
                tenantAndApplnNumbersMap,
                businessIds,
                workflowService);

        List<ProcessInstance> processInstances =
                processResponse.getProcessInstances();

        if (CollectionUtils.isEmpty(processInstances)
                || businessObjects.length() == 0) {
            return inboxes;
        }

        Map<String, ProcessInstance> processMap =
                processInstances.stream()
                        .collect(Collectors.toMap(
                                ProcessInstance::getBusinessId,
                                Function.identity()));

        // combine business objects and process instances into inbox items
        buildInboxes(
                inboxes,
                businessKeys,
                businessMap,
                processMap,
                serviceSearchMap,
                moduleName);

        // Update totalCount in ctx for billing modules
        if (isBillingModule(moduleName)) {
            ctx.setTotalCount(processMap.size());
        }

        return inboxes;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inbox Builder
    // ─────────────────────────────────────────────────────────────────────────

    private void buildInboxes(
            List<Inbox> inboxes,
            List<String> businessKeys,
            Map<String, Object> businessMap,
            Map<String, ProcessInstance> processMap,
            Map<String, Object> serviceSearchMap,
            String moduleName) {

        boolean isBilling = isBillingModule(moduleName);

        if (CollectionUtils.isEmpty(businessKeys)) {

            // No explicit key order — iterate businessMap keys
            businessMap.keySet().forEach(key -> {

                ProcessInstance processInstance = processMap.get(key);
                if (processInstance == null) return;

                Inbox inbox = buildInbox(
                        key, businessMap, processMap,
                        serviceSearchMap, isBilling);
                inboxes.add(inbox);
            });

        } else {

            // Maintain order of businessKeys
            businessKeys.forEach(key -> {

                if (!isBilling && processMap.get(key) == null) return;

                Inbox inbox = buildInbox(
                        key, businessMap, processMap,
                        serviceSearchMap, isBilling);

                if (inbox.getProcessInstance() != null) {
                    inboxes.add(inbox);
                }
            });
        }
    }

    private Inbox buildInbox(
            String key,
            Map<String, Object> businessMap,
            Map<String, ProcessInstance> processMap,
            Map<String, Object> serviceSearchMap,
            boolean isBilling) {

        Inbox inbox = new Inbox();

        inbox.setProcessInstance(processMap.get(key));

        inbox.setBusinessObject(
                InboxService.toMap((JSONObject) businessMap.get(key)));

        if (isBilling) {
            Object consumerCode =
                    inbox.getBusinessObject().get("consumerCode");

            if (consumerCode != null
                    && serviceSearchMap.containsKey(
                    consumerCode.toString())) {

                inbox.setServiceObject(
                        InboxService.toMap(
                                (JSONObject) serviceSearchMap.get(
                                        consumerCode.toString())));
            }
        }

        return inbox;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Business Object Map Builder
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildBusinessMap(
            JSONArray businessObjects,
            List<String> businessServiceName,
            String businessIdParam) {

        // PGR AI has different response structure
        if (businessServiceName.contains(PGRAiConstants.PGR_SERVICE)) {
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

        return StreamSupport.stream(
                        businessObjects.spliterator(), false)
                .collect(Collectors.toMap(
                        s -> ((JSONObject) s)
                                .get(businessIdParam)
                                .toString(),
                        s -> s,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Billing module helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isBillingModule(String moduleName) {
        return BS_WS_MODULENAME.equalsIgnoreCase(moduleName)
                || BS_SW_MODULENAME.equalsIgnoreCase(moduleName);
    }

    /**
     * For WS/SW billing amendment — fetches the underlying connection objects
     * so they can be set as serviceObject on the Inbox.
     * Returns empty map for all other modules.
     */
    private Map<String, Object> fetchServiceSearchMap(
            JSONArray businessObjects,
            List<String> businessServiceName,
            HashMap<String, Object> moduleSearchCriteria,
            InboxSearchCriteria criteria,
            RequestInfo requestInfo,
            String moduleName,
            int bsFlag) {

        Map<String, Object> serviceSearchMap = new LinkedHashMap<>();

        if (businessObjects.length() == 0
                || !isBillingModule(moduleName)) {
            return serviceSearchMap;
        }

        // Determine which business service (WS or SW)
        String businessService = moduleSearchCriteria
                .getOrDefault(BS_BUSINESS_SERVICE_PARAM, "")
                .toString();

        if (businessService.isEmpty()) {
            return serviceSearchMap;
        }

        // Fetch the service search map config for WS or SW
        Map<String, String> srvSearchMap =
                inboxService.fetchAppropriateServiceMapPublic(
                        List.of(businessService), moduleName);

        if (srvSearchMap == null || srvSearchMap.isEmpty()) {
            return serviceSearchMap;
        }

        // Build search criteria for connection search
        HashMap<String, Object> serviceSearchCriteria =
                new HashMap<>(moduleSearchCriteria);

        String consumerCodeParam = srvSearchMap.get("consumerCodeParam");
        if (consumerCodeParam != null
                && serviceSearchCriteria.containsKey(BS_CONSUMER_NO_PARAM)) {
            serviceSearchCriteria.put(
                    consumerCodeParam,
                    serviceSearchCriteria.get(BS_CONSUMER_NO_PARAM));
        }

        serviceSearchCriteria.remove(BS_CONSUMER_NO_PARAM);
        serviceSearchCriteria.remove(BS_BUSINESS_SERVICE_PARAM);
        serviceSearchCriteria.remove(BS_APPLICATION_NUMBER_PARAM);
        serviceSearchCriteria.remove("status");
        serviceSearchCriteria.put("searchType", "CONNECTION");

        JSONArray serviceObjects = inboxService.fetchModuleSearchObjectsPublic(
                serviceSearchCriteria,
                List.of(businessService),
                criteria.getTenantId(),
                requestInfo,
                srvSearchMap);

        return StreamSupport.stream(
                        serviceObjects.spliterator(), false)
                .collect(Collectors.toMap(
                        s -> ((JSONObject) s)
                                .get("connectionNo")
                                .toString(),
                        s -> s,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Workflow process instance fetch
    // ─────────────────────────────────────────────────────────────────────────

    private ProcessInstanceResponse getProcessInstances(
            ProcessInstanceSearchCriteria processCriteria,
            RequestInfo requestInfo,
            String moduleName,
            List<String> roles,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<Object> businessIds,
            WorkflowService workflowService) {

        // BPA citizen — multi-tenant process instance fetch
        if (!ObjectUtils.isEmpty(moduleName)
                && moduleName.equals(BpaConstants.BPA)
                && roles.contains(BpaConstants.CITIZEN)) {

            return fetchBpaCitizenProcessInstances(
                    processCriteria,
                    requestInfo,
                    tenantAndApplnNumbersMap,
                    businessIds,
                    workflowService);
        }

        return workflowService.getProcessInstance(
                processCriteria, requestInfo);
    }

    private ProcessInstanceResponse fetchBpaCitizenProcessInstances(
            ProcessInstanceSearchCriteria processCriteria,
            RequestInfo requestInfo,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<Object> businessIds,
            WorkflowService workflowService) {

        Map<String, List<String>> tenantApplicationMap = new HashMap<>();

        for (Object businessId : businessIds) {
            for (Map.Entry<String, List<String>> entry
                    : tenantAndApplnNumbersMap.entrySet()) {
                if (entry.getValue().contains(businessId)) {
                    tenantApplicationMap
                            .computeIfAbsent(
                                    entry.getKey(),
                                    k -> new ArrayList<>())
                            .add(String.valueOf(businessId));
                }
            }
        }

        ProcessInstanceResponse mergedResponse =
                new ProcessInstanceResponse();

        for (Map.Entry<String, List<String>> entry
                : tenantApplicationMap.entrySet()) {

            processCriteria.setTenantId(entry.getKey());
            processCriteria.setBusinessIds(entry.getValue());

            ProcessInstanceResponse response =
                    workflowService.getProcessInstance(
                            processCriteria, requestInfo);

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

    // ─────────────────────────────────────────────────────────────────────────
    // ElasticSearch fetch for WS / SW
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchFromElasticSearch(
            InboxSearchCriteria criteria,
            Map<String, Long> businessServiceSlaMap,
            InboxContext ctx) {

        List<Map<String, Object>> result = new ArrayList<>();

        try {
            JsonNode responseNode = mapper.convertValue(
                    elasticSearchRepository.elasticSearchApplications(
                            criteria, null),
                    JsonNode.class);

            JsonNode output = responseNode
                    .get(DSSConstants.ELASTICSEARCH_HIT_KEY)
                    .get(DSSConstants.ELASTICSEARCH_HIT_KEY);

            // Update totalCount from ES response
            ctx.setTotalCount(
                    responseNode
                            .get(DSSConstants.ELASTICSEARCH_HIT_KEY)
                            .get("total")
                            .intValue());

            if (!isNull(output) && output.isArray()) {
                for (JsonNode node : output) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("Data",
                            node.get("_source").get("Data"));
                    data.put("serviceSLA",
                            getApplicationServiceSla(
                                    businessServiceSlaMap,
                                    data.get("Data")));
                    result.add(data);
                }
            }

        } catch (HttpClientErrorException e) {
            log.error("ElasticSearch fetch failed: {}", e.getMessage());
            throw new CustomException(
                    "ELASTICSEARCH_ERROR",
                    "ES error: " + e.getMessage());
        }

        return result;
    }

    private Long getApplicationServiceSla(
            Map<String, Long> slaMap,
            Object data) {

        try {
            Map<String, Object> properties =
                    mapper.convertValue(data, Map.class);

            Map<String, Object> additionalDetails =
                    (Map<String, Object>) properties.get(
                            "additionalDetails");

            if (additionalDetails == null
                    || additionalDetails.get("appCreatedDate") == null) {
                return null;
            }

            Long createdDate =
                    ((Number) additionalDetails.get("appCreatedDate"))
                            .longValue();

            Map<String, Object> history =
                    (Map<String, Object>)
                            ((List<?>) properties.get("history")).get(0);

            Long sla = slaMap.get(history.get("businessService"));
            if (sla == null) return null;

            return Math.round(
                    (sla - (System.currentTimeMillis() - createdDate))
                            / (double) (24 * 60 * 60 * 1000));

        } catch (Exception e) {
            log.error("Failed to calculate application SLA", e);
            return null;
        }
    }
}