package org.egov.pt.models.oldProperty;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OldPropertyTest {

    @Test
    void setOldAuditDetails_whenRoundTrip_retainsValue() {
        OldProperty target = new OldProperty();
        target.setOldAuditDetails(null);
        assertThat(target.getOldAuditDetails()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new OldProperty()).isNotNull();
    }
}
