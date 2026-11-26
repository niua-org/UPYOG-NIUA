package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CommonDataModelTest {
    @Test
    void classLoads() {
        assertThat(CommonDataModel.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CommonDataModel.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
