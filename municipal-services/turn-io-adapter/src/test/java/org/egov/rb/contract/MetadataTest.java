package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataTest {

    @Test
    void setNlu_whenRoundTrip_retainsValue() {
        Metadata target = new Metadata();
        target.setNlu(null);
        assertThat(target.getNlu()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Metadata()).isNotNull();
    }
}
