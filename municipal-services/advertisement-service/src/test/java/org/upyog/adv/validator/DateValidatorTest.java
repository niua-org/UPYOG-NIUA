package org.upyog.adv.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DateValidatorTest {

    private final DateValidator validator = new DateValidator();

    @Mock
    private ConstraintValidatorContext context;

    @Test
    @DisplayName("isValid_whenNull_returnsTrue")
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("isValid_whenInvalidFormat_returnsFalse")
    void isValid_whenInvalidFormat_returnsFalse() {
        assertThat(validator.isValid("not-a-date", context)).isFalse();
    }
}
