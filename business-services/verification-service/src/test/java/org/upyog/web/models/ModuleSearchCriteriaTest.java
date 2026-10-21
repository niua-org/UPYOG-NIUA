package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleSearchCriteriaTest {
    @Test
    void classLoads() {
        assertThat(ModuleSearchCriteria.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ModuleSearchCriteria.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
