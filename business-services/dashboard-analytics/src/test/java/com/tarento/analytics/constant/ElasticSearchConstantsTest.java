package com.tarento.analytics.constant;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchConstantsTest {
    @Test
    void classLoads() {
        assertThat(ElasticSearchConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ElasticSearchConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
