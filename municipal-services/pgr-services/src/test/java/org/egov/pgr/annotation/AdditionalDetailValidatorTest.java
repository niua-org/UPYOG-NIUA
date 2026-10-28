package org.egov.pgr.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdditionalDetailValidatorTest {

    private AdditionalDetailValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new AdditionalDetailValidator();
        CharacterConstraint constraint = mock(CharacterConstraint.class);
        org.mockito.Mockito.when(constraint.size()).thenReturn(10);
        validator.initialize(constraint);
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_whenWithinSize_returnsTrue() {
        assertThat(validator.isValid("short", context)).isTrue();
    }

    @Test
    void isValid_whenExceedsSize_returnsFalse() {
        assertThat(validator.isValid("this-is-way-too-long", context)).isFalse();
    }
}
