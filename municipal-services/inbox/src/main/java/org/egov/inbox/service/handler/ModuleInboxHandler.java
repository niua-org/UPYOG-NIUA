package org.egov.inbox.service.handler;

import java.util.List;

/**
 * ModuleInboxHandler — strategy interface for per-module inbox handling.
 *
 * One implementation per module (TL, BPA, PT, CHB, NDC, CND, ...).
 * All implementations live in ModuleHandlers.java and are auto-registered
 * via ModuleHandlerRegistry at Spring startup.
 *
 * ─── How to add a NEW module ──────────────────────────────────────────────
 *  1. Create a class in this package implementing ModuleInboxHandler.
 *  2. Annotate with @Service.
 *  3. Implement supports(), fetchApplicationIds(), getApplicationIdParamKey().
 *  4. Override fetchCount() and paramsToRemove() if needed.
 *  5. Done — InboxOrchestrator needs NO changes.
 * ──────────────────────────────────────────────────────────────────────────
 *
 * ─── Contract ─────────────────────────────────────────────────────────────
 *  fetchApplicationIds() MUST do one of:
 *    a) Populate ctx.businessKeys AND ctx.moduleSearchCriteria with fetched IDs
 *    b) Call ctx.setSearchResultEmpty(true) when searcher returns no results
 *
 *  fetchCount() MUST return:
 *    - A non-negative integer (actual count) to override workflow count
 *    - -1 to fall back to the workflow process count (default)
 * ──────────────────────────────────────────────────────────────────────────
 */
public interface ModuleInboxHandler {

    /**
     * Returns true if this handler supports the given module name.
     *
     * Called by ModuleHandlerRegistry to find the right handler.
     *
     * Example:
     *   "TL"  → TLModuleHandler
     *   "BPA" → BPAModuleHandler
     *   "PT"  → PTModuleHandler
     *
     * @param moduleName  module name from processCriteria
     * @return true if this handler owns the given module
     */
    boolean supports(String moduleName);

    /**
     * Fetch application IDs from the module's searcher service.
     *
     * Must populate ctx.businessKeys with fetched IDs.
     * Must call ctx.setSearchResultEmpty(true) if no results found.
     * Must put fetched IDs into ctx.criteria.moduleSearchCriteria
     * under the appropriate param key.
     *
     * @param ctx  shared pipeline context for this request
     */
    void fetchApplicationIds(InboxContext ctx);

    /**
     * Fetch the total application count from the module's searcher service.
     *
     * Override this when the module has its own count API
     * (e.g. TL, BPA, NOC, NDC, PT, PGR AI).
     *
     * Return -1 to skip and use the workflow process count instead.
     * Default implementation returns -1 (fall back to workflow count).
     *
     * @param ctx  shared pipeline context for this request
     * @return total count, or -1 to use workflow count
     */
    default int fetchCount(InboxContext ctx) {
        return -1;
    }

    /**
     * The moduleSearchCriteria key under which fetched application IDs
     * are stored.
     *
     * Examples:
     *   TL    → "applicationNumber"
     *   BPA   → "applicationNumber"  (BPA_APPLICATION_NUMBER_PARAM)
     *   PT    → "acknowledgementIds"
     *   CHB   → "bookingNo"
     *   WT/MT → "bookingNo"
     *
     * @return the param key string
     */
    String getApplicationIdParamKey();

    /**
     * List of moduleSearchCriteria keys to remove after IDs are fetched.
     *
     * These are params that were used for filtering in the searcher
     * but should not be forwarded to the module search API call.
     *
     * Common removals: "locality", "offset", "status", "mobileNumber"
     *
     * Default implementation removes nothing — override as needed.
     *
     * @return list of keys to remove from moduleSearchCriteria
     */
    default List<String> paramsToRemove() {
        return List.of();
    }
}