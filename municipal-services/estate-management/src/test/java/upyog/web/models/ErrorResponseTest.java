// generated-by: generate_per_class_unit_tests.py
package upyog.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class ErrorResponseTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new ErrorResponse()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(ErrorResponse.builder().build()).isNotNull();
}

    }
