package org.egov.inbox.service.handler;

import lombok.Builder;
import lombok.Data;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.web.model.InboxSearchCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InboxContext {

    private InboxSearchCriteria criteria;
    private RequestInfo requestInfo;
    private HashMap<String, String> statusIdNameMap;

    private Map<String, String> srvMap;

    @Builder.Default
    private List<String> businessKeys = new ArrayList<>();

    @Builder.Default
    private Boolean searchResultEmpty = Boolean.FALSE;

    private Integer totalCount;

    public boolean isSearchResultEmpty() {
        return Boolean.TRUE.equals(this.searchResultEmpty);
    }

    public void setSearchResultEmpty(boolean empty) {
        this.searchResultEmpty = empty;
    }

    public List<String> getBusinessKeys() {
        if (this.businessKeys == null)
            this.businessKeys = new ArrayList<>();
        return this.businessKeys;
    }

    public void addBusinessKey(String key) {
        getBusinessKeys().add(key);
    }

    public void addBusinessKeys(List<String> keys) {
        if (keys == null || keys.isEmpty())
            return;
        getBusinessKeys().addAll(keys);
    }

    public void addModuleSearchCriteria(String key, Object value) {
        if (this.criteria.getModuleSearchCriteria() == null)
            this.criteria.setModuleSearchCriteria(new HashMap<>());
        this.criteria.getModuleSearchCriteria().put(key, value);
    }

    public void removeModuleSearchCriteria(String key) {
        if (this.criteria == null || this.criteria.getModuleSearchCriteria() == null)
            return;
        this.criteria.getModuleSearchCriteria().remove(key);
    }
}
