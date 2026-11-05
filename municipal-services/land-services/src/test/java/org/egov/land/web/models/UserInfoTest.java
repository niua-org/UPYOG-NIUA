// generated-by: generate_per_class_unit_tests.py
package org.egov.land.web.models;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class UserInfoTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new UserInfo()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(UserInfo.builder().build()).isNotNull();
}

    }
