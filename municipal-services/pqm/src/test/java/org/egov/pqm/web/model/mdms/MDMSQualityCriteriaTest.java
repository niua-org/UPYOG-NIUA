package org.egov.pqm.web.model.mdms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MDMSQualityCriteriaTest {

    @Test
    void setCode_whenRoundTrip_retainsValue() {
        MDMSQualityCriteria target = new MDMSQualityCriteria();
        target.setCode("test");
        assertThat(target.getCode()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new MDMSQualityCriteria()).isNotNull();
    }
}
