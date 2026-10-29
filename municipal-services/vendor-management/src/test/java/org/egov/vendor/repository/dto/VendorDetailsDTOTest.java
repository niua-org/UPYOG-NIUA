// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.repository.dto;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class VendorDetailsDTOTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new VendorDetailsDTO()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(VendorDetailsDTO.builder().build()).isNotNull();
}

    }
