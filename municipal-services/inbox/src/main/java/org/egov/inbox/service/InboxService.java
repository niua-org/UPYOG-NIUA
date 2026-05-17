package org.egov.inbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.model.vehicle.VehicleSearchCriteria;
import org.egov.inbox.model.vehicle.VehicleTripDetail;
import org.egov.inbox.model.vehicle.VehicleTripDetailResponse;
import org.egov.inbox.model.vehicle.VehicleTripSearchCriteria;
import org.egov.inbox.repository.ServiceRequestRepository;
import org.egov.inbox.service.handler.InboxOrchestrator;
import org.egov.inbox.util.ErrorConstants;
import org.egov.inbox.util.FSMConstants;
import org.egov.inbox.web.model.*;
import org.egov.inbox.web.model.workflow.BusinessService;
import org.egov.inbox.web.model.workflow.State;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import java.util.*;

import static org.egov.inbox.util.FSMConstants.*;

// Entry point for inbox API. Delegates to InboxOrchestrator and exposes
// shared utility methods used by other handler classes.
@Slf4j
@Service
public class InboxService {

    private final InboxConfiguration config;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper mapper;
    private final WorkflowService workflowService;

    @Autowired
    private InboxOrchestrator inboxOrchestrator;

    @Autowired
    public InboxService(InboxConfiguration config,
                        ServiceRequestRepository serviceRequestRepository,
                        ObjectMapper mapper,
                        WorkflowService workflowService) {
        this.config = config;
        this.serviceRequestRepository = serviceRequestRepository;
        this.mapper = mapper;
        this.mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.workflowService = workflowService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main API entry point
    // ─────────────────────────────────────────────────────────────────────────

    public InboxResponse fetchInboxData(
            InboxSearchCriteria criteria,
            RequestInfo requestInfo) {

        log.info("Inbox request received for module: {}",
                criteria.getProcessSearchCriteria().getModuleName());

        return inboxOrchestrator.fetchInboxData(criteria, requestInfo);
    }

    // Returns the service config map for the given business service
    public Map<String, String> fetchAppropriateServiceMapPublic(
            List<String> businessServiceName,
            String moduleName) {

        return fetchAppropriateServiceMap(businessServiceName, moduleName);
    }

    // Calls the module search API and returns the list of business objects
    public JSONArray fetchModuleObjectsPublic(
            HashMap<String, Object> moduleSearchCriteria,
            List<String> businessServiceName,
            String tenantId,
            RequestInfo requestInfo,
            Map<String, String> srvMap) {

        return fetchModuleObjects(
                moduleSearchCriteria,
                businessServiceName,
                tenantId,
                requestInfo,
                srvMap);
    }

    // Used for WS/SW billing amendment to fetch connection objects
    public JSONArray fetchModuleSearchObjectsPublic(
            HashMap<String, Object> moduleSearchCriteria,
            List<String> businessServiceName,
            String tenantId,
            RequestInfo requestInfo,
            Map<String, String> srvSearchMap) {

        return fetchModuleSearchObjects(
                moduleSearchCriteria,
                businessServiceName,
                tenantId,
                requestInfo,
                srvSearchMap);
    }

    // Returns vehicle trip status counts for FSM
    public List<Map<String, Object>> fetchVehicleTripResponsePublic(
            InboxSearchCriteria criteria,
            RequestInfo requestInfo,
            List<String> applicationStatus) {

        VehicleSearchCriteria vehicleTripSearchCriteria = new VehicleSearchCriteria();
        vehicleTripSearchCriteria.setApplicationStatus(applicationStatus);
        vehicleTripSearchCriteria.setTenantId(criteria.getTenantId());
        VehicleCustomResponse vehicleCustomResponse = fetchApplicationCount(vehicleTripSearchCriteria, requestInfo);
        if (vehicleCustomResponse != null && vehicleCustomResponse.getApplicationStatusCount() != null)
            return vehicleCustomResponse.getApplicationStatusCount();
        return new ArrayList<>();
    }

    // Adds vehicle trip entries into the status count map for FSM
    public void populateStatusCountMapPublic(
            List<HashMap<String, Object>> statusCountMap,
            List<Map<String, Object>> vehicleResponse,
            BusinessService businessService) {

        if (CollectionUtils.isEmpty(vehicleResponse) || businessService == null) return;
        for (State appState : businessService.getStates()) {
            vehicleResponse.forEach(trip -> {
                HashMap<String, Object> map = new HashMap<>();
                if (trip.get(APPLICATIONSTATUS).equals(appState.getApplicationStatus())) {
                    map.put(COUNT, trip.get(COUNT));
                    map.put(APPLICATIONSTATUS, appState.getApplicationStatus());
                    map.put(STATUSID, appState.getUuid());
                    map.put(FSMConstants.BUSINESS_SERVICE_PARAM, FSM_VEHICLE_TRIP_MODULE);
                }
                if (!map.isEmpty()) statusCountMap.add(map);
            });
        }
    }

    // Returns vehicle trip details for the given FSM application numbers
    public List<VehicleTripDetail> fetchVehicleStatusForApplicationPublic(
            List<String> requiredApplications,
            RequestInfo requestInfo,
            String tenantId) {

        VehicleTripSearchCriteria criteria = new VehicleTripSearchCriteria();
        criteria.setApplicationNos(requiredApplications);
        criteria.setTenantId(tenantId);
        return fetchVehicleTripDetailsByReferenceNo(criteria, requestInfo);
    }

    // Returns FSM application IDs filtered by vehicle trip status
    public List<String> fetchVehicleStateMap(
            List<String> inputStatuses,
            RequestInfo requestInfo,
            String tenantId,
            Integer limit,
            Integer offSet) {

        VehicleTripSearchCriteria vehicleTripSearchCriteria =
                new VehicleTripSearchCriteria();
        vehicleTripSearchCriteria.setApplicationStatus(inputStatuses);
        vehicleTripSearchCriteria.setTenantId(tenantId);
        vehicleTripSearchCriteria.setLimit(limit);
        vehicleTripSearchCriteria.setOffset(offSet);

        StringBuilder url = new StringBuilder(config.getFsmHost());
        url.append(config.getFetchApplicationIds());

        Object result = serviceRequestRepository.fetchResult(
                url, vehicleTripSearchCriteria);

        VehicleCustomResponse response;
        try {
            response = mapper.convertValue(result, VehicleCustomResponse.class);
            if (response != null && response.getApplicationIdList() != null) {
                return response.getApplicationIdList();
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorConstants.PARSING_ERROR,
                    "Failed to parse response of ProcessInstance");
        }
        return new ArrayList<>();
    }

    // Fetches vehicle trip details by application reference numbers
    public List<VehicleTripDetail> fetchVehicleTripDetailsByReferenceNo(
            VehicleTripSearchCriteria vehicleTripSearchCriteria,
            RequestInfo requestInfo) {

        StringBuilder url = new StringBuilder(config.getVehicleHost());
        url.append(config.getVehicleSearchTripPath());

        Object result = serviceRequestRepository.fetchResult(
                url, vehicleTripSearchCriteria);

        VehicleTripDetailResponse response;
        try {
            response = mapper.convertValue(result, VehicleTripDetailResponse.class);
            if (response != null && response.getVehicleTripDetail() != null) {
                return response.getVehicleTripDetail();
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorConstants.PARSING_ERROR,
                    "Failed to parse response of ProcessInstance");
        }
        return new ArrayList<>();
    }

    // Returns vehicle application status count from FSM vehicle service
    public VehicleCustomResponse fetchApplicationCount(
            VehicleSearchCriteria criteria,
            RequestInfo requestInfo) {

        StringBuilder url = new StringBuilder(config.getVehicleHost());
        url.append(config.getVehicleApplicationStatusCountPath());

        Object result = serviceRequestRepository.fetchResult(url, criteria);

        try {
            return mapper.convertValue(result, VehicleCustomResponse.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorConstants.PARSING_ERROR,
                    "Failed to parse response of ProcessInstance");
        }
    }

    // Converts JSONObject to Map
    public static Map<String, Object> toMap(JSONObject object) throws JSONException {

        Map<String, Object> map = new HashMap<>();

        if (object == null) {
            return map;
        }

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }

            map.put(key, value);
        }

        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {

        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }

            list.add(value);
        }

        return list;
    }

    private Map<String, String> fetchAppropriateServiceMap(
            List<String> businessServiceName,
            String moduleName) {

        StringBuilder appropriateKey = new StringBuilder();

        for (String businessServiceKeys :
                config.getServiceSearchMapping().keySet()) {

            if (businessServiceKeys.contains(
                    businessServiceName.get(0))) {
                appropriateKey.append(businessServiceKeys);
                break;
            }
        }

        if (ObjectUtils.isEmpty(appropriateKey)) {
            throw new CustomException(
                    "EG_INBOX_SEARCH_ERROR",
                    "Inbox service is not configured for the provided business services");
        }

        for (String inputBusinessService : businessServiceName) {
            if (!FSMConstants.FSM_MODULE.equalsIgnoreCase(moduleName)) {
                if (!appropriateKey.toString().contains(inputBusinessService)) {
                    throw new CustomException(
                            "EG_INBOX_SEARCH_ERROR",
                            "Cross module search is NOT allowed.");
                }
            }
        }

        return config.getServiceSearchMapping().get(appropriateKey.toString());
    }

    private JSONArray fetchModuleObjects(
            HashMap<String, Object> moduleSearchCriteria,
            List<String> businessServiceName,
            String tenantId,
            RequestInfo requestInfo,
            Map<String, String> srvMap) {

        if (CollectionUtils.isEmpty(srvMap)
                || StringUtils.isEmpty(srvMap.get("searchPath"))) {
            throw new CustomException(
                    ErrorConstants.INVALID_MODULE_SEARCH_PATH,
                    "search path not configured for the businessService : "
                            + businessServiceName);
        }

        StringBuilder url = new StringBuilder(srvMap.get("searchPath"));
        url.append("?tenantId=").append(tenantId);

        // pet-service: remove status param
        if (moduleSearchCriteria.containsKey("status")
                && businessServiceName.contains("ptr")) {
            moduleSearchCriteria.remove("status");
        }

        // asset-service: reset offset
        if (businessServiceName.contains("asset-create")
                && moduleSearchCriteria.containsKey("offset")) {
            moduleSearchCriteria.put("offset", 0);
        }

        Set<String> searchParams = moduleSearchCriteria.keySet();
        searchParams.forEach(param -> {

            if (param.equalsIgnoreCase("tenantId")) {
                return;
            }

            if (moduleSearchCriteria.get(param) instanceof Collection) {
                url.append("&").append(param).append("=");
                url.append(StringUtils.arrayToDelimitedString(
                        ((Collection<?>) moduleSearchCriteria.get(param)).toArray(),
                        ","));

            } else if (param.equalsIgnoreCase("appStatus")) {
                url.append("&").append("applicationStatus").append("=")
                        .append(moduleSearchCriteria.get(param));

            } else if (param.equalsIgnoreCase("consumerNo")) {
                url.append("&").append("connectionNumber").append("=")
                        .append(moduleSearchCriteria.get(param));

            } else if (moduleSearchCriteria.get(param) != null) {
                url.append("&").append(param).append("=")
                        .append(moduleSearchCriteria.get(param));
            }
        });

        log.info("\nfetchModuleObjects URL :::: {}", url);

        RequestInfoWrapper requestInfoWrapper =
                RequestInfoWrapper.builder().requestInfo(requestInfo).build();

        Object result = serviceRequestRepository.fetchResult(url, requestInfoWrapper);

        LinkedHashMap responseMap;
        try {
            responseMap = mapper.convertValue(result, LinkedHashMap.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorConstants.PARSING_ERROR,
                    "Failed to parse response of ProcessInstance Count");
        }

        JSONObject jsonObject = new JSONObject(responseMap);

        try {
            return jsonObject.getJSONArray(srvMap.get("dataRoot"));
        } catch (Exception e) {
            throw new CustomException(
                    ErrorConstants.INVALID_MODULE_DATA,
                    "search api could not find data in dataroot "
                            + srvMap.get("dataRoot"));
        }
    }

    private JSONArray fetchModuleSearchObjects(
            HashMap<String, Object> moduleSearchCriteria,
            List<String> businessServiceName,
            String tenantId,
            RequestInfo requestInfo,
            Map<String, String> srvMap) {

        if (CollectionUtils.isEmpty(srvMap)
                || StringUtils.isEmpty(srvMap.get("searchPath"))) {
            throw new CustomException(
                    ErrorConstants.INVALID_MODULE_SEARCH_PATH,
                    "search path not configured for the businessService : "
                            + businessServiceName);
        }

        StringBuilder url = new StringBuilder(srvMap.get("searchPath"));
        url.append("?tenantId=").append(tenantId);

        Set<String> searchParams = moduleSearchCriteria.keySet();
        searchParams.forEach(param -> {

            if (param.equalsIgnoreCase("tenantId")
                    || param.equalsIgnoreCase("limit")) {
                return;
            }

            if (moduleSearchCriteria.get(param) instanceof Collection) {
                url.append("&").append(param).append("=");
                url.append(StringUtils.arrayToDelimitedString(
                        ((Collection<?>) moduleSearchCriteria.get(param)).toArray(),
                        ","));
            } else if (moduleSearchCriteria.get(param) != null) {
                url.append("&").append(param).append("=")
                        .append(moduleSearchCriteria.get(param));
            }
        });

        log.info("\nfetchModuleSearchObjects URL :::: {}", url);

        RequestInfoWrapper requestInfoWrapper =
                RequestInfoWrapper.builder().requestInfo(requestInfo).build();

        Object result = serviceRequestRepository.fetchResult(url, requestInfoWrapper);

        LinkedHashMap responseMap;
        try {
            responseMap = mapper.convertValue(result, LinkedHashMap.class);
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorConstants.PARSING_ERROR,
                    "Failed to parse response of ProcessInstance Count");
        }

        JSONObject jsonObject = new JSONObject(responseMap);

        try {
            return jsonObject.getJSONArray(srvMap.get("dataRoot"));
        } catch (Exception e) {
            throw new CustomException(
                    ErrorConstants.INVALID_MODULE_DATA,
                    "search api could not find data in serviceMap "
                            + srvMap.get("dataRoot"));
        }
    }


}