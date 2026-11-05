// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AssetRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AssetRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AssetRequest.builder().build()).isNotNull();
}

    }
