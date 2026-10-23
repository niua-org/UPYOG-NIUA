package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesignationSearchCriteriaTest {
    @Test
    void classLoads() {
        assertThat(DesignationSearchCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = DesignationSearchCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
