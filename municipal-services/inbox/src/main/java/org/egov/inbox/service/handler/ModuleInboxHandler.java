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

    /**
     * Remaps the incoming module name to the internal name used by the pipeline.
     * Only needs to be overridden by handlers that accept an alias
     * (e.g. WSModuleHandler maps bsWs-service → WS, bsSw-service → SW).
     * Default: returns moduleName unchanged.
     */
    default String resolveInternalModuleName(String moduleName) {
        return moduleName;
    }

    /**
     * Return false if this module does not use workflow total count.
     * Default: true.
     */
    default boolean workflowTotalCount() {
        return true;
    }

    /**
     * Return false if this module does not use nearing SLA count.
     * Default: true.
     */
    default boolean workflowNearingSlaCount() {
        return true;
    }

    /**
     * Called before fetchApplicationIds() to let the handler populate
     * any per-tenant application number maps (e.g. BPA citizen multi-tenant).
     *
     * Default: no-op.
     *
     * @param ctx                    shared pipeline context
     * @param statusCountMap         current status count map (may be mutated)
     * @param tenantAndApplnNumbersMap  map to populate with tenantId → applicationNumbers
     * @param roles                  roles of the requesting user
     * @return updated statusCountMap
     */
    default List<HashMap<String, Object>> enrichStatusCountPreFetch(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            java.util.Map<String, List<String>> tenantAndApplnNumbersMap,
            List<String> roles,
            List<String> inputStatuses) {
        return statusCountMap;
    }

    /**
     * Called after inboxes are assembled to let the handler do any
     * post-assembly enrichment (e.g. FSM vehicle trip enrichment).
     *
     * Default: no-op — returns statusCountMap unchanged, totalCount unchanged.
     *
     * @param ctx             shared pipeline context
     * @param statusCountMap  current status count map
     * @param inputStatuses   original input statuses from the request
     * @param inboxes         assembled inbox list (may be mutated)
     * @param totalCount      current total count
     * @return updated [statusCountMap, totalCount] packed as PostAssembleResult
     */
    default PostAssembleResult enrichStatusCountPostAssemble(
            InboxContext ctx,
            List<HashMap<String, Object>> statusCountMap,
            List<String> inputStatuses,
            List<org.egov.inbox.web.model.Inbox> inboxes,
            Integer totalCount) {
        return new PostAssembleResult(statusCountMap, totalCount);
    }

    /** Carries the two mutable outputs of enrichStatusCountPostAssemble(). */
    class PostAssembleResult {
        public final List<HashMap<String, Object>> statusCountMap;
        public final Integer totalCount;
        public PostAssembleResult(List<HashMap<String, Object>> statusCountMap, Integer totalCount) {
            this.statusCountMap = statusCountMap;
            this.totalCount     = totalCount;
        }
    }
}