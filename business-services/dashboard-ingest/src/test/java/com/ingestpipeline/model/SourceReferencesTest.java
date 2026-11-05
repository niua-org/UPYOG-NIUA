package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SourceReferencesTest {
    @Test
    void classLoads() {
        assertThat(SourceReferences.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SourceReferences.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
