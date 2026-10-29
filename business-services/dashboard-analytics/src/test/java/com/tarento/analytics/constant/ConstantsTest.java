package com.tarento.analytics.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantsTest {

    @Test
    void httpMethodConstantsAreDefined() {
        assertThat(Constants.GET).isEqualTo("GET");
        assertThat(Constants.POST).isEqualTo("POST");
        assertThat(Constants.RATING_LIST).containsExactly(1L, 2L, 3L, 4L, 5L);
    }
}
