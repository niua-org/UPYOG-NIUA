package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GetUserByIdRequestTest {
    @Test
    void classLoads() {
        assertThat(GetUserByIdRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = GetUserByIdRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
