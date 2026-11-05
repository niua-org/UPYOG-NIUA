package org.egov.pqm.anomaly.finder.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        Document target = new Document();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Document()).isNotNull();
    }
}
