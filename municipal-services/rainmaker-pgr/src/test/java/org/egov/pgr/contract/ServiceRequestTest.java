// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.contract;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ServiceRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ServiceRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ServiceRequest.builder().build()).isNotNull();
}

    }
