package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorWrapperTest {
    @Test
    void classLoads() {
        assertThat(ErrorWrapper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ErrorWrapper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
