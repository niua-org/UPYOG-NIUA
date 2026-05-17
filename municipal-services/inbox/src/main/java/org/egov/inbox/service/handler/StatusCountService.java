package org.egov.inbox.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.service.BPAInboxFilterService;
import org.egov.inbox.service.InboxService;
import org.egov.inbox.service.WorkflowService;
import org.egov.inbox.model.vehicle.VehicleTripDetail;
import org.egov.inbox.util.FSMConstants;
import org.egov.inbox.web.model.Inbox;
import org.egov.inbox.web.model.InboxSearchCriteria;
import org.egov.inbox.web.model.workflow.BusinessService;
import org.egov.inbox.web.model.workflow.ProcessInstance;
import org.egov.inbox.web.model.workflow.ProcessInstanceResponse;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.egov.inbox.util.BpaConstants.BPA;
import static org.egov.inbox.util.BpaConstants.CITIZEN;
import static org.egov.inbox.util.BpaConstants.LOCALITY_PARAM;
import static org.egov.inbox.util.BpaConstants.STATUS_ID;
import static org.egov.inbox.util.FSMConstants.APPLICATIONSTATUS;
import static org.egov.inbox.util.FSMConstants.CITIZEN_FEEDBACK_PENDING_STATE;
import static org.egov.inbox.util.FSMConstants.COMPLETED_STATE;
import static org.egov.inbox.util.FSMConstants.COUNT;
import static org.egov.inbox.util.FSMConstants.DISPOSED_STATE;
import static org.egov.inbox.util.FSMConstants.DSO_INPROGRESS_STATE;
import static org.egov.inbox.util.FSMConstants.FSM_VEHICLE_TRIP_MODULE;
import static org.egov.inbox.util.FSMConstants.STATUSID;
import static org.egov.inbox.util.FSMConstants.VEHICLE_LOG;
import static org.egov.inbox.util.FSMConstants.WAITING_FOR_DISPOSAL_STATE;
import static org.egov.inbox.util.TLConstants.BUSINESS_SERVICE_PARAM;

@Slf4j
@Service
public class StatusCountService {

    // ─────────────────────────────────────────────────────────────────────────
    // BPA Citizen — multi-tenant status count
    // ─────────────────────────────────────────────────────────────────────────

    public List<HashMap<String, Object>> handleBpaCitizenStatusCount(
            InboxSearchCriteria criteria,
            ProcessInstanceSearchCriteria processCriteria,
            HashMap<String, String> statusIdNameMap,
            List<HashMap<String, Object>> statusCountMap,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<String> roles,
            RequestInfo requestInfo,
            WorkflowService workflowService) {

        // Only run for BPA citizen role
        if (ObjectUtils.isEmpty(processCriteria.getModuleName())
                || !BPA.equals(processCriteria.getModuleName())
                || !roles.contains(CITIZEN)) {
            return statusCountMap;
        }

        if (tenantAndApplnNumbersMap.isEmpty()) {
            return statusCountMap;
        }

        List<HashMap<String, Object>> bpaCitizenMap = new ArrayList<>();

        // Snapshot and restore processCriteria state
        String savedTenant   = processCriteria.getTenantId();
        List<String> savedBizIds = processCriteria.getBusinessIds();
        List<String> savedStatus = processCriteria.getStatus();

        if (!statusIdNameMap.isEmpty()) {
            processCriteria.setStatus(
                    new ArrayList<>(statusIdNameMap.keySet()));
        }

        for (Map.Entry<String, List<String>> entry
                : tenantAndApplnNumbersMap.entrySet()) {

            processCriteria.setTenantId(entry.getKey());
            processCriteria.setBusinessIds(entry.getValue());

            List<HashMap<String, Object>> tenantCount =
                    workflowService.getProcessStatusCount(
                            requestInfo, processCriteria);

            if (bpaCitizenMap.isEmpty()) {
                bpaCitizenMap.addAll(tenantCount);
            } else {
                for (HashMap<String, Object> tenantMap : tenantCount) {
                    for (HashMap<String, Object> citizenMap : bpaCitizenMap) {
                        if (citizenMap.containsValue(
                                tenantMap.get(STATUS_ID))) {
                            citizenMap.put(
                                    COUNT,
                                    Integer.parseInt(String.valueOf(
                                            citizenMap.get(COUNT)))
                                            + Integer.parseInt(String.valueOf(
                                            tenantMap.get(COUNT))));
                        }
                    }
                }
            }
        }

        // Restore
        processCriteria.setTenantId(savedTenant);
        processCriteria.setBusinessIds(savedBizIds);
        processCriteria.setStatus(savedStatus);

        return bpaCitizenMap.isEmpty() ? statusCountMap : bpaCitizenMap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BPA Locality — locality-based status count filter
    // ─────────────────────────────────────────────────────────────────────────

    public List<HashMap<String, Object>> handleBpaLocalityStatusCount(
            InboxSearchCriteria criteria,
            ProcessInstanceSearchCriteria processCriteria,
            HashMap<String, String> statusIdNameMap,
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            BPAInboxFilterService bpaService,
            RequestInfo requestInfo) {

        if (ObjectUtils.isEmpty(processCriteria.getModuleName())
                || !BPA.equals(processCriteria.getModuleName())) {
            return statusCountMap;
        }

        HashMap<String, Object> moduleSearchCriteria =
                criteria.getModuleSearchCriteria();

        if (moduleSearchCriteria.get(LOCALITY_PARAM) != null) {

            for (Map<String, Object> statusWiseCount : statusCountMap) {

                List<String> statusList = Collections.singletonList(
                        String.valueOf(statusWiseCount.get(STATUS_ID)));

                criteria.getProcessSearchCriteria().setStatus(statusList);

                Integer count = bpaService.fetchApplicationCountFromSearcher(
                        criteria, statusIdNameMap, requestInfo);

                if (count == 0) {
                    statusWiseCount.clear();
                } else {
                    statusWiseCount.put(COUNT, count);
                }
            }

            // Restore input statuses after locality-based count
            criteria.getProcessSearchCriteria().setStatus(inputStatuses);
        }

        return statusCountMap.stream()
                .filter(map -> !map.isEmpty())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FSM — vehicle trip status count enrichment + inbox enrichment
    // ─────────────────────────────────────────────────────────────────────────

    public List<HashMap<String, Object>> enrichFsmStatusCount(
            InboxSearchCriteria criteria,
            RequestInfo requestInfo,
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            List<Inbox> inboxes,
            HashMap<String, Object> moduleSearchCriteria,
            ProcessInstanceSearchCriteria processCriteria,
            List<String> businessServiceName,
            Map<String, String> srvMap,
            InboxService inboxService,
            WorkflowService workflowService) {

        // ── Fetch vehicle trip status counts ──────────────────────────────────
        List<String> appStatus = Arrays.asList(
                WAITING_FOR_DISPOSAL_STATE,
                DISPOSED_STATE);

        List<Map<String, Object>> vehicleResponse =
                inboxService.fetchVehicleTripResponsePublic(
                        criteria, requestInfo, appStatus);

        BusinessService businessService =
                workflowService.getBusinessService(
                        criteria.getTenantId(),
                        requestInfo,
                        FSM_VEHICLE_TRIP_MODULE);

        inboxService.populateStatusCountMapPublic(
                statusCountMap, vehicleResponse, businessService);

        // ── Enrich inboxes with vehicle trip details ──────────────────────────
        List<String> requiredApps = inboxes.stream()
                .filter(inbox ->
                        inbox.getProcessInstance() != null
                                && inbox.getProcessInstance().getState() != null)
                .filter(inbox -> {
                    String state = inbox.getProcessInstance()
                            .getState().getApplicationStatus();
                    return DSO_INPROGRESS_STATE.equals(state)
                            || CITIZEN_FEEDBACK_PENDING_STATE.equals(state)
                            || COMPLETED_STATE.equals(state);
                })
                .map(inbox -> inbox.getProcessInstance().getBusinessId())
                .collect(Collectors.toList());

        List<VehicleTripDetail> tripDetails =
                inboxService.fetchVehicleStatusForApplicationPublic(
                        requiredApps, requestInfo, criteria.getTenantId());

        inboxes.forEach(inbox -> {
            if (inbox.getProcessInstance() != null
                    && inbox.getProcessInstance().getBusinessId() != null) {

                List<VehicleTripDetail> trips = tripDetails.stream()
                        .filter(trip ->
                                inbox.getProcessInstance()
                                        .getBusinessId()
                                        .equals(trip.getReferenceNo()))
                        .collect(Collectors.toList());

                inbox.getBusinessObject().put(VEHICLE_LOG, trips);
            }
        });

        // ── FSM vehicle-only inbox (no FSM apps, but vehicle trips exist) ─────
        if (CollectionUtils.isEmpty(inboxes)
                && !moduleSearchCriteria.containsKey("applicationNos")) {

            List<String> fsmApplications =
                    inboxService.fetchVehicleStateMap(
                            inputStatuses.stream()
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()),
                            requestInfo,
                            criteria.getTenantId(),
                            criteria.getLimit(),
                            criteria.getOffset());

            moduleSearchCriteria.put("applicationNos", fsmApplications);
            moduleSearchCriteria.put("applicationStatus", requiredApps);

            processCriteria.setBusinessIds(fsmApplications);
            processCriteria.setStatus(null);

            ProcessInstanceResponse processResponse =
                    workflowService.getProcessInstance(
                            processCriteria, requestInfo);

            Map<String, ProcessInstance> processMap =
                    processResponse.getProcessInstances().stream()
                            .collect(Collectors.toMap(
                                    ProcessInstance::getBusinessId,
                                    Function.identity()));

            JSONArray vehicleObjects = inboxService.fetchModuleObjectsPublic(
                    moduleSearchCriteria,
                    businessServiceName,
                    criteria.getTenantId(),
                    requestInfo,
                    srvMap);

            String businessIdParam = srvMap.get("businessIdProperty");

            Map<String, Object> vehicleBusinessMap =
                    StreamSupport.stream(
                                    vehicleObjects.spliterator(), false)
                            .collect(Collectors.toMap(
                                    obj -> ((JSONObject) obj)
                                            .get(businessIdParam).toString(),
                                    obj -> obj,
                                    (e1, e2) -> e1,
                                    LinkedHashMap::new));

            if (vehicleObjects.length() > 0
                    && !processResponse.getProcessInstances().isEmpty()) {

                fsmApplications.forEach(key -> {
                    Inbox inbox = new Inbox();
                    inbox.setProcessInstance(processMap.get(key));
                    inbox.setBusinessObject(
                            InboxService.toMap(
                                    (JSONObject) vehicleBusinessMap.get(key)));
                    inboxes.add(inbox);
                });
            }
        }

        return aggregateFsmStatuses(statusCountMap);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FSM — total count from vehicle trip statuses
    // ─────────────────────────────────────────────────────────────────────────

    public Integer getFsmTotalCount(
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            Integer current) {

        int extra = 0;

        for (HashMap<String, Object> map : statusCountMap) {
            if ((WAITING_FOR_DISPOSAL_STATE.equals(map.get(APPLICATIONSTATUS))
                    || DISPOSED_STATE.equals(map.get(APPLICATIONSTATUS)))
                    && inputStatuses.contains(map.get(STATUSID))) {
                extra += (int) map.get(COUNT);
            }
        }

        return current + extra;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FSM — aggregate duplicate application statuses
    // (FSM has pay-now and post-pay which share same applicationStatus)
    // ─────────────────────────────────────────────────────────────────────────

    private List<HashMap<String, Object>> aggregateFsmStatuses(
            List<HashMap<String, Object>> statusCountMap) {

        List<HashMap<String, Object>> aggregated = new ArrayList<>();

        for (HashMap<String, Object> entry : statusCountMap) {

            boolean matchFound = false;
            HashMap<String, Object> temp = new HashMap<>();

            for (HashMap<String, Object> agg : aggregated) {

                String entryStatus =
                        (String) entry.get(APPLICATIONSTATUS);
                String aggStatus =
                        (String) agg.get(APPLICATIONSTATUS);

                if (aggStatus != null
                        && aggStatus.equalsIgnoreCase(entryStatus)) {

                    agg.put(COUNT,
                            (Integer) entry.get(COUNT)
                                    + (Integer) agg.get(COUNT));

                    agg.put(APPLICATIONSTATUS,
                            entry.get(APPLICATIONSTATUS));

                    agg.put(BUSINESS_SERVICE_PARAM,
                            entry.get(BUSINESS_SERVICE_PARAM)
                                    + ","
                                    + agg.get(BUSINESS_SERVICE_PARAM));

                    agg.put(STATUSID,
                            entry.get(STATUSID)
                                    + ","
                                    + agg.get(STATUSID));

                    matchFound = true;
                    break;

                } else {
                    temp.put(COUNT, entry.get(COUNT));
                    temp.put(APPLICATIONSTATUS,
                            entry.get(APPLICATIONSTATUS));
                    temp.put(BUSINESS_SERVICE_PARAM,
                            entry.get(BUSINESS_SERVICE_PARAM));
                    temp.put(STATUSID, entry.get(STATUSID));
                }
            }

            if (aggregated.isEmpty()) {
                aggregated.add(entry);
            } else if (!matchFound) {
                aggregated.add(temp);
            }
        }

        return aggregated;
    }
}