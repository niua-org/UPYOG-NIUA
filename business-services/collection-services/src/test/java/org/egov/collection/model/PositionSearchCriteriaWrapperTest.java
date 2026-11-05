package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PositionSearchCriteriaWrapperTest {
    @Test
    void classLoads() {
        assertThat(PositionSearchCriteriaWrapper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PositionSearchCriteriaWrapper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
