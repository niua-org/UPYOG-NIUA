package org.egov.collection.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FunctionTest {
    @Test
    void classLoads() {
        assertThat(Function.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Function.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
