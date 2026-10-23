package org.egov.demand.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IdGenRepoTest {
    @Test
    void classLoads() {
        assertThat(IdGenRepo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = IdGenRepo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
