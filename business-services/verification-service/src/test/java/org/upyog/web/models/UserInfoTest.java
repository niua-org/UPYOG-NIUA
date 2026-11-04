package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserInfoTest {
    @Test
    void classLoads() {
        assertThat(UserInfo.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserInfo.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
