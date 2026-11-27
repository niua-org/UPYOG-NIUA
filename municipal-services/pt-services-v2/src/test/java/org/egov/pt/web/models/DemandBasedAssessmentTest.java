package org.egov.pt.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandBasedAssessmentTest {

    @Test
    void setAssessmentNumber_whenRoundTrip_retainsValue() {
        DemandBasedAssessment target = new DemandBasedAssessment();
        target.setAssessmentNumber("test");
        assertThat(target.getAssessmentNumber()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DemandBasedAssessment()).isNotNull();
    }
}
