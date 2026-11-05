// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.web.model.user;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class UserRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new UserRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(UserRequest.builder().build()).isNotNull();
}

    }
