package org.egov.inbox.service.handler;

import lombok.Builder;
import lombok.Data;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.web.model.InboxSearchCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Holds all request-scoped data that flows through the inbox pipeline.
// Passed between InboxService, ModuleHandlers and InboxAssembler.
@Data
@Builder
public class InboxContext {

    private InboxSearchCriteria criteria;
    private RequestInfo requestInfo;
    private HashMap<String, String> statusIdNameMap;

    // service config map for the current business service
    private Map<String, String> srvMap;

    // application IDs fetched by the module handler, used to maintain order in InboxAssembler
    @Builder.Default
    private List<String> businessKeys = new ArrayList<>();

    // set to true when searcher returns no results, InboxAssembler returns empty list immediately
    @Builder.Default
    private Boolean searchResultEmpty = Boolean.FALSE;

    // bsFlag: 0 = not billing, 1 = WS billing, 2 = SW billing (set by WSModuleHandler)
    @Builder.Default
    private int bsFlag = 0;

    // total application count, set by module handler or ElasticSearch
    private Integer totalCount;

    public boolean isSearchResultEmpty() {
        return Boolean.TRUE.equals(this.searchResultEmpty);
    }

    public void setSearchResultEmpty(boolean empty) {
        this.searchResultEmpty = empty;
    }

    public List<String> getBusinessKeys() {
        if (this.businessKeys == null) this.businessKeys = new ArrayList<>();
        return this.businessKeys;
    }

    public void addBusinessKey(String key) {
        getBusinessKeys().add(key);
    }

    public void addBusinessKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        getBusinessKeys().addAll(keys);
    }

    public void addModuleSearchCriteria(String key, Object value) {
        if (this.criteria.getModuleSearchCriteria() == null)
            this.criteria.setModuleSearchCriteria(new HashMap<>());
        this.criteria.getModuleSearchCriteria().put(key, value);
    }

    public void removeModuleSearchCriteria(String key) {
        if (this.criteria == null || this.criteria.getModuleSearchCriteria() == null) return;
        this.criteria.getModuleSearchCriteria().remove(key);
    }
}
