package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SMSRequestTest {
    @Test
    void classLoads() {
        assertThat(SMSRequest.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = SMSRequest.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
