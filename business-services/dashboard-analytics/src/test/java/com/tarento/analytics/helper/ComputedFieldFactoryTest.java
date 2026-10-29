package com.tarento.analytics.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ComputedFieldFactoryTest {

    @Mock
    private AdditiveComputedField additiveComputedField;
    @Mock
    private SubtractiveComputedField subtractiveComputedField;
    @Mock
    private PercentageComputedField percentageComputedField;
    @Mock
    private AverageComputedField averageComputedField;

    @InjectMocks
    private ComputedFieldFactory factory;

    @Test
    void getInstance_additive_returnsAdditiveField() {
        assertThat(factory.getInstance("AdditiveComputedField")).isSameAs(additiveComputedField);
    }

    @Test
    void getInstance_subtractive_returnsSubtractiveField() {
        assertThat(factory.getInstance("SubtractiveComputedField")).isSameAs(subtractiveComputedField);
    }

    @Test
    void getInstance_percentage_returnsPercentageField() {
        assertThat(factory.getInstance("PercentageComputedField")).isSameAs(percentageComputedField);
    }

    @Test
    void getInstance_average_returnsAverageField() {
        assertThat(factory.getInstance("AverageComputedField")).isSameAs(averageComputedField);
    }
}
