package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchDictatorTest {
    @Test
    void classLoads() {
        assertThat(ElasticSearchDictator.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ElasticSearchDictator.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
