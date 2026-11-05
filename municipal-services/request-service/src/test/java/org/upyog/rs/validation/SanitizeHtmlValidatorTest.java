package org.upyog.rs.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SanitizeHtmlValidatorTest {

    private SanitizeHtmlValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new SanitizeHtmlValidator();
    }

    @Test
    @DisplayName("isValid_whenNull_returnsTrue")
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("isValid_whenPlainText_returnsTrue")
    void isValid_whenPlainText_returnsTrue() {
        assertThat(validator.isValid("hello", context)).isTrue();
    }

    @Test
    @DisplayName("isValid_whenScriptTag_returnsFalse")
    void isValid_whenScriptTag_returnsFalse() {
        assertThat(validator.isValid("<script>alert(1)</script>", context)).isFalse();
    }
}
