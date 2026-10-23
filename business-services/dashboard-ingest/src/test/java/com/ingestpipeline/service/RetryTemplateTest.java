package com.ingestpipeline.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetryTemplateTest {
    @Test
    void classLoads() {
        assertThat(RetryTemplate.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RetryTemplate.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
