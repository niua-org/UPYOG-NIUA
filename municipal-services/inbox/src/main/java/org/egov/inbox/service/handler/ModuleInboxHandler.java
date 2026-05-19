package org.egov.inbox.service.handler;

import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.service.WorkflowService;
import org.egov.inbox.web.model.Inbox;
import org.egov.inbox.web.model.workflow.ProcessInstanceResponse;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface ModuleInboxHandler {

    boolean supports(String moduleName);

    void fetchApplicationIds(InboxContext ctx);

    default int fetchCount(InboxContext ctx) {
        return -1;
    }

    String getApplicationIdParamKey();

    default List<String> paramsToRemove() {
        return List.of();
    }

    default String getInternalModuleName(String moduleName) {
        return moduleName;
    }

    default boolean isWorkflowTotalCountRequired() {
        return true;
    }

    default boolean isWorkflowNearingSlaCountRequired() {
        return true;
    }

    default List<HashMap<String, Object>> enrichStatusCountPreFetch(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<String> roles,
            List<String> inputStatuses) {
        return statusCountMap;
    }

    default PostAssembleResult enrichStatusCountPostAssemble(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            List<Inbox> inboxes,
            Integer totalCount) {
        return new PostAssembleResult(statusCountMap, totalCount);
    }

    default ProcessInstanceResponse getProcessInstances(
            ProcessInstanceSearchCriteria processCriteria,
            RequestInfo requestInfo,
            WorkflowService workflowService,
            Map<String, List<String>> tenantAndApplnNumbersMap,
            List<Object> businessIds,
            List<String> roles) {

        return workflowService.getProcessInstance(processCriteria, requestInfo);
    }

    default Map<String, Object> buildBusinessMap(
            JSONArray businessObjects,
            String businessIdParam) {

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

    class PostAssembleResult {
        public final List<HashMap<String, Object>> statusCountMap;
        public final Integer totalCount;

        public PostAssembleResult(List<HashMap<String, Object>> statusCountMap, Integer totalCount) {
            this.statusCountMap = statusCountMap;
            this.totalCount = totalCount;
        }
    }
}