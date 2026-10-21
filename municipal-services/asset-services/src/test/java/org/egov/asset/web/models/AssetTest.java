// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AssetTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new Asset()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(Asset.builder().build()).isNotNull();
}

    }
