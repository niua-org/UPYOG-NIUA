// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models.disposal;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AssetDisposalTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AssetDisposal()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AssetDisposal.builder().build()).isNotNull();
}

    }
