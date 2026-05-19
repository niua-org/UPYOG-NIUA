package org.egov.inbox.service.handler;

import java.util.HashMap;
import java.util.List;

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

    default String resolveInternalModuleName(String moduleName) {
        return moduleName;
    }

    default boolean workflowTotalCount() {
        return true;
    }

    default boolean workflowNearingSlaCount() {
        return true;
    }

    default List<HashMap<String, Object>> enrichStatusCountPreFetch(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            java.util.Map<String, List<String>> tenantAndApplnNumbersMap,
            List<String> roles,
            List<String> inputStatuses) {
        return statusCountMap;
    }

    default PostAssembleResult enrichStatusCountPostAssemble(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            List<org.egov.inbox.web.model.Inbox> inboxes,
            Integer totalCount) {
        return new PostAssembleResult(statusCountMap, totalCount);
    }

    class PostAssembleResult {
        public final List<HashMap<String, Object>> statusCountMap;
        public final Integer totalCount;
        public PostAssembleResult(List<HashMap<String, Object>> statusCountMap, Integer totalCount) {
            this.statusCountMap = statusCountMap;
            this.totalCount     = totalCount;
        }
    }
}