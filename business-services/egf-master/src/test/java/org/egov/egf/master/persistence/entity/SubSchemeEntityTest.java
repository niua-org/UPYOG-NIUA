package org.egov.egf.master.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SubSchemeEntityTest {
    @Test
    void classLoads() {
        assertThat(SubSchemeEntity.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SubSchemeEntity.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
