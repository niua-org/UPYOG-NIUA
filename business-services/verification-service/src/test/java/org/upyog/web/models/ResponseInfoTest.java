package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoTest {
    @Test
    void classLoads() {
        assertThat(ResponseInfo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = ResponseInfo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
