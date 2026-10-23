package org.egov.swservice.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionTest {

    @Test
    void setConnectionHolders_whenRoundTrip_retainsValue() {
        Connection target = new Connection();
        target.setConnectionHolders(java.util.Collections.emptyList());
        assertThat(target.getConnectionHolders()).isEqualTo(java.util.Collections.emptyList());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Connection()).isNotNull();
    }
}
