package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {
    @Test
    void classLoads() {
        assertThat(Pagination.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Pagination.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
