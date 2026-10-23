package org.egov.hrms.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RequestInfoWrapperTest {
    @Test
    void classLoads() {
        assertThat(RequestInfoWrapper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RequestInfoWrapper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
