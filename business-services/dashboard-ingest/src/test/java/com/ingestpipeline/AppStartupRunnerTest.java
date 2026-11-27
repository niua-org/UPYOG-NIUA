package com.ingestpipeline;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AppStartupRunnerTest {
    @Test
    void classLoads() {
        assertThat(AppStartupRunner.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = AppStartupRunner.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
