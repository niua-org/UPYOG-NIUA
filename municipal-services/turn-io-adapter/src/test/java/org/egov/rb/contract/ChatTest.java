package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTest {

    @Test
    void setAssigned_to_whenRoundTrip_retainsValue() {
        Chat target = new Chat();
        target.setAssigned_to("test");
        assertThat(target.getAssigned_to()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Chat()).isNotNull();
    }
}
