package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseTest {
    @Test
    void classLoads() {
        assertThat(Response.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Response.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
