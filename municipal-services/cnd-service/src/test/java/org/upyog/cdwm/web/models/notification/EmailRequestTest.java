// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.web.models.notification;

    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    class EmailRequestTest {

@Test
void newInstance_whenCreated_isNotNull() {
    assertThat(new EmailRequest()).isNotNull();
}


@Test
void builder_whenDefaultBuilds_isNotNull() {
    assertThat(EmailRequest.builder().build()).isNotNull();
}

    }
