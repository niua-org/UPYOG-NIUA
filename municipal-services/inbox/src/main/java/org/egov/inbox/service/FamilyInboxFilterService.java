package org.egov.inbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.repository.ServiceRequestRepository;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.egov.inbox.util.CommonConstants.*;
import static org.egov.inbox.util.FamilyConstants.*;

@Slf4j
@Service
public class FamilyInboxFilterService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private InboxConfiguration config;

    public List<String> fetchApplicationNumbersFromSearcher(InboxSearchCriteria criteria,
            HashMap<String, String> StatusIdNameMap, RequestInfo requestInfo) {
        List<String> applicationNumbers = new ArrayList<>();
        HashMap moduleSearchCriteria = criteria.getModuleSearchCriteria();
        ProcessInstanceSearchCriteria processCriteria = criteria.getProcessSearchCriteria();

        Map<String, Object> searcherRequest = new HashMap<>();
        Map<String, Object> searchCriteria = new HashMap<>();

        searchCriteria.put(TENANT_ID_PARAM, criteria.getTenantId());
        searchCriteria.put(BUSINESS_SERVICE_PARAM, processCriteria.getBusinessService());

        if (moduleSearchCriteria.containsKey(FAMILY_ID_PARAM)) {
            searchCriteria.put(FAMILY_ID_PARAM, moduleSearchCriteria.get(FAMILY_ID_PARAM));
        }

        if (!ObjectUtils.isEmpty(processCriteria.getAssignee())) {
            searchCriteria.put(ASSIGNEE_PARAM, processCriteria.getAssignee());
        }
        if (!ObjectUtils.isEmpty(processCriteria.getStatus())) {
            searchCriteria.put(STATUS_PARAM, processCriteria.getStatus());
        } else {
            if (StatusIdNameMap != null && StatusIdNameMap.values().size() > 0) {
                if (CollectionUtils.isEmpty(processCriteria.getStatus())) {
                    searchCriteria.put(STATUS_PARAM, StatusIdNameMap.keySet());
                }
            }
        }

        searchCriteria.put(OFFSET_PARAM, criteria.getOffset());
        searchCriteria.put(NO_OF_RECORDS_PARAM, criteria.getLimit());
        moduleSearchCriteria.put(LIMIT_PARAM, criteria.getLimit());

        searcherRequest.put(REQUESTINFO_PARAM, requestInfo);
        searcherRequest.put(SEARCH_CRITERIA_PARAM, searchCriteria);

        StringBuilder uri = new StringBuilder();
        if (moduleSearchCriteria.containsKey(SORT_ORDER_PARAM)
                && moduleSearchCriteria.get(SORT_ORDER_PARAM).equals(DESC_PARAM)) {
            uri.append(config.getSearcherHost()).append(config.getFamilyInboxSearcherDescEndpoint());
        } else {
            uri.append(config.getSearcherHost()).append(config.getFamilyInboxSearcherEndpoint());
        }
        log.info("Checking Family searcherRequest: {}", searcherRequest);
        Object result = restTemplate.postForObject(uri.toString(), searcherRequest, Map.class);

        try {
            String jsonString = mapper.writeValueAsString(result);
            applicationNumbers = JsonPath.read(jsonString, "$.families[*].family_id");
            log.info("Family Application Numbers: {}", applicationNumbers);
        } catch (Exception e) {
            log.error("Error while parsing Family searcher results", e);
        }

        return applicationNumbers;
    }

    public Integer fetchApplicationCountFromSearcher(InboxSearchCriteria criteria,
            HashMap<String, String> StatusIdNameMap, RequestInfo requestInfo) {
        Integer totalCount = 0;
        HashMap moduleSearchCriteria = criteria.getModuleSearchCriteria();
        ProcessInstanceSearchCriteria processCriteria = criteria.getProcessSearchCriteria();

        Map<String, Object> searcherRequest = new HashMap<>();
        Map<String, Object> searchCriteria = new HashMap<>();

        searchCriteria.put(TENANT_ID_PARAM, criteria.getTenantId());
        searchCriteria.put(BUSINESS_SERVICE_PARAM, processCriteria.getBusinessService());

        if (moduleSearchCriteria.containsKey(FAMILY_ID_PARAM)) {
            searchCriteria.put(FAMILY_ID_PARAM, moduleSearchCriteria.get(FAMILY_ID_PARAM));
        }

        if (!ObjectUtils.isEmpty(processCriteria.getAssignee())) {
            searchCriteria.put(ASSIGNEE_PARAM, processCriteria.getAssignee());
        }
        if (!ObjectUtils.isEmpty(processCriteria.getStatus())) {
            searchCriteria.put(STATUS_PARAM, processCriteria.getStatus());
        } else {
            if (StatusIdNameMap != null && StatusIdNameMap.values().size() > 0) {
                if (CollectionUtils.isEmpty(processCriteria.getStatus())) {
                    searchCriteria.put(STATUS_PARAM, StatusIdNameMap.keySet());
                }
            }
        }

        searcherRequest.put(REQUESTINFO_PARAM, requestInfo);
        searcherRequest.put(SEARCH_CRITERIA_PARAM, searchCriteria);

        StringBuilder uri = new StringBuilder();
        uri.append(config.getSearcherHost()).append(config.getFamilyInboxSearcherCountEndpoint());

        log.info("Checking Family count searcherRequest: {}", searcherRequest);
        Object result = restTemplate.postForObject(uri.toString(), searcherRequest, Map.class);

        try {
            String jsonString = mapper.writeValueAsString(result);
            double count = JsonPath.read(jsonString, "$.TotalCount[0].count");
            totalCount = (int) count;
        } catch (Exception e) {
            log.error("Error while parsing Family count results", e);
        }
        return totalCount;
    }
}

