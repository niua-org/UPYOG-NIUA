package org.egov.inbox.service.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.inbox.service.FamilyInboxFilterService;
import org.egov.inbox.service.handler.InboxContext;
import org.egov.inbox.service.handler.ModuleInboxHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.egov.inbox.util.CommonConstants.*;
import static org.egov.inbox.util.FamilyConstants.*;

/**
 * Inbox handler implementation for the Family Service module.
 * <p>
 * Implements {@link ModuleInboxHandler} to coordinate search and filtering of
 * family application records between the Inbox orchestration layer,
 * {@code egov-searcher}, and downstream family microservices.
 * </p>
 */
@Slf4j
@Service
public class FamilyModuleHandler implements ModuleInboxHandler {

    @Autowired
    private FamilyInboxFilterService familyService;

    /**
     * Determines whether this handler supports the specified module.
     *
     * @param moduleName Name of the module from the incoming Inbox request.
     * @return {@code true} if the module name matches {@link FamilyConstants#FAMILY_SERVICE}, {@code false} otherwise.
     */
    @Override
    public boolean supports(String moduleName) {
        return FAMILY_SERVICE.equals(moduleName);
    }

    /**
     * Fetches matching family application IDs from the searcher service and enriches the {@link InboxContext}.
     * <p>
     * Queries {@code egov-searcher} through {@link FamilyInboxFilterService}, populates
     * the criteria with the retrieved IDs under {@code familyIds}, sets business keys,
     * and normalizes status filtering parameters.
     * </p>
     *
     * @param ctx The current Inbox execution context containing search criteria and request metadata.
     */
    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = familyService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria().put(FAMILY_IDS_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    /**
     * Retrieves the total count of family records matching the search criteria.
     * Used for pagination controls, status badge counts, and SLA metrics in the Inbox UI.
     *
     * @param ctx The current Inbox execution context.
     * @return Total count of matching family records.
     */
    @Override
    public int fetchCount(InboxContext ctx) {
        return familyService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    /**
     * Returns the parameter key used by the downstream Family service search API
     * to accept a collection of application / family IDs.
     *
     * @return The parameter name {@link FamilyConstants#FAMILY_IDS_PARAM} ({@code "familyIds"}).
     */
    @Override
    public String getApplicationIdParamKey() {
        return FAMILY_IDS_PARAM;
    }

    /**
     * Specifies the list of request parameters that should be stripped from the
     * criteria map before forwarding queries to the downstream Family service search endpoint.
     *
     * @return List of parameter names to remove: {@code applicationStatus}, {@code locality},
     *         {@code offset}, and {@code familyId}.
     */
    @Override
    public List<String> paramsToRemove() {
        return List.of(STATUS_PARAM, LOCALITY_PARAM, OFFSET_PARAM, FAMILY_ID_PARAM);
    }
}

