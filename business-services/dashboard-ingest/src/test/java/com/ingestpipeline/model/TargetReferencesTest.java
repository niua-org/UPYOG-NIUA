package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TargetReferencesTest {
    @Test
    void classLoads() {
        assertThat(TargetReferences.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TargetReferences.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
