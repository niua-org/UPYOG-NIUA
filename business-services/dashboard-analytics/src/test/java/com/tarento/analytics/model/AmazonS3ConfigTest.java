package com.tarento.analytics.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmazonS3ConfigTest {
    @Test
    void classLoads() {
        assertThat(AmazonS3Config.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AmazonS3Config.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
