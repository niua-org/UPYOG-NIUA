// generated-by: generate_per_class_unit_tests.py
package upyog.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class RentPaymentDetailsTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new RentPaymentDetails()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(RentPaymentDetails.builder().build()).isNotNull();
}

    }
