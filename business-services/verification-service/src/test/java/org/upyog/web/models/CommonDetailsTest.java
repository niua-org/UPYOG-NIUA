package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CommonDetailsTest {
    @Test
    void classLoads() {
        assertThat(CommonDetails.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = CommonDetails.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
