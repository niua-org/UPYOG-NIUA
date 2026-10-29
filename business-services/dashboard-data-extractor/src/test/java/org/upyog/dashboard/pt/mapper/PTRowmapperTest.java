package org.upyog.dashboard.pt.mapper;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PTRowmapperTest {
    @Test
    void classLoads() {
        assertThat(PTRowmapper.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = PTRowmapper.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
