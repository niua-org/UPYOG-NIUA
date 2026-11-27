package org.egov.rb.pgr.v2.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdditionalDetailValidatorTest {

    private AdditionalDetailValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new AdditionalDetailValidator();
        CharacterConstraint constraint = org.mockito.Mockito.mock(CharacterConstraint.class);
        org.mockito.Mockito.when(constraint.size()).thenReturn(5);
        validator.initialize(constraint);
    }

    @Test
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_whenTooLong_returnsFalse() {
        assertThat(validator.isValid("123456", context)).isFalse();
    }

    @Test
    void isValid_whenWithinSize_returnsTrue() {
        assertThat(validator.isValid("12345", context)).isTrue();
    }
}
