package org.upyog.sv.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateValidatorTest {

    private DateValidator dateValidator;

    @BeforeEach
    void setUp() {
        dateValidator = new DateValidator();
        dateValidator.initialize(null);
    }

    @Test
    void isValid_whenNullOrEmpty_returnsTrue() {
        assertThat(dateValidator.isValid(null, null)).isTrue();
        assertThat(dateValidator.isValid("", null)).isTrue();
    }

    @Test
    void isValid_whenFormattedDate_returnsTrue() {
        assertThat(dateValidator.isValid("2026-02-01", null)).isTrue();
    }

    @Test
    void isValid_whenMalformedDate_returnsFalse() {
        assertThat(dateValidator.isValid("01-02-2026", null)).isFalse();
    }
}
