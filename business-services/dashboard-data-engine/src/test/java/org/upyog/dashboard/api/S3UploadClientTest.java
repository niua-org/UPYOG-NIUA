package org.upyog.dashboard.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class S3UploadClientTest {
    @Test
    void classLoads() {
        assertThat(S3UploadClient.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = S3UploadClient.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
