package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.service.BillingAmendmentInboxFilterService;
import org.egov.inbox.service.InboxService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.RequestInfoWrapper;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.egov.inbox.util.BSConstants.*;

@Slf4j
@Service
public class WSModuleHandler implements ModuleInboxHandler {

        @Autowired
        private BillingAmendmentInboxFilterService billService;

        @Autowired
        private InboxConfiguration config;

        @Lazy
        @Autowired
        private InboxService inboxService;

        @Override
        public boolean supports(String moduleName) {
                return BS_WS.equalsIgnoreCase(moduleName) || BS_SW.equalsIgnoreCase(moduleName);
        }

        @Override
        public boolean isWorkflowTotalCountRequired() {
                return false;
        }

        @Override
        public String getInternalModuleName(String moduleName) {
                if (BS_WS.equalsIgnoreCase(moduleName))
                        return BS_WS_MODULENAME;
                if (BS_SW.equalsIgnoreCase(moduleName))
                        return BS_SW_MODULENAME;
                return moduleName;
        }

        public String getOriginalModuleName(String internalModuleName) {
                if (BS_WS_MODULENAME.equals(internalModuleName))
                        return BS_WS;
                if (BS_SW_MODULENAME.equals(internalModuleName))
                        return BS_SW;
                return internalModuleName;
        }

        @Override
        public int fetchCount(InboxContext ctx) {

                ProcessInstanceSearchCriteria processCriteria = ctx.getCriteria().getProcessSearchCriteria();

                String saved = processCriteria.getModuleName();

                processCriteria.setModuleName(getOriginalModuleName(saved));

                int count = billService.fetchApplicationCountFromSearcher(
                                ctx.getCriteria(),
                                ctx.getStatusIdNameMap(),
                                ctx.getRequestInfo());

                processCriteria.setModuleName(saved);

                return count;
        }

        @Override
        public void fetchApplicationIds(InboxContext ctx) {

                ProcessInstanceSearchCriteria processCriteria = ctx.getCriteria().getProcessSearchCriteria();

                HashMap<String, Object> moduleSearchCriteria = ctx.getCriteria().getModuleSearchCriteria();

                String saved = processCriteria.getModuleName();

                processCriteria.setModuleName(getOriginalModuleName(saved));

                Map<String, List<String>> result = billService.fetchConsumerNumbersFromSearcher(
                                ctx.getCriteria(),
                                ctx.getStatusIdNameMap(),
                                ctx.getRequestInfo());

                processCriteria.setModuleName(saved);

                List<String> consumerCodes = result.get("consumerCodes");
                List<String> amendmentIds = result.get("amendmentIds");

                if (!CollectionUtils.isEmpty(consumerCodes)) {

                        String businessService = BS_WS_MODULENAME.equals(saved) ? "WS" : "SW";

                        moduleSearchCriteria.put(
                                        BS_CONSUMER_NO_PARAM,
                                        consumerCodes);

                        moduleSearchCriteria.put(
                                        BS_BUSINESS_SERVICE_PARAM,
                                        businessService);

                        moduleSearchCriteria.put(
                                        "isPropertyDetailsRequired",
                                        true);

                        moduleSearchCriteria.remove(MOBILE_NUMBER_PARAM);
                        moduleSearchCriteria.remove(ASSIGNEE_PARAM);
                        moduleSearchCriteria.remove(LOCALITY_PARAM);
                        moduleSearchCriteria.remove(OFFSET_PARAM);

                        ctx.addBusinessKeys(amendmentIds);

                } else {

                        ctx.setSearchResultEmpty(true);
                }
        }

        @Override
        public String getApplicationIdParamKey() {
                return BS_CONSUMER_NO_PARAM;
        }

        @Override
        public List<String> paramsToRemove() {
                return List.of();
        }

        public Map<String, String> fetchServiceSearchMap(String businessServiceName) {

                StringBuilder appropriateKey = new StringBuilder();

                for (String key : config.getBsServiceSearchMapping().keySet()) {

                        if (key.contains(businessServiceName)) {

                                appropriateKey.append(key);

                                break;
                        }
                }

                if (ObjectUtils.isEmpty(appropriateKey))
                        throw new CustomException(
                                        "EG_INBOX_SEARCH_ERROR",
                                        "Inbox service is not configured for the provided business services");

                return config.getBsServiceSearchMapping().get(
                                appropriateKey.toString());
        }

        public Map<String, Object> fetchServiceObjectMap(
                        JSONArray businessObjects,
                        HashMap<String, Object> moduleSearchCriteria,
                        InboxSearchCriteria criteria,
                        RequestInfoWrapper requestInfoWrapper,
                        String moduleName) {

                Map<String, Object> serviceSearchMap = new LinkedHashMap<>();

                if (businessObjects.length() == 0
                                || (!BS_WS_MODULENAME.equalsIgnoreCase(moduleName)
                                                && !BS_SW_MODULENAME.equalsIgnoreCase(moduleName))) {

                        return serviceSearchMap;
                }

                String businessService = moduleSearchCriteria
                                .getOrDefault(BS_BUSINESS_SERVICE_PARAM, "")
                                .toString();

                if (businessService.isEmpty()) {
                        return serviceSearchMap;
                }

                Map<String, String> srvSearchMap = fetchServiceSearchMap(businessService);

                if (srvSearchMap == null || srvSearchMap.isEmpty()) {
                        return serviceSearchMap;
                }

                HashMap<String, Object> serviceSearchCriteria = new HashMap<>(moduleSearchCriteria);

                String consumerCodeParam = srvSearchMap.get("consumerCodeParam");

                if (consumerCodeParam != null
                                && serviceSearchCriteria.containsKey(
                                                BS_CONSUMER_NO_PARAM)) {

                        serviceSearchCriteria.put(
                                        consumerCodeParam,
                                        serviceSearchCriteria.get(
                                                        BS_CONSUMER_NO_PARAM));
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
                                requestInfoWrapper.getRequestInfo(),
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
}