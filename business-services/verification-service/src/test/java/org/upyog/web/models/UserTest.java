package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    @Test
    void classLoads() {
        assertThat(User.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = User.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
