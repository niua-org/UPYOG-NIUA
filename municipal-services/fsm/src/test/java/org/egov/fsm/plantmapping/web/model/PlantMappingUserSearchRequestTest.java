package org.egov.fsm.plantmapping.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantMappingUserSearchRequestTest {

    @Test
    void setRequestInfo_whenRoundTrip_retainsValue() {
        PlantMappingUserSearchRequest target = new PlantMappingUserSearchRequest();
        target.setRequestInfo(new org.egov.common.contract.request.RequestInfo());
        assertThat(target.getRequestInfo()).isEqualTo(new org.egov.common.contract.request.RequestInfo());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new PlantMappingUserSearchRequest()).isNotNull();
    }
}
