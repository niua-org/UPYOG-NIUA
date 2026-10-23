// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.web.models.users;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class UserSearchRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new UserSearchRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(UserSearchRequest.builder().build()).isNotNull();
}

    }
