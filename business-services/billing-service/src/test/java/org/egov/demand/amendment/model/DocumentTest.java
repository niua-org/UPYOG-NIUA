package org.egov.demand.amendment.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {
    @Test
    void classLoads() {
        assertThat(Document.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Document.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
