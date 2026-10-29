package org.egov.demand.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintValidatorContext;

class SanitizeHtmlValidatorTest {

    private SanitizeHtmlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SanitizeHtmlValidator();
        validator.initialize(null);
    }

    @Test
    void isValid_acceptsNull() {
        assertThat(validator.isValid(null, mockContext())).isTrue();
    }

    @Test
    void isValid_acceptsPlainText() {
        assertThat(validator.isValid("Hello world", mockContext())).isTrue();
    }

    @Test
    void isValid_acceptsAllowedFormatting() {
        assertThat(validator.isValid("<b>bold</b>", mockContext())).isTrue();
    }

    @Test
    void isValid_rejectsScriptTag() {
        assertThat(validator.isValid("<script>alert(1)</script>", mockContext())).isFalse();
    }

    @Test
    void isValid_rejectsEventHandlerInjection() {
        assertThat(validator.isValid("<img src=x onerror=alert(1)>", mockContext())).isFalse();
    }

    private ConstraintValidatorContext mockContext() {
        return org.mockito.Mockito.mock(ConstraintValidatorContext.class);
    }
}
