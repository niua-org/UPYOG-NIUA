package org.upyog.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizeHtmlValidatorTest {

    private final SanitizeHtmlValidator validator = new SanitizeHtmlValidator();

    @Test
    void isValid_null_returnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_script_returnsFalse() {
        assertThat(validator.isValid("<script>x</script>", null)).isFalse();
    }
}
