package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserResponseTest {
    @Test
    void classLoads() {
        assertThat(UserResponse.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserResponse.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
