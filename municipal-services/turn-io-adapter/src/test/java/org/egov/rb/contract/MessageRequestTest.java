package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRequestTest {

    @Test
    void setContacts_whenRoundTrip_retainsValue() {
        MessageRequest target = new MessageRequest();
        target.setContacts(java.util.Collections.emptyList());
        assertThat(target.getContacts()).isEqualTo(java.util.Collections.emptyList());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new MessageRequest()).isNotNull();
    }
}
