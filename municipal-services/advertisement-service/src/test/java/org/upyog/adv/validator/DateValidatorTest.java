package org.upyog.adv.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DateValidatorTest {

    private DateValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new DateValidator();
    }

    @Test
    void isValid_whenBlank_returnsTrue() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    void isValid_whenInvalidFormat_returnsFalse() {
        assertThat(validator.isValid("not-a-date", context)).isFalse();
    }

    @Test
    void isValid_whenValidFormat_returnsTrue() {
        assertThat(validator.isValid("2025-01-01", context)).isTrue();
    }
}
