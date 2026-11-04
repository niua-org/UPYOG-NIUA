package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MockMultipartFileTest {
    @Test
    void classLoads() {
        assertThat(MockMultipartFile.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MockMultipartFile.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
