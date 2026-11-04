// generated-by: generate_per_class_unit_tests.py
package org.egov.wscalculation.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class GeoLocationTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new GeoLocation()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(GeoLocation.builder().build()).isNotNull();
}

    }
