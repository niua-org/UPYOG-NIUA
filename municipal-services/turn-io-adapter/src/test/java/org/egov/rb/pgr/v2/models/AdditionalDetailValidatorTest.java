package org.egov.rb.pgr.v2.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdditionalDetailValidatorTest {

    private final AdditionalDetailValidator validator = new AdditionalDetailValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @BeforeEach
    void setUp() {
        CharacterConstraint constraint = mock(CharacterConstraint.class);
        when(constraint.size()).thenReturn(10);
        validator.initialize(constraint);
    }

    @Test
    @DisplayName("isValid_whenNull_returnsTrue")
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("isValid_whenWithinSize_returnsTrue")
    void isValid_whenWithinSize_returnsTrue() {
        assertThat(validator.isValid("short", context)).isTrue();
    }

    @Test
    @DisplayName("isValid_whenExceedsSize_returnsFalse")
    void isValid_whenExceedsSize_returnsFalse() {
        assertThat(validator.isValid("this value is too long", context)).isFalse();
    }
}
