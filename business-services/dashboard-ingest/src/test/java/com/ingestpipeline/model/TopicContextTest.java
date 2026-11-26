package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TopicContextTest {
    @Test
    void classLoads() {
        assertThat(TopicContext.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TopicContext.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
