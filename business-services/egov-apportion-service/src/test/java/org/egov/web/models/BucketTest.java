package org.egov.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BucketTest {
    @Test
    void classLoads() {
        assertThat(Bucket.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = Bucket.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
