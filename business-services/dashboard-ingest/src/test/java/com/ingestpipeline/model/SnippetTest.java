package com.ingestpipeline.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SnippetTest {
    @Test
    void classLoads() {
        assertThat(Snippet.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Snippet.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
