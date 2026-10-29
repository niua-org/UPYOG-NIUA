package org.upyog.dashboard.mdms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MdmsCriteriaReqTest {
    @Test
    void classLoads() {
        assertThat(MdmsCriteriaReq.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = MdmsCriteriaReq.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
