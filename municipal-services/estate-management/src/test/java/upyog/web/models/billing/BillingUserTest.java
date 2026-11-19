// generated-by: generate_per_class_unit_tests.py
package upyog.web.models.billing;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class BillingUserTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new BillingUser()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(BillingUser.builder().build()).isNotNull();
}

    }
