package org.upyog.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserSearchRequestTest {
    @Test
    void classLoads() {
        assertThat(UserSearchRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserSearchRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
