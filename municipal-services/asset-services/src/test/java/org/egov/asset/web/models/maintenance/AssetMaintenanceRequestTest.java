// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models.maintenance;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AssetMaintenanceRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AssetMaintenanceRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AssetMaintenanceRequest.builder().build()).isNotNull();
}

    }
