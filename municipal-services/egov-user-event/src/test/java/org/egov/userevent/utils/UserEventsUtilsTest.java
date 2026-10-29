package org.egov.userevent.utils;

import org.egov.userevent.web.contract.EventSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventsUtilsTest {

    private final UserEventsUtils utils = new UserEventsUtils();

    @Test
    void buildRecepientListForSearch_addsAllKeyword() {
        EventSearchCriteria criteria = new EventSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        utils.buildRecepientListForSearch(criteria);
        assertThat(criteria.getRecepients()).contains(UserEventsConstants.ALL_KEYWORD);
    }

    @Test
    void buildRecepientListForSearch_withRole_expandsPatterns() {
        EventSearchCriteria criteria = new EventSearchCriteria();
        criteria.setRoles(List.of("CITIZEN.EMPLOYEE"));
        criteria.setTenantId("pb");
        utils.buildRecepientListForSearch(criteria);
        assertThat(criteria.getRecepients()).anyMatch(r -> r.contains("CITIZEN"));
    }
}
