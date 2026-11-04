// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.web.models.user;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class CreateUserRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new CreateUserRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(CreateUserRequest.builder().build()).isNotNull();
}

    }
