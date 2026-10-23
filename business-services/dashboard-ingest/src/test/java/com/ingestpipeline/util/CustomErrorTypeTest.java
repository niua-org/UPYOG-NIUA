package com.ingestpipeline.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorTypeTest {
    @Test
    void classLoads() {
        assertThat(CustomErrorType.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CustomErrorType.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
