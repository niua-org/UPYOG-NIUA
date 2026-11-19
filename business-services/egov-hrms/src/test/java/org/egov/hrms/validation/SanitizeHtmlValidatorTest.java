package org.egov.hrms.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizeHtmlValidatorTest {

    private SanitizeHtmlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SanitizeHtmlValidator();
    }

    @Test
    void isValid_nullValue_returnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_plainText_returnsTrue() {
        assertThat(validator.isValid("Hello HRMS", null)).isTrue();
    }

    @Test
    void isValid_scriptTag_returnsFalse() {
        assertThat(validator.isValid("<script>alert(1)</script>", null)).isFalse();
    }
}
