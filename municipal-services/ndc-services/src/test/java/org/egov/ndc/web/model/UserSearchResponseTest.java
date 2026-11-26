package org.egov.ndc.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchResponseTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        UserSearchResponse target = new UserSearchResponse();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new UserSearchResponse()).isNotNull();
    }
}
