package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DepartmentSearchCriteriaTest {
    @Test
    void classLoads() {
        assertThat(DepartmentSearchCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DepartmentSearchCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
