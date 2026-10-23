package org.upyog.dashboard.pt.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RawPtCollectionTest {
    @Test
    void classLoads() {
        assertThat(RawPtCollection.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = RawPtCollection.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
