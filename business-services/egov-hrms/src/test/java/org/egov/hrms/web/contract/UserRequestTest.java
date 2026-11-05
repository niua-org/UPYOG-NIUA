package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserRequestTest {
    @Test
    void classLoads() {
        assertThat(UserRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = UserRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
