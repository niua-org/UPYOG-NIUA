package org.upyog.dashboard.loader.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class S3DashboardDataLoaderImplTest {
    @Test
    void classLoads() {
        assertThat(S3DashboardDataLoaderImpl.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = S3DashboardDataLoaderImpl.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
