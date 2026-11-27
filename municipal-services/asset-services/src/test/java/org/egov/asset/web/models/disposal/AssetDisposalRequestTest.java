// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.web.models.disposal;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class AssetDisposalRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new AssetDisposalRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(AssetDisposalRequest.builder().build()).isNotNull();
}

    }
