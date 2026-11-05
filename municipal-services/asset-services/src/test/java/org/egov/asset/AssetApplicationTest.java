// generated-by: generate_per_class_unit_tests.py
package org.egov.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(AssetApplication.class).isNotNull();
        assertThat(AssetApplication.class.getSimpleName()).isEqualTo("AssetApplication");
    }
}
