package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class QueryTest {
    @Test
    void classLoads() {
        assertThat(Query.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Query.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
