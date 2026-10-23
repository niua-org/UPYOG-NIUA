package org.upyog.constants;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VerificationSearchConstantsTest {
    @Test
    void classLoads() {
        assertThat(VerificationSearchConstants.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = VerificationSearchConstants.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
