package org.upyog.tp.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_whenPlainText_returnsTrue() {
        assertThat(validator.isValid("hello", context)).isTrue();
    }

    @Test
    void isValid_whenScriptTag_returnsFalse() {
        assertThat(validator.isValid("<script>x</script>", context)).isFalse();
    }
}
