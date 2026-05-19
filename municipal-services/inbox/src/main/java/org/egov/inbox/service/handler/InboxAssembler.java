package org.egov.inbox.service.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.repository.ElasticSearchRepository;
import org.egov.inbox.service.InboxService;
import org.egov.inbox.service.WorkflowService;
import org.egov.inbox.service.handler.impl.WSModuleHandler;
import org.egov.inbox.util.DSSConstants;
import org.egov.inbox.web.model.Inbox;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.RequestInfoWrapper;
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

        @Autowired
        private WSModuleHandler wsModuleHandler;

        @Autowired
        private ModuleHandlerRegistry registry;

        public List<Inbox> assemble(
                        InboxContext ctx,
                        ProcessInstanceSearchCriteria processCriteria,
                        List<String> businessServiceName,
                        Map<String, String> srvMap,
                        Map<String, Long> businessServiceSlaMap,
                        Map<String, List<String>> tenantAndApplnNumbersMap,
                        List<String> roles,
                        RequestInfo requestInfo,
                        WorkflowService workflowService) {

                List<Inbox> inboxes = new ArrayList<>();

                InboxSearchCriteria criteria = ctx.getCriteria();

                String moduleName = processCriteria.getModuleName();

                HashMap<String, Object> moduleSearchCriteria = criteria.getModuleSearchCriteria();

                String businessIdParam = srvMap.get("businessIdProperty");

                List<String> businessKeys = ctx.getBusinessKeys();

                if (WS.equals(moduleName) || SW.equals(moduleName)) {

                        List<Map<String, Object>> esResult = fetchFromElasticSearch(
                                        criteria,
                                        businessServiceSlaMap,
                                        ctx);

                        esResult.forEach(result -> {

                                Inbox inbox = new Inbox();

                                JsonNode jsonNode = mapper.convertValue(
                                                result.get("Data"),
                                                JsonNode.class);

                                JSONObject jsonObject = new JSONObject();

                                jsonObject.put("Data", jsonNode);

                                jsonObject.put(
                                                "serviceSLA",
                                                result.get("serviceSLA"));

                                inbox.setBusinessObject(
                                                InboxService.toMap(jsonObject));

                                inboxes.add(inbox);
                        });

                        return inboxes;
                }

                if (ctx.isSearchResultEmpty()) {
                        return inboxes;
                }

                JSONArray businessObjects = inboxService.fetchModuleObjectsPublic(
                                moduleSearchCriteria,
                                businessServiceName,
                                criteria.getTenantId(),
                                requestInfo,
                                srvMap);

                Map<String, Object> businessMap;

                if (!ObjectUtils.isEmpty(moduleName)
                                && registry.hasHandler(moduleName)) {

                        businessMap = registry.getHandler(moduleName).get()
                                        .buildBusinessMap(
                                                        businessObjects,
                                                        businessIdParam);

                } else {

                        businessMap = buildBusinessMap(
                                        businessObjects,
                                        businessIdParam);
                }

                List<Object> businessIds = new ArrayList<>(businessMap.keySet());

                processCriteria.setBusinessIds((List) businessIds);

                processCriteria.setIsProcessCountCall(false);

                Map<String, Object> serviceSearchMap = new LinkedHashMap<>();

                if (registry.hasHandler(moduleName)
                                && registry.getHandler(moduleName).get() instanceof WSModuleHandler) {

                        serviceSearchMap = wsModuleHandler.fetchServiceObjectMap(
                                        businessObjects,
                                        moduleSearchCriteria,
                                        criteria,
                                        RequestInfoWrapper.builder()
                                                        .requestInfo(requestInfo)
                                                        .build(),
                                        moduleName);
                }

                ProcessInstanceResponse processResponse;

                if (!ObjectUtils.isEmpty(moduleName)
                                && registry.hasHandler(moduleName)) {

                        processResponse = registry.getHandler(moduleName).get()
                                        .getProcessInstances(
                                                        processCriteria,
                                                        requestInfo,
                                                        workflowService,
                                                        tenantAndApplnNumbersMap,
                                                        businessIds,
                                                        roles);

                } else {

                        processResponse = workflowService.getProcessInstance(
                                        processCriteria,
                                        requestInfo);
                }

                List<ProcessInstance> processInstances = processResponse.getProcessInstances();

                if (CollectionUtils.isEmpty(processInstances)
                                || businessObjects.length() == 0) {

                        return inboxes;
                }

                Map<String, ProcessInstance> processMap = processInstances.stream()
                                .collect(Collectors.toMap(
                                                ProcessInstance::getBusinessId,
                                                Function.identity()));

                buildInboxes(
                                inboxes,
                                businessKeys,
                                businessMap,
                                processMap,
                                serviceSearchMap,
                                moduleName);

                if (isBillingModule(moduleName)) {
                        ctx.setTotalCount(processMap.size());
                }

                return inboxes;
        }

        private void buildInboxes(
                        List<Inbox> inboxes,
                        List<String> businessKeys,
                        Map<String, Object> businessMap,
                        Map<String, ProcessInstance> processMap,
                        Map<String, Object> serviceSearchMap,
                        String moduleName) {

                boolean isBilling = isBillingModule(moduleName);

                if (CollectionUtils.isEmpty(businessKeys)) {

                        businessMap.keySet().forEach(key -> {

                                ProcessInstance processInstance = processMap.get(key);

                                if (processInstance == null)
                                        return;

                                Inbox inbox = buildInbox(
                                                key,
                                                businessMap,
                                                processMap,
                                                serviceSearchMap,
                                                isBilling);

                                inboxes.add(inbox);
                        });

                } else {

                        businessKeys.forEach(key -> {

                                if (!isBilling
                                                && processMap.get(key) == null)
                                        return;

                                Inbox inbox = buildInbox(
                                                key,
                                                businessMap,
                                                processMap,
                                                serviceSearchMap,
                                                isBilling);

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
                                InboxService.toMap(
                                                (JSONObject) businessMap.get(key)));

                if (isBilling) {

                        Object consumerCode = inbox.getBusinessObject()
                                        .get("consumerCode");

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

        private Map<String, Object> buildBusinessMap(
                        JSONArray businessObjects,
                        String businessIdParam) {

                return businessObjects.toList().stream()
                                .collect(Collectors.toMap(
                                                s -> ((Map<String, Object>) s)
                                                                .get(businessIdParam)
                                                                .toString(),
                                                s -> s,
                                                (e1, e2) -> e1,
                                                LinkedHashMap::new));
        }

        private boolean isBillingModule(String moduleName) {

                return BS_WS_MODULENAME.equalsIgnoreCase(moduleName)
                                || BS_SW_MODULENAME.equalsIgnoreCase(moduleName);
        }

        private List<Map<String, Object>> fetchFromElasticSearch(
                        InboxSearchCriteria criteria,
                        Map<String, Long> businessServiceSlaMap,
                        InboxContext ctx) {

                List<Map<String, Object>> result = new ArrayList<>();

                try {

                        JsonNode responseNode = mapper.convertValue(
                                        elasticSearchRepository
                                                        .elasticSearchApplications(
                                                                        criteria,
                                                                        null),
                                        JsonNode.class);

                        JsonNode output = responseNode
                                        .get(DSSConstants.ELASTICSEARCH_HIT_KEY)
                                        .get(DSSConstants.ELASTICSEARCH_HIT_KEY);

                        ctx.setTotalCount(
                                        responseNode
                                                        .get(DSSConstants.ELASTICSEARCH_HIT_KEY)
                                                        .get("total")
                                                        .intValue());

                        if (!isNull(output) && output.isArray()) {

                                for (JsonNode node : output) {

                                        Map<String, Object> data = new HashMap<>();

                                        data.put(
                                                        "Data",
                                                        node.get("_source")
                                                                        .get("Data"));

                                        data.put(
                                                        "serviceSLA",
                                                        getApplicationServiceSla(
                                                                        businessServiceSlaMap,
                                                                        data.get("Data")));

                                        result.add(data);
                                }
                        }

                } catch (HttpClientErrorException e) {

                        log.error(
                                        "ElasticSearch fetch failed: {}",
                                        e.getMessage());

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

                        Map<String, Object> properties = mapper.convertValue(data, Map.class);

                        Map<String, Object> additionalDetails = (Map<String, Object>) properties
                                        .get("additionalDetails");

                        if (additionalDetails == null
                                        || additionalDetails.get("appCreatedDate") == null) {
                                return null;
                        }

                        Long createdDate = ((Number) additionalDetails.get("appCreatedDate"))
                                        .longValue();

                        Map<String, Object> history = (Map<String, Object>) ((List<?>) properties.get("history"))
                                        .get(0);

                        Long sla = slaMap.get(history.get("businessService"));

                        if (sla == null)
                                return null;

                        return Math.round(
                                        (sla - (System.currentTimeMillis()
                                                        - createdDate))
                                                        / (double) (24 * 60 * 60 * 1000));

                } catch (Exception e) {

                        log.error(
                                        "Failed to calculate application SLA",
                                        e);

                        return null;
                }
        }
}