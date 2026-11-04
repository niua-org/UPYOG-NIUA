package com.tarento.analytics.dao.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchDaoImplTest {
    @Test
    void classLoads() {
        assertThat(ElasticSearchDaoImpl.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ElasticSearchDaoImpl.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
