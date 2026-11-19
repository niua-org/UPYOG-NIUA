package org.egov.garbageservice.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchResponseContentTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        UserSearchResponseContent target = new UserSearchResponseContent();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new UserSearchResponseContent()).isNotNull();
    }
}
