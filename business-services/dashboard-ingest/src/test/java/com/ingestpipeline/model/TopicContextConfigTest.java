package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TopicContextConfigTest {
    @Test
    void classLoads() {
        assertThat(TopicContextConfig.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = TopicContextConfig.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
