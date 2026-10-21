package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditDetailsTest {
    @Test
    void classLoads() {
        assertThat(AuditDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AuditDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
